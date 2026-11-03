package com.rush.gateway.filter

import com.rush.gateway.error.ErrorResponseWriter
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 게이트웨이에서 JWT를 한 번만 검증하고, 검증된 사용자 ID를 X-User-Id 헤더로 뒤쪽 서비스에 넘깁니다.
 * 뒤쪽 서비스(Spring/Nest/Go)는 헤더만 읽으면 됩니다.
 */
@Component
class JwtAuthenticationFilter(
    @Value("\${jwt.secret}") secret: String,
    private val errorResponseWriter: ErrorResponseWriter,
) : GlobalFilter,
    Ordered {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    // 토큰 없이 접근 가능한 경로
    private val publicPaths = listOf("/auth/", "/actuator/")

    // 각 서비스의 OpenAPI 문서(/{prefix}/v3/api-docs)는 게이트웨이 Swagger UI가 토큰 없이 가져옵니다.
    private val apiDocsPath = Regex("^/[a-z-]+/v3/api-docs(/.*)?$")

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        // 클라이언트가 X-User-Id를 직접 넣어 위조하지 못하게 항상 지웁니다.
        val sanitized = request.mutate().headers { it.remove(USER_ID_HEADER) }.build()

        val path = request.path.value()
        if (publicPaths.any { path.startsWith(it) } || apiDocsPath.matches(path)) {
            return chain.filter(exchange.mutate().request(sanitized).build())
        }

        val token =
            request.headers.getFirst("Authorization")?.removePrefix("Bearer ")
                ?: request.queryParams.getFirst(TOKEN_QUERY) // WebSocket은 헤더를 못 넣는 경우가 많아서 쿼리로도 받습니다.
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

        exchange.attributes[USER_ID_HEADER] = userId
        // 쿼리로 받은 토큰은 검증이 끝났으므로 서비스 쪽 로그에 남지 않게 지웁니다.
        val authed =
            sanitized
                .mutate()
                .uri(withoutTokenQuery(request.uri))
                .header(USER_ID_HEADER, userId)
                .build()
        return chain.filter(exchange.mutate().request(authed).build())
    }

    // 인증은 디코딩된 키로 토큰을 읽으므로, ?%74oken=처럼 인코딩된 키도 디코딩한 뒤 비교해 지웁니다.
    private fun withoutTokenQuery(uri: URI): URI {
        val rawQuery = uri.rawQuery ?: return uri
        val kept = rawQuery.split('&').filterNot { decodedKey(it) == TOKEN_QUERY }
        return UriComponentsBuilder
            .fromUri(uri)
            .replaceQuery(kept.joinToString("&").ifEmpty { null })
            .build(true)
            .toUri()
    }

    private fun decodedKey(param: String): String? =
        runCatching { URLDecoder.decode(param.substringBefore('='), StandardCharsets.UTF_8) }.getOrNull()

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> = errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED)

    override fun getOrder() = -1

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
        private const val TOKEN_QUERY = "token"
    }
}
