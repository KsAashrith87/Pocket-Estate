# Deploy Pocket Estate to EC2

This deployment runs the Angular/Spring Boot application, MySQL 8.4, and Caddy on one EC2 instance. Caddy obtains and renews HTTPS certificates. The instance accepts public traffic only on ports 80 and 443; administer it using AWS Systems Manager Session Manager instead of opening SSH.

> This single-instance design is suitable for a small deployment, but it is not highly available. MySQL data is stored on the EC2-attached Docker volume. Establish and test off-instance backups before storing real tenant or payment information.

## Prerequisites

- An AWS account with permissions to create EC2, Elastic IP, security group, IAM role/profile, and Systems Manager resources.
- A default VPC with a public subnet in the selected AWS region.
- Terraform 1.5+ and AWS CLI configured for the account.
- A DNS name whose A record can point to the Elastic IP.
- Access to the application source repository from the EC2 instance.

## Provision the instance

From the repository root:

```bash
cd deploy/ec2/terraform
terraform init
terraform plan
terraform apply
terraform output
```

The instance is created with an encrypted 24 GiB root volume, a static Elastic IP, a least-purpose Systems Manager instance role, and no inbound SSH rule. Creating AWS resources can incur charges. Review the plan and AWS pricing before applying.

Create or update the DNS A record for your host name to the `public_ip` output, then wait for DNS to resolve before starting Caddy.

## Deploy the application

Start an administrative shell using the `instance_id` output:

```bash
aws ssm start-session --target <instance-id> --region <aws-region>
```

On the instance, obtain the repository using your approved access method, then:

```bash
cd <repository-directory>
cp .env.example .env
chmod 600 .env
nano .env
```

Set `DATABASE_PASSWORD` and `DATABASE_ROOT_PASSWORD` to separate, long random values. Set `DOMAIN` to the DNS name pointing at the Elastic IP and `ACME_EMAIL` to an email address for certificate notices. Set `SESSION_COOKIE_SECURE=true`. Keep `.env` private; never commit it or put passwords in shell command arguments.

Start the service from the repository root:

```bash
sudo docker compose -f docker-compose.yml -f deploy/ec2/docker-compose.yml up -d --build
```

Visit `https://<your-domain>`. The first startup applies the Flyway schema migrations to the MySQL container. New users can create their own accounts through the app. Database port 3306 is not exposed to the internet.

## Operations

```bash
sudo docker compose -f docker-compose.yml -f deploy/ec2/docker-compose.yml ps
sudo docker compose -f docker-compose.yml -f deploy/ec2/docker-compose.yml logs -f app
sudo docker compose -f docker-compose.yml -f deploy/ec2/docker-compose.yml logs -f caddy
```

To deploy an updated version, update the repository checkout and rerun the `docker compose ... up -d --build` command. Back up the MySQL volume off-instance and test restoring it. Do not run `docker compose down -v` on a live installation; removing the volume deletes all accounts and business data.

For production use with important customer data, move MySQL to Amazon RDS, configure automated backups and point-in-time recovery, and use a managed secret store. Restrict AWS administrative permissions and establish monitoring, patching, and recovery procedures.

## Tear down

After backing up any data, run `terraform destroy` from `deploy/ec2/terraform`. This releases the Elastic IP and deletes the EC2 instance and its attached root volume. Remove the DNS A record separately.
