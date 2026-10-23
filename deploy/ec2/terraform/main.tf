terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_security_group" "app" {
  name        = "${var.name}-web"
  description = "Public HTTPS and HTTP for Pocket Estate; administration uses Systems Manager."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP for ACME validation and HTTPS redirect"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS application traffic"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP/3 application traffic"
    from_port   = 443
    to_port     = 443
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-web"
  }
}

resource "aws_iam_role" "ssm" {
  name = "${var.name}-ssm"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ssm" {
  name = "${var.name}-ssm"
  role = aws_iam_role.ssm.name
}

resource "aws_instance" "app" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.ssm.name
  associate_public_ip_address = true

  user_data = <<-USERDATA
    #!/bin/bash
    set -euo pipefail
    dnf install -y docker git
    systemctl enable --now docker
    usermod -aG docker ec2-user
    compose_version="v2.39.4"
    compose_dir="/usr/local/lib/docker/cli-plugins"
    compose_url="https://github.com/docker/compose/releases/download/$${compose_version}"
    install -d -m 0755 "$${compose_dir}"
    curl -fsSL "$${compose_url}/checksums.txt" -o /tmp/compose-checksums.txt
    grep '  docker-compose-linux-x86_64$' /tmp/compose-checksums.txt > /tmp/compose-checksum
    test -s /tmp/compose-checksum
    curl -fsSL "$${compose_url}/docker-compose-linux-x86_64" -o "$${compose_dir}/docker-compose"
    (cd "$${compose_dir}" && sha256sum -c /tmp/compose-checksum)
    chmod 0755 "$${compose_dir}/docker-compose"
    docker compose version
  USERDATA

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 24
    encrypted             = true
    delete_on_termination = true
  }

  depends_on = [aws_iam_role_policy_attachment.ssm]

  tags = {
    Name = var.name
  }
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = "${var.name}-ip"
  }
}
