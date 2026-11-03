package com.rush.gateway.filter

import com.rush.gateway.logging.RequestContext
import com.rush.gateway.security.ClientIpResolver
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.UUID

/**
 * 가장 바깥 필터입니다. 요청마다 X-Request-Id를 새로 만들어 서비스와 응답에 넘기고,
 * 요청이 끝나면 접근 로그를 한 줄 남깁니다. 400·401·403 같은 게이트웨이 응답도 기록됩니다.
 */
@Component
class AccessLogFilter(
    private val clientIpResolver: ClientIpResolver,
) : WebFilter,
    Ordered {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val requestId = UUID.randomUUID().toString()
        val clientIp = clientIpResolver.resolve(exchange.request)
        RequestContext.put(exchange, RequestContext.REQUEST_ID, requestId)
        RequestContext.put(exchange, RequestContext.CLIENT_IP, clientIp)

        val request =
            exchange.request
                .mutate()
                .headers { it.set(REQUEST_ID_HEADER, requestId) }
                .build()
        exchange.response.headers.set(REQUEST_ID_HEADER, requestId)
        val mutated = exchange.mutate().request(request).build()
        val context = Context.of(RequestContext.REQUEST_ID, requestId, RequestContext.CLIENT_IP, clientIp)

        val path = exchange.request.path.value()
        if (path.startsWith("/actuator/")) return chain.filter(mutated).contextWrite(context)

        val start = System.nanoTime()
        return chain
            .filter(mutated)
            .doFinally { log(exchange, path, start) }
            .contextWrite(context)
    }

    private fun log(
        exchange: ServerWebExchange,
        path: String,
        start: Long,
    ) {
        val status = exchange.response.statusCode?.value()
        val latencyMs = (System.nanoTime() - start) / 1_000_000
        val routeId = exchange.getAttribute<Route>(GATEWAY_ROUTE_ATTR)?.id
        RequestContext.withMdc(exchange) {
            log
                .atInfo()
                .addKeyValue("method", exchange.request.method.name())
                .addKeyValue("path", path)
                .addKeyValue("status", status)
                .addKeyValue("latencyMs", latencyMs)
                .addKeyValue("routeId", routeId ?: "-")
                .log("{} {} {} {}ms", exchange.request.method, path, status, latencyMs)
        }
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }
}
