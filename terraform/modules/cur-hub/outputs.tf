output "central_bucket_name" {
  description = "Name of the central hub CUR S3 bucket"
  value       = aws_s3_bucket.central.id
}

output "central_bucket_arn" {
  description = "ARN of the central hub CUR S3 bucket"
  value       = aws_s3_bucket.central.arn
}

output "glue_database_name" {
  description = "Name of the Glue catalog database for CUR data"
  value       = aws_glue_catalog_database.billing.name
}

output "athena_workgroup_name" {
  description = "Name of the Athena workgroup for billing queries"
  value       = aws_athena_workgroup.billing.name
}
