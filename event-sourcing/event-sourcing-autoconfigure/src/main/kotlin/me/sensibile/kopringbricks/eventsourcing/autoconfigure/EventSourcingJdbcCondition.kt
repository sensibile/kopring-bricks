package me.sensibile.kopringbricks.eventsourcing.autoconfigure

import me.sensibile.kopringbricks.support.jdbc.autoconfigure.PostgresqlJdbcCondition
import org.springframework.boot.autoconfigure.condition.ConditionOutcome
import org.springframework.boot.autoconfigure.condition.SpringBootCondition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class EventSourcingJdbcCondition : SpringBootCondition() {
    override fun getMatchOutcome(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): ConditionOutcome = condition.outcome(context)

    private companion object {
        private val condition =
            PostgresqlJdbcCondition(
                conditionName = "EventSourcingJdbc",
                dialectProperty = "kopring.bricks.event-sourcing.jdbc.dialect",
            )
    }
}
