package me.sensibile.kopringbricks.observability.logging.autoconfigure

import org.slf4j.MDC

internal object MdcContext {

    fun capture(): Map<String, String>? =
        MDC.getCopyOfContextMap()

    fun apply(context: Map<String, String>?) {
        if (context.isNullOrEmpty()) {
            MDC.clear()
        } else {
            MDC.setContextMap(context)
        }
    }

    fun restore(context: Map<String, String>?) {
        apply(context)
    }
}
