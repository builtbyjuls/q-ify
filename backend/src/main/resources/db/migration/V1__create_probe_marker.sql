create table qify_probe (
    id integer primary key,
    description varchar(64) not null,
    applied_at timestamptz not null default current_timestamp
);

insert into qify_probe (id, description)
values (1, 'backend risk probe');
