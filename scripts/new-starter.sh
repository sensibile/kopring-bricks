#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DRY_RUN=false
DOMAIN=""
NAME=""
PACKAGE_SEGMENT=""
CLASS_PREFIX=""
DISPLAY_NAME=""
DESCRIPTION=""

usage() {
  cat <<'EOF'
Usage: scripts/new-starter.sh [options] <domain> <name>

Creates a new starter pair:
  <domain>/<name>-autoconfigure
  <domain>/<name>-starter

Options:
  --package-segment <value> package segment below me.sensibile.kopringbricks
  --class-prefix <value>    Kotlin class prefix, e.g. ProblemDetails
  --display-name <value>    POM display name, e.g. Problem Details
  --description <value>     POM description base
  --dry-run                 print planned changes without writing files
  --help                    show this help

Example:
  scripts/new-starter.sh messaging kafka-producer \
    --package-segment messaging.kafka \
    --class-prefix KafkaProducer \
    --display-name "Kafka Producer" \
    --description "Kafka producer defaults for Spring applications"
EOF
}

die() {
  echo "new-starter: $*" >&2
  exit 1
}

kebab_to_pascal() {
  local input="$1"
  local output=""
  local part

  IFS='-' read -ra parts <<< "$input"
  for part in "${parts[@]}"; do
    [[ -n "$part" ]] || continue
    output+="${part^}"
  done

  printf '%s' "$output"
}

kebab_to_package_segment() {
  printf '%s' "$1" | tr '-' '.'
}

kebab_to_words() {
  local input="$1"
  local output=""
  local part

  IFS='-' read -ra parts <<< "$input"
  for part in "${parts[@]}"; do
    [[ -n "$part" ]] || continue
    if [[ -n "$output" ]]; then
      output+=" "
    fi
    output+="${part^}"
  done

  printf '%s' "$output"
}

validate_kebab() {
  local label="$1"
  local value="$2"

  [[ "$value" =~ ^[a-z][a-z0-9]*(-[a-z0-9]+)*$ ]] ||
    die "$label must be kebab-case: $value"
}

write_file() {
  local path="$1"
  local content="$2"

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "create $path"
    return 0
  fi

  mkdir -p "$(dirname "$path")"
  printf '%s' "$content" > "$path"
}

append_include() {
  local module_path="$1"
  local include_line="include(\"$module_path\")"

  if grep -Fqx "$include_line" "$ROOT_DIR/settings.gradle.kts"; then
    return 0
  fi

  if [[ "$DRY_RUN" == "true" ]]; then
    echo "append settings.gradle.kts: $include_line"
    return 0
  fi

  printf '\n%s\n' "$include_line" >> "$ROOT_DIR/settings.gradle.kts"
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --package-segment)
      PACKAGE_SEGMENT="${2:-}"
      shift 2
      ;;
    --class-prefix)
      CLASS_PREFIX="${2:-}"
      shift 2
      ;;
    --display-name)
      DISPLAY_NAME="${2:-}"
      shift 2
      ;;
    --description)
      DESCRIPTION="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    --*)
      die "unknown option: $1"
      ;;
    *)
      if [[ -z "$DOMAIN" ]]; then
        DOMAIN="$1"
      elif [[ -z "$NAME" ]]; then
        NAME="$1"
      else
        die "unexpected argument: $1"
      fi
      shift
      ;;
  esac
done

[[ -n "$DOMAIN" ]] || die "domain is required"
[[ -n "$NAME" ]] || die "name is required"
validate_kebab "domain" "$DOMAIN"
validate_kebab "name" "$NAME"

PACKAGE_SEGMENT="${PACKAGE_SEGMENT:-$(kebab_to_package_segment "$DOMAIN").$(kebab_to_package_segment "$NAME")}"
CLASS_PREFIX="${CLASS_PREFIX:-$(kebab_to_pascal "$NAME")}"
DISPLAY_NAME="${DISPLAY_NAME:-$(kebab_to_words "$NAME")}"
DESCRIPTION="${DESCRIPTION:-Spring Boot starter for $DISPLAY_NAME.}"

