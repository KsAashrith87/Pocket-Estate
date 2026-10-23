output "public_ip" {
  description = "Elastic IP address. Point the application's DNS A record here."
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "EC2 instance ID for AWS Systems Manager sessions."
  value       = aws_instance.app.id
}
