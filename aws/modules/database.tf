resource "aws_db_subnet_group" "database_subnet_group" {
  name       = "database_subnet_group"
  subnet_ids = local.database_subnet_ids
}

resource "aws_security_group" "database_sg" {
  name        = "${local.resource_prefix}-database_sg"
  description = "Security group for database"
  vpc_id      = local.vpc_id

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    security_groups = [aws_security_group.ecs_service_sg.id]
    description = "Allow MySQL from internal network"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_instance" "database_instance" {
  identifier     = replace("${local.resource_prefix}-database_instance",  "_", "-")
  engine         = "mysql"
  engine_version = "8.4.7"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  storage_type      = "gp3"

  db_name                     =  "wordpress"
  username                    = "admin"
  manage_master_user_password = true

  db_subnet_group_name    = aws_db_subnet_group.database_subnet_group.name
  vpc_security_group_ids  = [aws_security_group.database_sg.id]

  publicly_accessible    = false
  skip_final_snapshot = true

  #availability_zones      = ["eu-north-1a"]
}


resource "aws_ssm_parameter" "wordpress_db_host" {
  name        = "/dev/WORDPRESS_DB_HOST_${local.resource_prefix}"
  description = "WordPress DB endpoint with port"
  type        = "String"  # plain text
  value       = aws_db_instance.database_instance.address
  tier        = "Standard"
}


resource "aws_ssm_parameter" "wordpress_db_name" {
  name        = "/dev/WORDPRESS_DB_NAME_${local.resource_prefix}"
  description = "WordPress database name"
  type        = "SecureString"  # encrypted with KMS
  value       = "wordpress"
  key_id      = "alias/aws/ssm"  # default AWS KMS key
  tier        = "Standard"
}