variable "account_id" {
  description = "AWS account ID for this spoke"
  type        = string

  validation {
    condition     = can(regex("^\\d{12}$", var.account_id))
    error_message = "account_id must be a 12-digit AWS account ID."
  }
}

variable "account_name" {
  description = "Human-readable name for the AWS account (used in resource naming)"
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.account_name))
    error_message = "account_name must contain only lowercase alphanumeric characters and hyphens."
  }
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod)"
  type        = string
}

variable "hub_bucket_arn" {
  description = "ARN of the central hub S3 bucket for CUR replication"
  type        = string
}

variable "hub_account_id" {
  description = "AWS account ID of the central billing hub account"
  type        = string

  validation {
    condition     = can(regex("^\\d{12}$", var.hub_account_id))
    error_message = "hub_account_id must be a 12-digit AWS account ID."
  }
}

variable "region" {
  description = "AWS region for CUR and S3 resources"
  type        = string
  default     = "ap-southeast-1"
}

variable "tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}
