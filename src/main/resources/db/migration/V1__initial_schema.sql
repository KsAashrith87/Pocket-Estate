create table app_user (
    id bigint not null auto_increment primary key,
    username varchar(255) not null unique,
    password varchar(255) not null,
    role varchar(64) not null
);

create table client (
    id bigint not null auto_increment primary key,
    name varchar(255) not null,
    email varchar(255) not null,
    phone varchar(255) not null,
    notes varchar(2000),
    owner_id bigint not null references app_user(id)
);

create table storage_unit (
    id bigint not null auto_increment primary key,
    unit_number varchar(255) not null,
    size varchar(255) not null,
    monthly_rate numeric(19, 2),
    status varchar(64),
    client_id bigint references client(id),
    owner_id bigint not null references app_user(id)
);

create table payment (
    id bigint not null auto_increment primary key,
    client_id bigint not null references client(id),
    amount numeric(19, 2),
    billing_month varchar(7),
    paid_on date,
    method varchar(64),
    owner_id bigint not null references app_user(id)
);

create table audit_log (
    id bigint not null auto_increment primary key,
    username varchar(255) not null,
    action varchar(64) not null,
    target_type varchar(64) not null,
    target_id varchar(255) not null,
    created_at timestamp(6) not null
);

create index idx_client_owner on client(owner_id);
create index idx_unit_owner on storage_unit(owner_id);
create index idx_payment_owner_paid_on on payment(owner_id, paid_on desc);
create index idx_audit_username_created_at on audit_log(username, created_at desc);
