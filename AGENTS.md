# AGENTS.md

This repository is often worked on by coding agents. Use an isolated worktree and task branch for code changes to avoid collisions with other agents.

## Project Shape

`kopring-bricks` is a collection of opinionated Spring Boot starters for Kotlin
applications. Keep each starter small, reusable, and focused on repeated
application plumbing rather than app-specific domain behavior.

Prefer these boundaries:

- `*-autoconfigure` modules own auto-configuration, properties, default beans,
  and conditional behavior.
- `*-starter` modules are dependency bundles for applications.
- `support/*` modules own internal helper code shared by starters. Do not treat
  them as application-facing starters or document them as dependencies for app
  consumers.
- `test-support` owns reusable fakes, recorders, and helpers for applications
  consuming the starters.
- `samples` demonstrates consumer behavior without becoming the source of
  library abstractions.

Do not add app-local business rules, credentials, deployment settings, or
one-off behavior to reusable starters.

## Default Workflow

Do not work directly on `main` for code changes.

Before starting a task:

```bash
git fetch origin
git worktree add ../kopring-bricks-{task} -b codex/{task} origin/main
cd ../kopring-bricks-{task}
```

Use a short, kebab-case task name. Examples:

- `codex/add-flyway-starter`
- `codex/fix-webmvc-error-handler`
- `codex/refactor-build-logic`

Keep each branch scoped to one task.

## Tidy First

Follow Kent Beck's Tidy First approach.

- Keep behavior-preserving tidies separate from feature changes whenever
  practical.
- Tidy before feature work when local duplication, naming, or structure makes
  the next change harder to reason about.
- Keep tidies small. If a tidy expands beyond the immediate area, stop and make
  it a separate task.
- Do not introduce abstractions speculatively. Extract only after real
  duplication or complexity appears.
- Run tests and relevant lint after tidies.
- If a tidy changes behavior, it is no longer a tidy; treat it as feature or bug
  work and test it explicitly.

## Review Loop

After each implementation pass, review the changed code before handing off.

Prioritize:

- bugs and behavioral regressions
- starter API compatibility
- auto-configuration ordering and back-off behavior
- module dependency boundary regressions
- missing tests for changed behavior
- documentation drift
- generated files that should or should not be committed

Apply necessary fixes found during review in the same task when they are clearly
scoped. Re-run the relevant tests and checks after review fixes. Call out any
remaining risks that are intentionally left unfixed.

## Module And Starter Rules

- Prefer using `scripts/new-starter.sh` or `mise run new-starter` when adding a
  new autoconfigure/starter pair.
- Keep starter APIs thin. Do not force a storage backend, transport, scheduler,
  or domain model unless the starter's purpose explicitly requires it.
- Use Spring Boot conditional annotations so application-provided beans win over
  defaults.
- Keep optional infrastructure optional. If a starter can run without JDBC,
  messaging, web, or observability dependencies, do not add those dependencies
  only for convenience.
- Put reusable test doubles in `test-support` instead of copying them into
  samples or application docs.
- Prefer Gradle module boundaries over package-only boundaries as the project
  grows.

## Verification

Run tests before opening a PR:

```bash
./gradlew test
```

Also run local publishing verification when the task changes Gradle, publishing, dependency metadata, starter modules, or build logic:

```bash
./gradlew publishToMavenLocal
```

Use `mise` tasks for local workflow when available:

```bash
mise run lint
mise run docs:check
mise run docs:facts
```

Run `mise run lint` before committing Kotlin changes. Run `mise run docs:check`
when changing starter lists, module references, README content, generated facts,
or agent-facing documentation.

If verification cannot be run, state why in the PR description.

## Testing Rules

- Prefer focused tests for the behavior changed by the task.
- Use `ApplicationContextRunner` for auto-configuration behavior.
- Test default bean creation, application bean back-off, disabled properties,
  and relevant conditional branches.
- Prefer fakes, stubs, fixtures, and real domain objects over mocking
  frameworks.
- Starter modules should have a thin smoke test that verifies the bundled
  auto-configuration is present on the starter test classpath.
- Add broader tests when touching shared behavior, starter contracts,
  dependency metadata, or sample workflows.

## Lint And Formatting

Do not add Gradle ktlint or detekt plugins unless explicitly requested.

This project uses the same local-tool style as Augur:

- `ktlint` runs as a CLI.
- `detekt` runs as a CLI through `mise`.
- `mise run lint` checks Kotlin style and detekt rules.
- `mise run format:ktlint` formats changed Kotlin files only.
- Formatting ownership belongs to ktlint. Disable or relax detekt rules that
  duplicate formatting concerns when they conflict with ktlint output.

## Documentation

Generated facts are kept in:

```text
docs/generated/project-facts.md
```

Update generated facts after changing modules, dependencies, source
declarations, starter pairs, auto-configuration imports, configuration
properties, or project principles:

```bash
mise run docs:facts
mise run docs:check
```

Use `docs/generated/project-facts.md` as factual source material for README and
human-authored docs. Do not invent modules, APIs, dependencies, properties, or
guarantees not supported by code or generated facts.

Agent-facing documentation includes `AGENTS.md`,
`docs/application-agent-guide.md`, `docs/generated/project-facts.md`, and
`docs/prompts/*`. Keep it factual, explicit, and easy for automation to follow.
README files are human-facing orientation and should not become an exhaustive
agent rulebook.

## Pull Requests

Push the task branch and open a PR into `main`:

```bash
git push -u origin codex/{task}
```

PRs should include:

- summary of changes
- verification commands and results
- notes about any skipped verification
- risks or follow-up work, if any

Do not auto-merge unless the user explicitly asks for it.

## Conflict Rules

If `origin/main` changes while working, update the task branch before final verification.

If merge conflicts occur, stop and report the conflicting files instead of guessing. Do not reset, checkout, delete, or overwrite changes from another agent unless the user explicitly requests it.

## Files To Avoid

Do not commit local state, generated output, IDE files, credentials, or tool-specific runtime files.

Examples:

- `.gradle/`
- `build/`
- `.idea/`
- `.vscode/`
- `.antigravitycli/`
- secrets or local credential files

## Git Hygiene

- Do not revert user changes unless explicitly asked.
- Keep edits scoped to the requested work.
- Avoid unrelated refactors.
- Do not bypass git hooks unless the user explicitly asks and the reason is
  documented in the PR.
- Run tests and relevant `mise` checks before handing off.

## Finishing A Task

After the PR is merged, clean up the local worktree and branch:

```bash
cd ..
git worktree remove kopring-bricks-{task}
git branch -d codex/{task}
```
