# Release Guide

This project publishes all modules with Gradle `maven-publish`.

## Local Verification

Run the same checks expected before release-oriented changes:

```bash
./gradlew test
./gradlew publishToMavenLocal
scripts/docs-facts.sh --check
scripts/docs-coverage-check.sh
scripts/test-tooling.sh
```

Run `mise run lint` when Kotlin source or build logic changes.

## GitHub Packages

GitHub Packages publishing is handled by `.github/workflows/build.yml`.

The workflow publishes on:

- pushes to `main`
- pushes of tags matching `v*`
- manual `workflow_dispatch` runs

Pull requests run tests only and do not publish packages.

The workflow uses:

- `GITHUB_ACTOR` as the package username
- `GITHUB_TOKEN` as the package password
- the Gradle `publish` task

## Release Flow

1. Merge the release-ready changes to `main`.
2. Confirm the `main` build succeeds.
3. For a tagged release, create and push a `v*` tag.
4. Confirm the publish job completes.
5. Check GitHub Packages for the expected module versions.

Current snapshots use the root Gradle version from `build.gradle.kts`.
