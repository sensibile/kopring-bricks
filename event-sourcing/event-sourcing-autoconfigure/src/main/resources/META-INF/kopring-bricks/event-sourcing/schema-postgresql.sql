create table if not exists event_store (
    id varchar(36) primary key,
    stream_id varchar(255) not null,
    stream_version bigint not null,
    event_type varchar(255) not null,
    event_version integer not null default 1,
    payload jsonb not null,
    metadata jsonb,
    occurred_at timestamptz not null,
    recorded_at timestamptz not null default now(),
    unique (stream_id, stream_version)
);

create index if not exists idx_event_store_stream
    on event_store (stream_id, stream_version);

create index if not exists idx_event_store_type
    on event_store (event_type, recorded_at);
