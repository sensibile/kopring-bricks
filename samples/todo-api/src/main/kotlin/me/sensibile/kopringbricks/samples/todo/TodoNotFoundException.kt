package me.sensibile.kopringbricks.samples.todo

import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import org.springframework.http.HttpStatus

class TodoNotFoundException(
    id: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "TODO_NOT_FOUND",
        detail = "Todo $id was not found",
        title = "Todo not found",
    )
