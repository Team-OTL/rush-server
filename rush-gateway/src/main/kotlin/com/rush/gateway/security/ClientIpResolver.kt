package com.rush.gateway.security

import com.rush.gateway.config.GatewayProperties
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component

/**
 * 클라이언트 IP를 판별합니다. IP 제한, 접근 로그, 헤더 정화가 모두 이 결과를 씁니다.
 *
 * 연결 IP가 신뢰 프록시일 때만 X-Forwarded-For를 오른쪽부터 읽어
 * 신뢰 프록시가 아닌 첫 IP를 클라이언트로 봅니다. 그 외에는 연결 IP를 그대로 씁니다.
 */
@Component
class ClientIpResolver(
    properties: GatewayProperties,
) {
    private val trustedProxies = IpRangeMatcher(properties.trustedProxies)

    fun resolve(request: ServerHttpRequest): String {
        val remote = remoteIp(request) ?: UNKNOWN
        if (!trustedProxies.matches(remote)) return remote

        var client = remote
        val hops =
            request.headers[X_FORWARDED_FOR]
                .orEmpty()
                .flatMap { it.split(',') }
                .map { it.trim() }
        for (hop in hops.asReversed()) {
            if (!IpRangeMatcher.isIpLiteral(hop)) break
            client = hop
            if (!trustedProxies.matches(hop)) break
        }
        return client
    }

    /** 요청이 신뢰 프록시를 거쳐 왔는지 여부입니다. */
    fun isFromTrustedProxy(request: ServerHttpRequest): Boolean = trustedProxies.matches(remoteIp(request))

    // IPv6 연결은 "0:0:0:0:0:0:0:1%0"처럼 scope id가 붙으므로 떼어 냅니다.
    private fun remoteIp(request: ServerHttpRequest): String? =
        request.remoteAddress
            ?.address
            ?.hostAddress
            ?.substringBefore('%')

    companion object {
        const val X_FORWARDED_FOR = "X-Forwarded-For"
        private const val UNKNOWN = "unknown"
    }
}
