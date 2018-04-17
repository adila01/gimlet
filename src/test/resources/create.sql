drop table customer if exists;
drop table invoice if exists;

create table customer(
    id int,
    name varchar(32)
);

insert into customer values (0, 'Kevin');

create table invoice(
    id int,
    customer_id int,
);