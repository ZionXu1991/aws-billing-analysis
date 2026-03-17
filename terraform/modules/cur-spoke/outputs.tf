output "bucket_name" {
  description = "Name of the spoke CUR S3 bucket"
  value       = aws_s3_bucket.cur.id
}

output "bucket_arn" {
  description = "ARN of the spoke CUR S3 bucket"
  value       = aws_s3_bucket.cur.arn
}

output "cur_report_name" {
  description = "Name of the CUR report definition"
  value       = aws_cur_report_definition.this.report_name
}