[[ "$PACKAGE_SEGMENT" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$ ]] ||
  die "package segment must be dot-separated lowercase identifiers: $PACKAGE_SEGMENT"
[[ "$CLASS_PREFIX" =~ ^[A-Z][A-Za-z0-9]*$ ]] ||
  die "class prefix must be PascalCase: $CLASS_PREFIX"

autoconfigure_module="$NAME-autoconfigure"
starter_module="$NAME-starter"
autoconfigure_dir="$ROOT_DIR/$DOMAIN/$autoconfigure_module"
starter_dir="$ROOT_DIR/$DOMAIN/$starter_module"
package_name="me.sensibile.kopringbricks.$PACKAGE_SEGMENT.autoconfigure"
package_dir="$(printf '%s' "$package_name" | tr '.' '/')"
autoconfiguration_class="${CLASS_PREFIX}AutoConfiguration"
properties_class="${CLASS_PREFIX}Properties"
test_class="${CLASS_PREFIX}AutoConfigurationTests"
property_prefix="kopring.bricks.$NAME"

[[ ! -e "$autoconfigure_dir" ]] || die "directory already exists: $autoconfigure_dir"
[[ ! -e "$starter_dir" ]] || die "directory already exists: $starter_dir"

write_file "$autoconfigure_dir/build.gradle.kts" "plugins {
    id(\"kopring.kotlin-autoconfigure-conventions\")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
}

publishing {
    publications {
        create<MavenPublication>(\"mavenJava\") {
            from(components[\"java\"])
            pom {
                name = \"Kopring Bricks $DISPLAY_NAME Autoconfigure\"
                description = \"$DESCRIPTION\"
                url = \"https://github.com/sensibile/kopring-bricks\"
            }
        }
    }
}
"

write_file "$starter_dir/build.gradle.kts" "plugins {
    id(\"kopring.starter-conventions\")
}

dependencies {
    api(project(\":$DOMAIN:$autoconfigure_module\"))
}

publishing {
    publications {
        create<MavenPublication>(\"mavenJava\") {
            from(components[\"java\"])
            pom {
                name = \"Kopring Bricks $DISPLAY_NAME Starter\"
                description = \"$DESCRIPTION\"
                url = \"https://github.com/sensibile/kopring-bricks\"
            }
        }
    }
}
"

write_file "$autoconfigure_dir/src/main/kotlin/$package_dir/$properties_class.kt" "package $package_name

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(\"$property_prefix\")
data class $properties_class(
    val enabled: Boolean = true,
)
"

write_file "$autoconfigure_dir/src/main/kotlin/$package_dir/$autoconfiguration_class.kt" "package $package_name

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties

@AutoConfiguration
@ConditionalOnProperty(
    prefix = \"$property_prefix\",
    name = [\"enabled\"],
    havingValue = \"true\",
    matchIfMissing = true,
)
@EnableConfigurationProperties($properties_class::class)
class $autoconfiguration_class
"

write_file "$autoconfigure_dir/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" "$package_name.$autoconfiguration_class
"

write_file "$autoconfigure_dir/src/test/kotlin/$package_dir/$test_class.kt" "package $package_name

import kotlin.test.Test

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class $test_class {
    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of($autoconfiguration_class::class.java))

    @Test
    fun \`loads when enabled\`() {
        contextRunner.run { context ->
            assert(context.isRunning)
        }
    }

    @Test
    fun \`backs off when disabled\`() {
        contextRunner
            .withPropertyValues(\"$property_prefix.enabled=false\")
            .run { context ->
                assert(context.isRunning)
            }
    }
}
"

append_include "$DOMAIN:$autoconfigure_module"
append_include "$DOMAIN:$starter_module"

if [[ "$DRY_RUN" == "true" ]]; then
  exit 0
fi

echo "Created starter modules:"
echo "  $DOMAIN/$autoconfigure_module"
echo "  $DOMAIN/$starter_module"
echo
echo "Next steps:"
echo "  ./gradlew :$DOMAIN:$autoconfigure_module:test"
echo "  update README.md with installation and configuration examples"
