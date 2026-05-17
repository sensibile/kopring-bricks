package me.sensibile.kopringbricks.observability.logging.autoconfigure

import org.springframework.core.task.TaskDecorator

class MdcTaskDecorator : TaskDecorator {

    override fun decorate(runnable: Runnable): Runnable {
        val callerContext = MdcContext.capture()

        return Runnable {
            val executorContext = MdcContext.capture()
            try {
                MdcContext.apply(callerContext)
                runnable.run()
            } finally {
                MdcContext.restore(executorContext)
            }
        }
    }
}
