package com.rush.gateway.error

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.util.concurrent.TimeoutException

@Order(-2)
@Component
class GlobalErrorHandler(
    private val writer: ErrorResponseWriter,
) : ErrorWebExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        exchange: ServerWebExchange,
        ex: Throwable,
    ): Mono<Void> {
        if (exchange.response.isCommitted) return Mono.error(ex)

        val status = statusOf(ex)
        if (status.is5xxServerError) {
            log.error("요청 처리 실패 {} {}", exchange.request.method, exchange.request.path, ex)
        }
        return writer.write(exchange, status)
    }

    private fun statusOf(ex: Throwable) =
        when (ex) {
            is ResponseStatusException -> HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
            is ConnectException -> HttpStatus.SERVICE_UNAVAILABLE
            is TimeoutException -> HttpStatus.GATEWAY_TIMEOUT
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
