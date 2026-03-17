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
  central_bucket_name = "billing-hub-cur-central"
  athena_results_bucket_name = "billing-hub-athena-results"
  glue_database_name  = "billing_cur_db"

  spoke_replication_role_arns = [
    for id in var.spoke_account_ids : "arn:aws:iam::${id}:role/billing-cur-replication-*"
  ]

  default_tags = merge(var.tags, {
    Module = "cur-hub"
  })
}

# ------------------------------------------------------------------------------
# Central S3 bucket for aggregated CUR data
# ------------------------------------------------------------------------------

resource "aws_s3_bucket" "central" {
  bucket        = local.central_bucket_name
  force_destroy = false

  tags = local.default_tags
}

resource "aws_s3_bucket_versioning" "central" {
  bucket = aws_s3_bucket.central.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "central" {
  bucket = aws_s3_bucket.central.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "central" {
  bucket = aws_s3_bucket.central.id

  rule {
    id     = "cur-central-lifecycle"
    status = "Enabled"

    transition {
      days          = 180
      storage_class = "STANDARD_IA"
    }

    expiration {
      days = 730
    }
  }
}

# ------------------------------------------------------------------------------
# Bucket policy - allow cross-account replication from spokes
# ------------------------------------------------------------------------------

data "aws_iam_policy_document" "central_bucket_policy" {
  statement {
    sid    = "AllowSpokeReplicationPutObject"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [for id in var.spoke_account_ids : "arn:aws:iam::${id}:root"]
    }

    actions = [
      "s3:ReplicateObject",
      "s3:ReplicateDelete",
      "s3:ReplicateTags",
      "s3:PutObject",
      "s3:ObjectOwnerOverrideToBucketOwner",
    ]

    resources = ["${aws_s3_bucket.central.arn}/*"]
  }

  statement {
    sid    = "AllowSpokeReplicationBucketVersioning"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [for id in var.spoke_account_ids : "arn:aws:iam::${id}:root"]
    }

    actions = [
      "s3:List*",
      "s3:GetBucketVersioning",
      "s3:PutBucketVersioning",
    ]

    resources = [aws_s3_bucket.central.arn]
  }
}

resource "aws_s3_bucket_policy" "central" {
  bucket = aws_s3_bucket.central.id
  policy = data.aws_iam_policy_document.central_bucket_policy.json
}

# ------------------------------------------------------------------------------
# Glue database and crawler for CUR data
# ------------------------------------------------------------------------------

resource "aws_glue_catalog_database" "billing" {
  name = local.glue_database_name
}

# IAM role for Glue crawler

data "aws_iam_policy_document" "glue_crawler_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["glue.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "glue_crawler" {
  name               = "billing-hub-glue-crawler"
  assume_role_policy = data.aws_iam_policy_document.glue_crawler_assume_role.json

  tags = local.default_tags
}

data "aws_iam_policy_document" "glue_crawler" {
  statement {
    sid    = "S3ReadAccess"
    effect = "Allow"

    actions = [
      "s3:GetObject",
      "s3:ListBucket",
    ]

    resources = [
      aws_s3_bucket.central.arn,
      "${aws_s3_bucket.central.arn}/*",
    ]
  }

  statement {
    sid    = "GlueAccess"
    effect = "Allow"

    actions = [
      "glue:*",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "CloudWatchLogsAccess"
    effect = "Allow"

    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = ["arn:aws:logs:*:*:*"]
  }
}

resource "aws_iam_role_policy" "glue_crawler" {
  name   = "billing-hub-glue-crawler-policy"
  role   = aws_iam_role.glue_crawler.id
  policy = data.aws_iam_policy_document.glue_crawler.json
}

resource "aws_glue_crawler" "billing" {
  name          = "billing-cur-crawler"
  database_name = aws_glue_catalog_database.billing.name
  role          = aws_iam_role.glue_crawler.arn
  schedule      = "cron(0 4 * * ? *)" # Daily at 4:00 UTC

  s3_target {
    path = "s3://${aws_s3_bucket.central.id}/"
  }

  schema_change_policy {
    delete_behavior = "LOG"
    update_behavior = "UPDATE_IN_DATABASE"
  }

  configuration = jsonencode({
    Version = 1.0
    Grouping = {
      TableGroupingPolicy = "CombineCompatibleSchemas"
    }
    CrawlerOutput = {
      Partitions = {
        AddOrUpdateBehavior = "InheritFromTable"
      }
    }
  })

  tags = local.default_tags
}

# ------------------------------------------------------------------------------
# Athena workgroup with result bucket
# ------------------------------------------------------------------------------

resource "aws_s3_bucket" "athena_results" {
  bucket        = local.athena_results_bucket_name
  force_destroy = false

  tags = local.default_tags
}

resource "aws_s3_bucket_server_side_encryption_configuration" "athena_results" {
  bucket = aws_s3_bucket.athena_results.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_athena_workgroup" "billing" {
  name          = "billing-workgroup"
  force_destroy = false

  configuration {
    enforce_workgroup_configuration = true

    result_configuration {
      output_location = "s3://${aws_s3_bucket.athena_results.id}/query-results/"

      encryption_configuration {
        encryption_option = "SSE_S3"
      }
    }

    bytes_scanned_cutoff_per_query = 104857600 # 100 MB
  }

  tags = local.default_tags
}
