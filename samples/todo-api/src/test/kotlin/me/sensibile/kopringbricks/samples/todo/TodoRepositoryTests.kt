package me.sensibile.kopringbricks.samples.todo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TodoRepositoryTests {
    private val repository = TodoRepository()

    @Test
    fun `runs completion validation inside the atomic update`() {
        val todo = repository.save("guarded update")
        val firstValidationStarted = CountDownLatch(1)
        val allowFirstCompletion = CountDownLatch(1)
        val secondValidationStarted = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val firstCompletion =
                executor.submit<Todo> {
                    repository.complete(todo.id) { current ->
                        require(current.version == todo.version)
                        firstValidationStarted.countDown()
                        check(allowFirstCompletion.await(1, TimeUnit.SECONDS))
                    }
                }

            assertThat(firstValidationStarted.await(1, TimeUnit.SECONDS)).isTrue()

            val secondCompletion =
                executor.submit<Todo> {
                    repository.complete(todo.id) { current ->
                        secondValidationStarted.countDown()
                        require(current.version == todo.version) {
                            "expected version ${todo.version} but was ${current.version}"
                        }
                    }
                }

            assertThat(secondValidationStarted.await(100, TimeUnit.MILLISECONDS)).isFalse()

            allowFirstCompletion.countDown()
            assertThat(firstCompletion.get(1, TimeUnit.SECONDS).version).isEqualTo(2)

            assertThatThrownBy {
                secondCompletion.get(1, TimeUnit.SECONDS)
            }.isInstanceOf(ExecutionException::class.java)
                .hasCauseInstanceOf(IllegalArgumentException::class.java)
        } finally {
            executor.shutdownNow()
        }
    }
}
