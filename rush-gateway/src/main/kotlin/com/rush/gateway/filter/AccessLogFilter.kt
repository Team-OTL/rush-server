package com.rush.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class AccessLogFilter :
    WebFilter,
    Ordered {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val requestId = UUID.randomUUID().toString()
        val request =
            exchange.request
                .mutate()
                .headers { it.set(REQUEST_ID_HEADER, requestId) }
                .build()
        exchange.response.headers.set(REQUEST_ID_HEADER, requestId)

        val path = exchange.request.path.value()
        if (path.startsWith("/actuator/")) return chain.filter(exchange.mutate().request(request).build())

        val start = System.nanoTime()
        return chain.filter(exchange.mutate().request(request).build()).doFinally {
            log.info(
                "{} {} {} {}ms requestId={} userId={}",
                exchange.request.method,
                path,
                exchange.response.statusCode?.value(),
                (System.nanoTime() - start) / 1_000_000,
                requestId,
                exchange.getAttribute<String>(JwtAuthenticationFilter.USER_ID_HEADER) ?: "-",
            )
        }
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }
}
