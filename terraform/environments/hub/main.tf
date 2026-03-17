terraform {
  required_version = ">= 1.5.0"

  backend "s3" {
    bucket         = "billing-terraform-state"
    key            = "hub/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "billing-terraform-locks"
    encrypt        = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-southeast-1"

  default_tags {
    tags = {
      Project     = "billing-hub"
      ManagedBy   = "terraform"
      Environment = "prod"
    }
  }
}

module "cur_hub" {
  source = "../../modules/cur-hub"

  spoke_account_ids = [
    "111111111001",
    "111111111002",
    "111111111003",
    "111111111004",
    "111111111005",
    "111111111006",
    "111111111007",
    "111111111008",
    "111111111009",
    "111111111010",
    "111111111011",
    "111111111012",
    "111111111013",
    "111111111014",
    "111111111015",
    "111111111016",
    "111111111017",
    "111111111018",
    "111111111019",
    "111111111020",
    "111111111021",
    "111111111022",
    "111111111023",
    "111111111024",
    "111111111025",
    "111111111026",
    "111111111027",
    "111111111028",
    "111111111029",
    "111111111030",
  ]

  region = "ap-southeast-1"

  tags = {
    CostCenter = "platform-engineering"
  }
}

# ------------------------------------------------------------------------------
# Outputs
# ------------------------------------------------------------------------------

output "central_bucket_name" {
  value = module.cur_hub.central_bucket_name
}

output "central_bucket_arn" {
  value = module.cur_hub.central_bucket_arn
}

output "glue_database_name" {
  value = module.cur_hub.glue_database_name
}

output "athena_workgroup_name" {
  value = module.cur_hub.athena_workgroup_name
}
