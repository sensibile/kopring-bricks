# ADR 0001: Rule Decision Starter Boundary

## Status

Proposed

## Context

Applications that use `kopring-bricks` may need feature flags, rollout rules, policy checks, or other rule-based branches. These systems repeatedly need the same integration concerns:

- Evaluate a named decision from typed context.
- Return an explainable result, not only a boolean.
- Keep rule changes auditable.
- Publish rule change events through an outbox when other systems must react.
- Protect rule admin APIs from stale updates.
- Make tests deterministic without a real rule backend.

The actual business rules, targeting model, storage schema, and rollout strategy are application-specific. A starter should reduce repeated Spring Boot plumbing without becoming a general-purpose rules engine.

## Decision

Create a future starter pair tentatively named:

- `rules:rule-decision-autoconfigure`
- `rules:rule-decision-starter`

The starter should provide a small decision abstraction and Spring Boot integration points. It should not own the application rule schema or mandate a storage backend.

## Scope

The starter should provide:

- `RuleDecisionClient` as the primary application API.
- `RuleDecisionRequest` carrying a decision key, subject, attributes, and optional default.
- `RuleDecisionResult` carrying the selected value, matched rule id/version when known, reason, and metadata.
- `RuleDecisionProvider` as the backend extension point implemented by applications or adapters.
- `RuleDecisionRecorder` as an optional hook for evaluation telemetry.
- Auto-configuration that creates `RuleDecisionClient` when a `RuleDecisionProvider` exists.
- A logging fallback recorder when no recorder is provided.
- Test-support fakes for recording decisions and scripted provider responses.

The starter should integrate with existing bricks instead of duplicating them:

- Use `audit-log-starter` for rule create/update/delete/enable/disable audit events in admin flows.
- Use `outbox-starter` for rule change events that other systems must consume.
- Use `concurrency-control-starter` for rule admin APIs that update versioned rules.
- Use `webmvc-error-starter` for error responses from sample/admin APIs.

## Out Of Scope

The starter should not provide:

- A full expression language.
- A hosted rule management UI.
- A required database schema.
- Automatic remote config synchronization.
- A mandatory feature flag vendor adapter.
- Domain-specific rule definitions such as tenant, plan, or operator semantics.

Applications may still build these pieces locally or through separate adapters.

## API Shape

The first API should stay deliberately small:

```kotlin
data class RuleDecisionRequest<T>(
    val key: String,
    val subject: RuleSubject? = null,
    val attributes: Map<String, Any?> = emptyMap(),
    val defaultValue: T,
)

data class RuleDecisionResult<T>(
    val key: String,
    val value: T,
    val matchedRuleId: String? = null,
    val matchedRuleVersion: Long? = null,
    val reason: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

interface RuleDecisionClient {
    fun <T> decide(request: RuleDecisionRequest<T>): RuleDecisionResult<T>
}

interface RuleDecisionProvider {
    fun <T> decide(request: RuleDecisionRequest<T>): RuleDecisionResult<T>
}
```

This shape keeps the application-facing API stable while letting a provider use a database, file, remote config service, or in-memory rules.

## Auto-Configuration

Suggested default behavior:

- Conditional root property: `kopring.bricks.rule-decision.enabled=true` by default.
- Create `DefaultRuleDecisionClient` only when a `RuleDecisionProvider` bean exists.
- Back off when the application defines a `RuleDecisionClient`.
- Create a logging `RuleDecisionRecorder` when no recorder bean exists.
- Do not create storage beans by default.
- Do not add JDBC dependencies from the starter.

This mirrors the audit/outbox direction: logging-only and app-owned backends must remain reachable without forced infrastructure.

## Configuration

Initial properties should be operational rather than domain-specific:

```yaml
kopring:
  bricks:
    rule-decision:
      enabled: true
      recording:
        enabled: true
        include-attributes: false
```

Do not add properties for rule syntax, rollout percentages, or storage until a concrete reusable use case appears.

## Testing Strategy

Library tests should cover:

- Auto-configuration creates a client when a provider exists.
- Auto-configuration backs off when an application client exists.
- No client is created when no provider exists.
- Default recorder backs off when an application recorder exists.
- Result metadata and matched rule fields pass through unchanged.

Application sample tests should cover:

- A sample todo/admin flow calls `RuleDecisionClient`.
- A rule admin update uses `concurrency-control-starter`.
- A rule change emits audit and outbox events.

## Consequences

This starter makes rule-based branches easier to wire consistently, but keeps rule ownership in applications. It also gives application agents a clear path: use the starter API for decision calls, then request new bricks capabilities only when repeated boilerplate appears across applications.

The tradeoff is that the first version will not feel like a complete feature flag product. That is intentional. The starter should become broader only when real application usage identifies stable abstractions.
