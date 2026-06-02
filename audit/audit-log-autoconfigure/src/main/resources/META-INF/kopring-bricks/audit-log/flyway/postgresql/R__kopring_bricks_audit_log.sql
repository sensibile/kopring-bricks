create table if not exists audit_log (
    id varchar(64) primary key,
    occurred_at timestamptz not null,
    actor_type varchar(64) not null,
    actor_id varchar(255) not null,
    actor_name varchar(255),
    action varchar(128) not null,
    target_type varchar(128) not null,
    target_id varchar(255) not null,
    target_name varchar(255),
    outcome varchar(32) not null,
    trace_id varchar(128),
    request_id varchar(128),
    reason text,
    metadata jsonb,
    before_state jsonb,
    after_state jsonb
);

create index if not exists audit_log_occurred_at_idx on audit_log (occurred_at desc);
create index if not exists audit_log_actor_idx on audit_log (actor_type, actor_id, occurred_at desc);
create index if not exists audit_log_target_idx on audit_log (target_type, target_id, occurred_at desc);
create index if not exists audit_log_action_idx on audit_log (action, occurred_at desc);
