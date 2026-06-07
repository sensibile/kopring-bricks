package me.sensibile.kopringbricks.support.jdbc.autoconfigure.test

import org.springframework.jdbc.core.simple.JdbcClient

fun JdbcClient.tableExists(tableName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from information_schema.tables
            where table_schema = 'public'
              and table_name = :tableName
              and table_type = 'BASE TABLE'
        )
        """.trimIndent(),
    ).param("tableName", tableName)
        .query(Boolean::class.java)
        .single()

fun JdbcClient.indexExists(indexName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from pg_indexes
            where schemaname = 'public'
              and indexname = :indexName
        )
        """.trimIndent(),
    ).param("indexName", indexName)
        .query(Boolean::class.java)
        .single()

fun JdbcClient.uniqueConstraintExists(constraintName: String): Boolean =
    sql(
        """
        select exists (
            select 1
            from information_schema.table_constraints
            where constraint_schema = 'public'
              and constraint_type = 'UNIQUE'
              and constraint_name = :constraintName
        )
        """.trimIndent(),
    ).param("constraintName", constraintName)
        .query(Boolean::class.java)
        .single()
