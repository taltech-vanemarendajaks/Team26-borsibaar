resource "aws_ecr_repository" "wordpress" {
  name                 = "${local.resource_prefix}/wordpress"
  image_tag_mutability = "MUTABLE"      # allows updating the tags
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecs_cluster" "wordpress_cluster" {
  name = "${local.resource_prefix}-wordpress_cluster"
}

resource "aws_ecs_task_definition" "wordpress_task" {
  family                   = "${local.resource_prefix}-wordpress_task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = local.ecs_task_execution_role_arn
  task_role_arn            = local.ecs_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "wordpress"
      image     = "${aws_ecr_repository.wordpress.repository_url}:latest"
      essential = true

      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "WORDPRESS_DB_HOST"
          value = aws_ssm_parameter.wordpress_db_host.arn
        },
        {
          name  = "WORDPRESS_DB_NAME"
          value = aws_ssm_parameter.wordpress_db_name.arn
        }
      ]

      secrets = [
        {
          name      = "WORDPRESS_DB_USER"
          valueFrom = "${aws_db_instance.database_instance.master_user_secret[0].secret_arn}:username::"
        },
        {
          name      = "WORDPRESS_DB_PASSWORD"
          valueFrom = "${aws_db_instance.database_instance.master_user_secret[0].secret_arn}:password::"
        }
      ]
    }
  ])
}

resource "aws_security_group" "ecs_service_sg" {
  name        = "${local.resource_prefix}-ecs_service_sg"
  description = "Security group for ECS service to allow traffic from ALB"
  vpc_id      = local.vpc_id

  # Ingress from ALB only
  ingress {
    description      = "Allow HTTP from ALB"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    security_groups  = [aws_security_group.alb_sg.id]   # Only allow traffic from ALB SG
  }

  # Outbound to anywhere (default)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_ecs_service" "wordpress_service" {
  name            = "${local.resource_prefix}-wordpress_service"
  cluster         = aws_ecs_cluster.wordpress_cluster.id
  task_definition = aws_ecs_task_definition.wordpress_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = local.private_subnet_ids
    security_groups = [aws_security_group.ecs_service_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.wordpress_tg.arn
    container_name   = "wordpress"
    container_port   = 80
  }

  depends_on = [
    aws_lb_listener.http_listener
  ]
}