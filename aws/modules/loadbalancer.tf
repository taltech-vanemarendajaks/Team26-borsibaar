resource "aws_security_group" "alb_sg" {
  name        = "${local.resource_prefix}-alb_sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = local.vpc_id

  ingress {
    description = "Allow HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

}

resource "aws_lb_target_group" "wordpress_tg" {
  name        = replace("${local.resource_prefix}--wordpress_tg",  "_", "-")
  port        = 80
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = local.vpc_id

  health_check {
    path                = "/wp-admin/images/wordpress-logo.svg"
    protocol            = "HTTP"
    interval            = 60
    timeout             = 5
    healthy_threshold   = 3
    unhealthy_threshold = 3
    matcher             = "200-399"
  }
}

resource "aws_lb" "wordpress_alb" {
  name               = replace("${local.resource_prefix}-wordpress_alb",  "_", "-")
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = local.public_subnet_ids

  enable_deletion_protection = false
}

resource "aws_lb_listener" "http_listener" {
  load_balancer_arn = aws_lb.wordpress_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.wordpress_tg.arn
  }
}