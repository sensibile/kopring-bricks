#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
README_PATH="$ROOT_DIR/README.md"
AGENT_GUIDE_PATH="$ROOT_DIR/docs/application-agent-guide.md"

missing=()

module_name() {
  printf '%s' "${1##*:}"
}

requires_docs() {
  local name
  name="$(module_name "$1")"

  [[ "$name" == *-starter || "$name" == *-test-support ]]
}

while IFS= read -r module; do
  requires_docs "$module" || continue

  name="$(module_name "$module")"
  if ! grep -Fq "$name" "$README_PATH"; then
    missing+=("README.md: $name")
  fi
  if ! grep -Fq "$name" "$AGENT_GUIDE_PATH"; then
    missing+=("docs/application-agent-guide.md: $name")
  fi
done < <(sed -n 's/^include("\(.*\)")/\1/p' "$ROOT_DIR/settings.gradle.kts")

if [[ "${#missing[@]}" -gt 0 ]]; then
  {
    echo "Documentation coverage check failed. Missing module references:"
    printf '  - %s\n' "${missing[@]}"
  } >&2
  exit 1
fi
