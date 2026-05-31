#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

fail() {
  echo "test-tooling: $*" >&2
  exit 1
}

assert_file_exists() {
  local path="$1"
  [[ -f "$path" ]] || fail "expected file to exist: $path"
}

assert_contains() {
  local path="$1"
  local expected="$2"
  grep -Fq -- "$expected" "$path" || fail "expected $path to contain: $expected"
}

assert_section_not_contains() {
  local path="$1"
  local start="$2"
  local end="$3"
  local unexpected="$4"

  ! awk -v start="$start" -v end="$end" -v unexpected="$unexpected" '
    $0 == start { in_section = 1; next }
    $0 == end { in_section = 0 }
    in_section && index($0, unexpected) > 0 { found = 1 }
    END { exit found ? 0 : 1 }
  ' "$path" || fail "expected $path section $start not to contain: $unexpected"
}

copy_repo() {
  local target="$1"

  rsync -a \
    --exclude .git \
    --exclude .gradle \
    --exclude build \
    --exclude '*/build' \
    "$ROOT_DIR"/ "$target"/
}

test_docs_facts_output() {
  local output_file="$1"

  "$ROOT_DIR/scripts/docs-facts.sh" --output "$output_file"

  assert_file_exists "$output_file"
  assert_contains "$output_file" '- `support:jdbc-autoconfigure` (support)'
  assert_section_not_contains \
    "$output_file" \
    '## Auto Configurations' \
    '## Configuration Properties' \
    '### `support:jdbc-autoconfigure`'
  assert_contains "$output_file" '## Samples'
  assert_contains "$output_file" '- `samples:todo-api`'
}

test_docs_checks() {
  "$ROOT_DIR/scripts/docs-facts.sh" --check
  "$ROOT_DIR/scripts/docs-coverage-check.sh"
}

test_new_starter_for_new_domain() {
  local repo_copy="$1"

  (
    cd "$repo_copy"
    scripts/new-starter.sh rules rule-decision \
      --package-segment rules.decision \
      --class-prefix RuleDecision \
      --display-name "Rule Decision" \
      --description "Rule decision wiring for Spring applications" >/dev/null
  )

  assert_contains "$repo_copy/settings.gradle.kts" 'include("rules:rule-decision-autoconfigure")'
  assert_contains "$repo_copy/settings.gradle.kts" 'include("rules:rule-decision-starter")'
  assert_file_exists "$repo_copy/rules/rule-decision-starter/src/test/java/me/sensibile/kopringbricks/rules/decision/starter/RuleDecisionStarterSmokeTests.java"
  assert_file_exists "$repo_copy/rules/rule-decision-autoconfigure/src/test/kotlin/me/sensibile/kopringbricks/rules/decision/autoconfigure/RuleDecisionAutoConfigurationTests.kt"
  assert_contains "$repo_copy/rules/rule-decision-autoconfigure/src/test/kotlin/me/sensibile/kopringbricks/rules/decision/autoconfigure/RuleDecisionAutoConfigurationTests.kt" 'assertThat(context).hasSingleBean(RuleDecisionProperties::class.java)'
  assert_contains "$repo_copy/rules/rule-decision-autoconfigure/src/test/kotlin/me/sensibile/kopringbricks/rules/decision/autoconfigure/RuleDecisionAutoConfigurationTests.kt" 'assertThat(context).doesNotHaveBean(RuleDecisionProperties::class.java)'
  assert_contains "$repo_copy/rules/rule-decision-starter/src/test/java/me/sensibile/kopringbricks/rules/decision/starter/RuleDecisionStarterSmokeTests.java" 'exposesConfigurationPropertiesOnClasspath'

  local order
  order="$(awk '
    /include\("rules:rule-decision-starter"\)/ { rules = NR }
    /include\("test-support:kopring-bricks-test-support"\)/ { test_support = NR }
    END {
      if (rules > 0 && test_support > 0 && rules < test_support) {
        print "ok"
      }
    }
  ' "$repo_copy/settings.gradle.kts")"
  [[ "$order" == "ok" ]] || fail "expected new domain includes before test-support"
  [[ -z "$(find "$repo_copy" -maxdepth 1 -name '.settings.gradle.kts.*' -print -quit)" ]] ||
    fail "temporary settings file was left behind"
}

test_new_starter_for_existing_domain() {
  local repo_copy="$1"

  (
    cd "$repo_copy"
    scripts/new-starter.sh messaging kafka-producer \
      --package-segment messaging.kafka \
      --class-prefix KafkaProducer \
      --display-name "Kafka Producer" \
      --description "Kafka producer defaults for Spring applications" >/dev/null
  )

  assert_file_exists "$repo_copy/messaging/kafka-producer-starter/src/test/java/me/sensibile/kopringbricks/messaging/kafka/starter/KafkaProducerStarterSmokeTests.java"

  local order
  order="$(awk '
    /include\("messaging:outbox-starter"\)/ { outbox = NR }
    /include\("messaging:kafka-producer-autoconfigure"\)/ { autoconfigure = NR }
    /include\("messaging:kafka-producer-starter"\)/ { starter = NR }
    /include\("test-support:kopring-bricks-test-support"\)/ { test_support = NR }
    END {
      if (outbox > 0 && autoconfigure > outbox && starter == autoconfigure + 1 && starter < test_support) {
        print "ok"
      }
    }
  ' "$repo_copy/settings.gradle.kts")"
  [[ "$order" == "ok" ]] || fail "expected existing domain includes after current messaging modules"
}

tmp_root="$(mktemp -d "${TMPDIR:-/tmp}/kopring-bricks-tooling-tests.XXXXXX")"
trap 'rm -rf "$tmp_root"' EXIT

test_docs_facts_output "$tmp_root/project-facts.md"
test_docs_checks

new_domain_copy="$tmp_root/new-domain"
existing_domain_copy="$tmp_root/existing-domain"
mkdir -p "$new_domain_copy" "$existing_domain_copy"
copy_repo "$new_domain_copy"
copy_repo "$existing_domain_copy"
test_new_starter_for_new_domain "$new_domain_copy"
test_new_starter_for_existing_domain "$existing_domain_copy"

echo "tooling tests passed"
