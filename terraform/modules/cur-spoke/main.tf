terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# ------------------------------------------------------------------------------
# Local values
# ------------------------------------------------------------------------------

locals {
  bucket_name     = "billing-cur-${var.account_name}-${var.account_id}"
  cur_report_name = "cur-${var.account_name}-hourly"

  default_tags = merge(var.tags, {
    Module      = "cur-spoke"
    AccountId   = var.account_id
    AccountName = var.account_name
    Environment = var.environment
  })
}

# ------------------------------------------------------------------------------
# S3 bucket for local CUR storage
# ------------------------------------------------------------------------------

resource "aws_s3_bucket" "cur" {
  bucket        = local.bucket_name
  force_destroy = false

  tags = local.default_tags
}

resource "aws_s3_bucket_versioning" "cur" {
  bucket = aws_s3_bucket.cur.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cur" {
  bucket = aws_s3_bucket.cur.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "cur" {
  bucket = aws_s3_bucket.cur.id

  rule {
    id     = "cur-lifecycle"
    status = "Enabled"

    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 365
    }
  }
}

# ------------------------------------------------------------------------------
# Bucket policy - allow CUR service to write reports
# ------------------------------------------------------------------------------

data "aws_iam_policy_document" "cur_bucket_policy" {
  statement {
    sid    = "AllowCURServiceGetBucketAcl"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["billingreports.amazonaws.com"]
    }

    actions   = ["s3:GetBucketAcl", "s3:GetBucketPolicy"]
    resources = [aws_s3_bucket.cur.arn]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [var.account_id]
    }
  }

  statement {
    sid    = "AllowCURServicePutObject"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["billingreports.amazonaws.com"]
    }

    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.cur.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [var.account_id]
    }
  }
}

resource "aws_s3_bucket_policy" "cur" {
  bucket = aws_s3_bucket.cur.id
  policy = data.aws_iam_policy_document.cur_bucket_policy.json
}

# ------------------------------------------------------------------------------
# IAM role for S3 replication to hub bucket
# ------------------------------------------------------------------------------

data "aws_iam_policy_document" "replication_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["s3.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "replication" {
  name               = "billing-cur-replication-${var.account_name}"
  assume_role_policy = data.aws_iam_policy_document.replication_assume_role.json

  tags = local.default_tags
}

data "aws_iam_policy_document" "replication" {
  statement {
    sid    = "SourceBucketPermissions"
    effect = "Allow"

    actions = [
      "s3:GetReplicationConfiguration",
      "s3:ListBucket",
    ]

    resources = [aws_s3_bucket.cur.arn]
  }

  statement {
    sid    = "SourceObjectPermissions"
    effect = "Allow"

    actions = [
      "s3:GetObjectVersionForReplication",
      "s3:GetObjectVersionAcl",
      "s3:GetObjectVersionTagging",
    ]

    resources = ["${aws_s3_bucket.cur.arn}/*"]
  }

  statement {
    sid    = "DestinationBucketPermissions"
    effect = "Allow"

    actions = [
      "s3:ReplicateObject",
      "s3:ReplicateDelete",
      "s3:ReplicateTags",
    ]

    resources = ["${var.hub_bucket_arn}/*"]
  }
}

resource "aws_iam_role_policy" "replication" {
  name   = "billing-cur-replication-policy"
  role   = aws_iam_role.replication.id
  policy = data.aws_iam_policy_document.replication.json
}

# ------------------------------------------------------------------------------
# S3 replication configuration to hub bucket
# ------------------------------------------------------------------------------

resource "aws_s3_bucket_replication_configuration" "cur_to_hub" {
  depends_on = [aws_s3_bucket_versioning.cur]

  bucket = aws_s3_bucket.cur.id
  role   = aws_iam_role.replication.arn

  rule {
    id     = "replicate-cur-to-hub"
    status = "Enabled"

    destination {
      bucket        = var.hub_bucket_arn
      storage_class = "STANDARD"

      account = var.hub_account_id

      access_control_translation {
        owner = "Destination"
      }
    }
  }
}

# ------------------------------------------------------------------------------
# CUR report definition
# ------------------------------------------------------------------------------

resource "aws_cur_report_definition" "this" {
  report_name                = local.cur_report_name
  time_unit                  = "HOURLY"
  format                     = "Parquet"
  compression                = "GZIP"
  additional_schema_elements = ["RESOURCES"]

  s3_bucket = aws_s3_bucket.cur.id
  s3_region = var.region
  s3_prefix = "cur-reports"

  report_versioning = "OVERWRITE_REPORT"

  depends_on = [aws_s3_bucket_policy.cur]
}
