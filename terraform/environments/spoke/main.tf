terraform {
  required_version = ">= 1.5.0"

  backend "s3" {
    bucket         = "billing-terraform-state"
    key            = "spoke/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "billing-terraform-locks"
    encrypt        = true

    # Workspace-based isolation: each spoke gets its own state file
    # e.g. spoke/env:/acme-prod/terraform.tfstate
    workspace_key_prefix = "spoke"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "billing-spoke"
      ManagedBy   = "terraform"
      Environment = var.environment
    }
  }
}

# ------------------------------------------------------------------------------
# Variables for spoke parameterization
# ------------------------------------------------------------------------------

variable "account_id" {
  description = "AWS account ID for this spoke"
  type        = string
}

variable "account_name" {
  description = "Human-readable name for this spoke account"
  type        = string
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)"
  type        = string
  default     = "prod"
}

variable "hub_bucket_arn" {
  description = "ARN of the central hub S3 bucket"
  type        = string
  default     = "arn:aws:s3:::billing-hub-cur-central"
}

variable "hub_account_id" {
  description = "AWS account ID of the billing hub account"
  type        = string
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

# ------------------------------------------------------------------------------
# Spoke module invocation
# ------------------------------------------------------------------------------

module "cur_spoke" {
  source = "../../modules/cur-spoke"

  account_id     = var.account_id
  account_name   = var.account_name
  environment    = var.environment
  hub_bucket_arn = var.hub_bucket_arn
  hub_account_id = var.hub_account_id
  region         = var.region

  tags = {
    CostCenter = "platform-engineering"
    Workspace  = terraform.workspace
  }
}

# ------------------------------------------------------------------------------
# Outputs
# ------------------------------------------------------------------------------

output "bucket_name" {
  value = module.cur_spoke.bucket_name
}

output "bucket_arn" {
  value = module.cur_spoke.bucket_arn
}

output "cur_report_name" {
  value = module.cur_spoke.cur_report_name
}
