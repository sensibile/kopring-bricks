create table if not exists outbox_event (
    id varchar(64) primary key,
    aggregate_type varchar(128) not null,
    aggregate_id varchar(255) not null,
    event_type varchar(255) not null,
    event_version integer not null,
    payload jsonb not null,
    headers jsonb,
    status varchar(32) not null,
    created_at timestamptz not null,
    available_at timestamptz not null,
    next_attempt_at timestamptz not null,
    claimed_at timestamptz,
    published_at timestamptz,
    retry_count integer not null default 0,
    last_error text
);

create index if not exists outbox_event_publish_idx
    on outbox_event (status, next_attempt_at, created_at);

create index if not exists outbox_event_aggregate_idx
    on outbox_event (aggregate_type, aggregate_id, created_at);
