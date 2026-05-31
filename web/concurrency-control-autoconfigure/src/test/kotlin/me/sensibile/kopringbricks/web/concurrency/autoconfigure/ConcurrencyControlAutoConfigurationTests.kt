package me.sensibile.kopringbricks.web.concurrency.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.function.Supplier
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConcurrencyControlAutoConfigurationTests {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(ConcurrencyControlAutoConfiguration::class.java)

    @Test
    fun `creates concurrency control primitives`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(ConcurrencyControlProperties::class.java)
            assertThat(context).hasSingleBean(ETagGenerator::class.java)
            assertThat(context).hasSingleBean(IfMatchValidator::class.java)
            assertThat(context).hasSingleBean(IdempotencyKeyResolver::class.java)
        }
    }

    @Test
    fun `can disable auto configuration`() {
        contextRunner
            .withPropertyValues("kopring.bricks.concurrency-control.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(ConcurrencyControlProperties::class.java)
                assertThat(context).doesNotHaveBean(ETagGenerator::class.java)
                assertThat(context).doesNotHaveBean(IfMatchValidator::class.java)
                assertThat(context).doesNotHaveBean(IdempotencyKeyResolver::class.java)
            }
    }

    @Test
    fun `backs off when custom etag generator is registered`() {
        contextRunner
            .withBean(ETagGenerator::class.java, Supplier { ETagGenerator { "\"custom-$it\"" } })
            .run { context ->
                val generator = context.getBean(ETagGenerator::class.java)

                assertThat(generator.generate(1)).isEqualTo("\"custom-1\"")
            }
    }

    @Test
    fun `backs off when custom if match validator is registered`() {
        val customValidator =
            IfMatchValidator(
                ETagGenerator { "\"custom-$it\"" },
                ConcurrencyControlProperties(),
            )

        contextRunner
            .withBean(IfMatchValidator::class.java, Supplier { customValidator })
            .run { context ->
                assertThat(context).hasSingleBean(IfMatchValidator::class.java)
                assertThat(context.getBean(IfMatchValidator::class.java)).isSameAs(customValidator)
            }
    }

    @Test
    fun `backs off when custom idempotency key resolver is registered`() {
        val customResolver = IdempotencyKeyResolver(ConcurrencyControlProperties())

        contextRunner
            .withBean(IdempotencyKeyResolver::class.java, Supplier { customResolver })
            .run { context ->
                assertThat(context).hasSingleBean(IdempotencyKeyResolver::class.java)
                assertThat(context.getBean(IdempotencyKeyResolver::class.java)).isSameAs(customResolver)
            }
    }

    @Test
    fun `generates strong etag by default`() {
        val generator = DefaultETagGenerator(ConcurrencyControlProperties())

        assertThat(generator.generate(7)).isEqualTo("\"7\"")
    }

    @Test
    fun `can generate weak etag`() {
        val generator =
            DefaultETagGenerator(
                ConcurrencyControlProperties(
                    etag = ConcurrencyControlProperties.ETag(strong = false),
                ),
            )

        assertThat(generator.generate(7)).isEqualTo("W/\"7\"")
    }

    @Test
    fun `if match validator accepts matching etag`() {
        val validator = defaultValidator()

        val currentETag = validator.requireMatch("\"1\", \"7\"", 7)

        assertThat(currentETag).isEqualTo("\"7\"")
    }

    @Test
    fun `if match validator accepts wildcard`() {
        val validator = defaultValidator()

        val currentETag = validator.requireMatch("*", 7)

        assertThat(currentETag).isEqualTo("\"7\"")
    }

    @Test
    fun `if match validator requires header`() {
        val validator = defaultValidator()

        assertThatThrownBy { validator.requireMatch(null, 7) }
            .isInstanceOf(PreconditionRequiredException::class.java)
            .extracting("statusCode")
            .isEqualTo(HttpStatus.PRECONDITION_REQUIRED)
    }

    @Test
    fun `if match validator rejects stale etag`() {
        val validator = defaultValidator()

        val exception =
            assertFailsWith<PreconditionFailedException> {
                validator.requireMatch("\"6\"", 7)
            }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.PRECONDITION_FAILED)
        assertThat(exception.body.properties).containsEntry("currentETag", "\"7\"")
    }

    @Test
    fun `resolves idempotency key from configured header`() {
        val resolver =
            IdempotencyKeyResolver(
                ConcurrencyControlProperties(
                    idempotency = ConcurrencyControlProperties.Idempotency(headerName = "X-Idempotency-Key"),
                ),
            )
        val headers = HttpHeaders().apply { add("X-Idempotency-Key", " request-1 ") }

        assertThat(resolver.resolve(headers)).isEqualTo("request-1")
    }

    @Test
    fun `idempotency conflict exposes key`() {
        val exception = IdempotencyConflictException("request-1")

        assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(exception.body.properties).containsEntry("idempotencyKey", "request-1")
    }

    @Test
    fun `version conflict exposes current version`() {
        val exception = VersionConflictException(currentVersion = 7)

        assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(exception.body.properties).containsEntry("currentVersion", 7)
    }

    private fun defaultValidator(): IfMatchValidator =
        IfMatchValidator(
            DefaultETagGenerator(ConcurrencyControlProperties()),
            ConcurrencyControlProperties(),
        )
}
