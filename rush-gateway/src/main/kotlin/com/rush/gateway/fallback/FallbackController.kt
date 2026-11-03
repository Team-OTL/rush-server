package com.rush.gateway.fallback

import com.rush.gateway.error.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.util.concurrent.TimeoutException

@RestController
class FallbackController {
    private val log = LoggerFactory.getLogger(javaClass)

    @RequestMapping("/fallback")
    fun fallback(exchange: ServerWebExchange): ResponseEntity<ErrorResponse> {
        val cause = exchange.getAttribute<Throwable>(CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR)
        val status = if (cause is TimeoutException) HttpStatus.GATEWAY_TIMEOUT else HttpStatus.SERVICE_UNAVAILABLE
        log.warn(
            "fallback route={} status={} cause={}",
            exchange.getAttribute<Route>(GATEWAY_ROUTE_ATTR)?.id,
            status.value(),
            cause.toString(),
        )
        return ResponseEntity.status(status).body(ErrorResponse.of(status))
    }
}
