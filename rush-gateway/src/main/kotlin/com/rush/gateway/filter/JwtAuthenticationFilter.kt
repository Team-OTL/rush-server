package com.rush.gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * 게이트웨이에서 JWT를 한 번만 검증하고, 검증된 사용자 ID를 X-User-Id 헤더로 뒤쪽 서비스에 넘깁니다.
 * 뒤쪽 서비스(Spring/Nest/Go)는 헤더만 읽으면 됩니다.
 */
@Component
class JwtAuthenticationFilter(
    @Value("\${jwt.secret}") secret: String,
) : GlobalFilter,
    Ordered {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    // 토큰 없이 접근 가능한 경로
    private val publicPaths = listOf("/auth/", "/actuator/")

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        // 클라이언트가 X-User-Id를 직접 넣어 위조하지 못하게 항상 지웁니다.
        val sanitized = request.mutate().headers { it.remove(USER_ID_HEADER) }.build()

        if (publicPaths.any { request.path.value().startsWith(it) }) {
            return chain.filter(exchange.mutate().request(sanitized).build())
        }

        val token =
            request.headers.getFirst("Authorization")?.removePrefix("Bearer ")
                ?: request.queryParams.getFirst("token") // WebSocket은 헤더를 못 넣는 경우가 많아서 쿼리로도 받습니다.
                ?: return unauthorized(exchange)

        val userId =
            runCatching {
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload.subject
            }.getOrNull() ?: return unauthorized(exchange)

        val authed = sanitized.mutate().header(USER_ID_HEADER, userId).build()
        return chain.filter(exchange.mutate().request(authed).build())
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    override fun getOrder() = -1

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
    }
}
