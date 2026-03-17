variable "spoke_account_ids" {
  description = "List of AWS account IDs for spoke accounts that replicate CUR data to the hub"
  type        = list(string)

  validation {
    condition     = alltrue([for id in var.spoke_account_ids : can(regex("^\\d{12}$", id))])
    error_message = "All spoke_account_ids must be 12-digit AWS account IDs."
  }
}

variable "region" {
  description = "AWS region for hub resources"
  type        = string
  default     = "ap-southeast-1"
}

variable "tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}
