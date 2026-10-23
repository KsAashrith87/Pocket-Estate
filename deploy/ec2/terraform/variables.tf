variable "aws_region" {
  description = "AWS region to use. The account must have a default VPC in this region."
  type        = string
  default     = "us-west-2"
}

variable "instance_type" {
  description = "EC2 instance size for the application and containerized MySQL database."
  type        = string
  default     = "t3.small"
}

variable "name" {
  description = "Prefix for AWS resources created by this deployment."
  type        = string
  default     = "pocket-estate"
}
