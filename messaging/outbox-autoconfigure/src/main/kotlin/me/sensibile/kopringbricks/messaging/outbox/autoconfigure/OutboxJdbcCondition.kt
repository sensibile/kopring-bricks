package me.sensibile.kopringbricks.messaging.outbox.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.PostgresqlJdbcCondition
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class OutboxJdbcCondition : SpringBootCondition() {
    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome = condition.outcome(context)

    private companion object {
        private val condition =
            PostgresqlJdbcCondition(
                conditionName = "OutboxJdbc",
                dialectProperty = "kopring.bricks.outbox.jdbc.dialect",
            )
    }
}
