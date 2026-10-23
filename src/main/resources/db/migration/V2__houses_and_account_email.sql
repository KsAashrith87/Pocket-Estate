alter table app_user add column email varchar(255);
create unique index uq_app_user_email on app_user(email);

create table house (
    id bigint not null auto_increment primary key,
    address varchar(255) not null,
    monthly_rent numeric(19, 2),
    status varchar(64),
    tenant_id bigint references client(id),
    owner_id bigint not null references app_user(id)
);

create table rent_payment (
    id bigint not null auto_increment primary key,
    house_id bigint not null references house(id),
    amount numeric(19, 2),
    billing_month varchar(7),
    paid_on date,
    method varchar(64),
    owner_id bigint not null references app_user(id)
);

create index idx_house_owner on house(owner_id);
create index idx_rent_payment_owner_paid_on on rent_payment(owner_id, paid_on desc);
