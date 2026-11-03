package com.rush.gateway.filter

import com.rush.gateway.error.ErrorResponseWriter
import com.rush.gateway.security.ClientIpResolver
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * 요청을 서비스로 넘기기 전에 정화합니다.
 *
 * 1. 형식이 잘못된 요청(경로 조작, 제어문자 헤더)은 400으로 막습니다.
 * 2. 게이트웨이가 값을 책임지는 헤더는 클라이언트가 보낸 값을 지우고 올바른 값으로 다시 씁니다.
 *    X-User-Id는 JwtAuthenticationFilter가, X-Request-Id는 AccessLogFilter가 다시 씁니다.
 *
 * AccessLogFilter 바로 뒤, Spring Security(-100) 앞에서 실행되어 400 응답도 접근 로그에 남습니다.
 */
@Component
class RequestSanitizeFilter(
    private val clientIpResolver: ClientIpResolver,
    private val errorResponseWriter: ErrorResponseWriter,
) : WebFilter,
    Ordered {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        if (!RequestValidator.isValidPath(request.uri.rawPath) || !RequestValidator.isValidHeaders(request.headers)) {
            return errorResponseWriter.write(exchange, HttpStatus.BAD_REQUEST)
        }

        val clientIp = clientIpResolver.resolve(request)
        val fromTrustedProxy = clientIpResolver.isFromTrustedProxy(request)
        val forwarded = forwardedValues(exchange, fromTrustedProxy)

        val sanitized =
            request
                .mutate()
                .headers { headers ->
                    headers.keys
                        .filter { name -> OWNED_HEADERS.any { it.equals(name, true) } || name.startsWith(INTERNAL_PREFIX, true) }
                        .forEach(headers::remove)
                    headers.set(X_REAL_IP, clientIp)
                    headers.set(ClientIpResolver.X_FORWARDED_FOR, clientIp)
                    headers.set(X_FORWARDED_PROTO, forwarded.proto)
                    headers.set(X_FORWARDED_HOST, forwarded.host)
                    headers.set(X_FORWARDED_PORT, forwarded.port)
                }.build()
        return chain.filter(exchange.mutate().request(sanitized).build())
    }

    // 신뢰 프록시를 거친 요청은 프록시가 보낸 값을, 그 외에는 게이트웨이가 받은 요청 값을 씁니다.
    private fun forwardedValues(
        exchange: ServerWebExchange,
        fromTrustedProxy: Boolean,
    ): Forwarded {
        val uri = exchange.request.uri
        val headers = exchange.request.headers
        val proto = uri.scheme ?: "http"
        val own =
            Forwarded(
                proto = proto,
                // IPv6 Host([::1]:8080)도 대괄호를 포함한 호스트만 남도록 URI에서 읽습니다.
                host = uri.host.orEmpty(),
                port =
                    (
                        if (uri.port != -1) {
                            uri.port
                        } else if (proto == "https") {
                            443
                        } else {
                            80
                        }
                    ).toString(),
            )
        if (!fromTrustedProxy) return own

        return Forwarded(
            proto = headers.getFirst(X_FORWARDED_PROTO) ?: own.proto,
            host = headers.getFirst(X_FORWARDED_HOST) ?: own.host,
            port = headers.getFirst(X_FORWARDED_PORT) ?: own.port,
        )
    }

    private data class Forwarded(
        val proto: String,
        val host: String,
        val port: String,
    )

    override fun getOrder() = ORDER

    companion object {
        const val ORDER = Ordered.HIGHEST_PRECEDENCE + 10
        const val X_REAL_IP = "X-Real-IP"
        const val X_FORWARDED_PROTO = "X-Forwarded-Proto"
        const val X_FORWARDED_HOST = "X-Forwarded-Host"
        const val X_FORWARDED_PORT = "X-Forwarded-Port"
        private const val INTERNAL_PREFIX = "X-Internal-"

        // 클라이언트 값을 믿지 않고 항상 지우는 헤더
        private val OWNED_HEADERS =
            listOf(
                JwtAuthenticationFilter.USER_ID_HEADER,
                X_REAL_IP,
                ClientIpResolver.X_FORWARDED_FOR,
                X_FORWARDED_PROTO,
                X_FORWARDED_HOST,
                X_FORWARDED_PORT,
                "X-Forwarded-Prefix",
                "Forwarded",
            )
    }
}
