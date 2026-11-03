package com.rush.gateway.security

import com.rush.gateway.config.GatewayProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress

class ClientIpResolverTest {
    private fun request(
        remote: String,
        vararg xff: String,
    ) = MockServerHttpRequest
        .get("/shop/ping")
        .remoteAddress(InetSocketAddress(remote, 12345))
        .apply { if (xff.isNotEmpty()) header("X-Forwarded-For", *xff) }
        .build()

    @Test
    fun `신뢰 프록시가 없으면 X-Forwarded-For를 무시하고 연결 IP를 쓴다`() {
        val resolver = ClientIpResolver(GatewayProperties())

        assertEquals("203.0.113.7", resolver.resolve(request("203.0.113.7", "127.0.0.1")))
    }

    @Test
    fun `신뢰 프록시에서 온 요청은 X-Forwarded-For의 마지막 비신뢰 IP를 쓴다`() {
        val resolver = ClientIpResolver(GatewayProperties(trustedProxies = listOf("10.0.0.0/8")))

        // 클라이언트가 앞에 127.0.0.1을 위조해 넣어도 프록시가 붙인 실제 IP가 선택됩니다.
        assertEquals("198.51.100.4", resolver.resolve(request("10.0.0.5", "127.0.0.1, 198.51.100.4, 10.0.0.9")))
    }

    @Test
    fun `X-Forwarded-For가 없으면 신뢰 프록시의 연결 IP를 쓴다`() {
        val resolver = ClientIpResolver(GatewayProperties(trustedProxies = listOf("10.0.0.0/8")))

        assertEquals("10.0.0.5", resolver.resolve(request("10.0.0.5")))
    }

    @Test
    fun `IP가 아닌 값을 만나면 그 직전 IP에서 멈춘다`() {
        val resolver = ClientIpResolver(GatewayProperties(trustedProxies = listOf("10.0.0.0/8")))

        assertEquals("10.0.0.9", resolver.resolve(request("10.0.0.5", "evil.example.com, 10.0.0.9")))
    }

    @Test
    fun `범위를 벗어난 IPv4를 만나면 그 직전 IP에서 멈춘다`() {
        val resolver = ClientIpResolver(GatewayProperties(trustedProxies = listOf("10.0.0.0/8")))

        assertEquals("10.0.0.5", resolver.resolve(request("10.0.0.5", "999.999.999.999")))
    }

    @Test
    fun `IPv6 연결 주소의 scope id는 떼어 낸다`() {
        val resolver = ClientIpResolver(GatewayProperties())
        val loopback = Inet6Address.getByAddress(null, InetAddress.getByName("::1").address, 1)
        val request =
            MockServerHttpRequest
                .get("/shop/ping")
                .remoteAddress(InetSocketAddress(loopback, 12345))
                .build()

        assertEquals("0:0:0:0:0:0:0:1", resolver.resolve(request))
    }
}
