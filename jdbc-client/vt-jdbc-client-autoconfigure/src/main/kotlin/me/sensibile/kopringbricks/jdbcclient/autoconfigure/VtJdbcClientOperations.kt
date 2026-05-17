package me.sensibile.kopringbricks.jdbcclient.autoconfigure

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier

import org.springframework.jdbc.core.simple.JdbcClient

class VtJdbcClientOperations(
    private val jdbcClient: JdbcClient,
    private val executor: Executor,
) {

    fun <T> submit(block: (JdbcClient) -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(Supplier { block(jdbcClient) }, executor)
}
