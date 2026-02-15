locals {
  region = "eu-north-1"
  team = "tiim_26"
  member = "hlp"
  resource_prefix = "${local.team}-${local.member}"

  vpc_id   = "vpc-02fb02f6b35b49a55"
  vpc_cidr = "10.0.0.0/16"
  public_subnet_ids = [
    "subnet-075cc0f55c52f431e",
    "subnet-0deda9b0ab4690507",
    "subnet-07d8eb94afbed060d"
  ]
  private_subnet_ids = [
    "subnet-0bdfc835013848e8d",
    "subnet-0d9ece4706152d816",
    "subnet-06cf55ea79ac8da5e"
  ]
  database_subnet_ids = [
    "subnet-0e0637e027a086783",
    "subnet-0c622d475dc905bf0",
    "subnet-07c332b0e2897959f"
  ]
  ecs_task_execution_role_arn = "arn:aws:iam::187833180667:role/CustomEcsTaskExecutionRole"
  ecs_task_role_arn           = "arn:aws:iam::187833180667:role/CustomEcsTaskRole"
}

terraform {
  backend "local" {
    path = "terraform.tfstate"
  }

  # backend "s3" {
  #   bucket = "nava-terraform-state-for-students"
  #   key    = "tiim_26/state.tfstate"
  #   region = "eu-north-1"
  # }
}

provider "aws" {
  region = local.region

  default_tags {
    tags = {
      Team = local.team
    }
  }
}