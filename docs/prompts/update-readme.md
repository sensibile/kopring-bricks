# Update README Prompt

Use `docs/generated/project-facts.md` as the factual source of truth for README updates.

Update `README.md` when any of these change:

- Gradle modules are added, removed, or renamed.
- Starter/autoconfigure pairings change.
- Starter dependencies change.
- Auto-configuration classes change.
- Environment post-processors change.
- `@ConfigurationProperties` prefixes or classes change.
- Sample applications change.

Rules:

- Do not invent modules, properties, dependencies, or examples that are not supported by code.
- Keep `docs/generated/project-facts.md` factual and generated; edit the generator instead of manually editing generated facts.
- Keep `README.md` human-authored and concise.
- Prefer short installation, configuration, and usage examples over exhaustive API listings.
- If code and README disagree, treat code plus `docs/generated/project-facts.md` as the source of truth.

Suggested workflow:

```bash
scripts/docs-facts.sh
git diff -- docs/generated/project-facts.md README.md
```

Then update `README.md` only where the generated facts reveal drift or missing documentation.
