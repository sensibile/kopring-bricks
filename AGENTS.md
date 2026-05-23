# AGENTS.md

This repository is often worked on by coding agents. Use an isolated worktree and task branch for code changes to avoid collisions with other agents.

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

## Verification

Run tests before opening a PR:

```bash
./gradlew test
```

Also run local publishing verification when the task changes Gradle, publishing, dependency metadata, starter modules, or build logic:

```bash
./gradlew publishToMavenLocal
```

If verification cannot be run, state why in the PR description.

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

## Finishing A Task

After the PR is merged, clean up the local worktree and branch:

```bash
cd ..
git worktree remove kopring-bricks-{task}
git branch -d codex/{task}
```
