package com.rush.gateway.fallback

import com.rush.gateway.response.CommonApiResponse
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory.CircuitBreakerStatusCodeException
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
    fun fallback(exchange: ServerWebExchange): ResponseEntity<CommonApiResponse> {
        val cause = exchange.getAttribute<Throwable>(CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR)
        val status = statusOf(cause)
        log.warn(
            "fallback route={} status={} cause={}",
            exchange.getAttribute<Route>(GATEWAY_ROUTE_ATTR)?.id,
            status.value(),
            cause.toString(),
        )
        return ResponseEntity.status(status).body(CommonApiResponse.error(status))
    }

    // 응답 지연은 504, 서비스가 5xx를 돌려줬으면 502, 차단기가 열렸거나 연결에 실패했으면 503입니다.
    private fun statusOf(cause: Throwable?) =
        when (cause) {
            is TimeoutException -> HttpStatus.GATEWAY_TIMEOUT
            is CircuitBreakerStatusCodeException -> HttpStatus.BAD_GATEWAY
            else -> HttpStatus.SERVICE_UNAVAILABLE
        }
}
