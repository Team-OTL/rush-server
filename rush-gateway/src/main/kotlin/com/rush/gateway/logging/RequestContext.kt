package com.rush.gateway.logging

import org.slf4j.MDC
import org.springframework.web.server.ServerWebExchange

/**
 * 요청 하나를 추적하는 값(requestId, userId, clientIp)의 키와 보관 방법을 모아 둡니다.
 *
 * WebFilter 체인 안의 로그는 Reactor context → MDC 전파로 이 값을 자동으로 갖습니다.
 * 체인 바깥(ErrorWebExceptionHandler 등)에서는 exchange attribute를 읽어 [withMdc]로 남깁니다.
 */
object RequestContext {
    const val REQUEST_ID = "requestId"
    const val USER_ID = "userId"
    const val CLIENT_IP = "clientIp"

    val KEYS = listOf(REQUEST_ID, USER_ID, CLIENT_IP)

    private fun attribute(key: String) = "rush.gateway.$key"

    fun put(
        exchange: ServerWebExchange,
        key: String,
        value: String,
    ) {
        exchange.attributes[attribute(key)] = value
    }

    fun get(
        exchange: ServerWebExchange,
        key: String,
    ): String? = exchange.getAttribute<String>(attribute(key))

    /** exchange에 저장된 값을 MDC에 넣은 채로 [block]을 실행합니다. */
    fun <T> withMdc(
        exchange: ServerWebExchange,
        block: () -> T,
    ): T {
        val previous = MDC.getCopyOfContextMap()
        KEYS.forEach { key -> get(exchange, key)?.let { MDC.put(key, it) } }
        try {
            return block()
        } finally {
            if (previous == null) MDC.clear() else MDC.setContextMap(previous)
        }
    }
}
