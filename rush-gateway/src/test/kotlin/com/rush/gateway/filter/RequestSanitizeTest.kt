package com.rush.gateway.filter

import com.sun.net.httpserver.HttpServer
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

private const val SECRET = "test-secret-key-for-gateway-sanitize-test-0123456789"

// remoteAddress가 필요하므로 @AutoConfigureWebTestClient(mock 바인딩) 대신 실제 서버로 요청합니다.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
    ],
)
class RequestSanitizeTest {
    @Autowired
    lateinit var client: WebTestClient

    @LocalServerPort
    var port: Int = 0

    @Test
    fun `클라이언트가 위조한 게이트웨이 소유 헤더는 올바른 값으로 바뀐다`() {
        client
            .get()
            .uri("/user/ping")
            .header("Authorization", "Bearer ${token("42")}")
            .header("X-User-Id", "1")
            .header("X-Forwarded-For", "10.0.0.1")
            .header("X-Real-IP", "10.0.0.1")
            .header("X-Forwarded-Host", "evil.example.com")
            .header("X-Internal-Role", "admin")
            .header("X-Request-Id", "forged")
            .exchange()
            .expectStatus()
            .isOk

        val headers = received.get().headers
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("42")
        assertThat(headers["X-Forwarded-For"]).containsExactly("127.0.0.1")
        assertThat(headers.getFirst("X-Real-IP")).isEqualTo("127.0.0.1")
        assertThat(headers.getFirst("X-Forwarded-Host")).isEqualTo("localhost")
        assertThat(headers.getFirst("X-Forwarded-Port")).isEqualTo(port.toString())
        assertThat(headers.getFirst("X-Forwarded-Proto")).isEqualTo("http")
        assertThat(headers.containsKey("X-Internal-Role")).isFalse()
        assertThat(headers.getFirst("X-Request-Id")).isNotEqualTo("forged")
    }

    @Test
    fun `쿼리로 받은 토큰은 서비스에 넘기지 않는다`() {
        client
            .get()
            .uri("/user/ping?token=${token("7")}&room=1")
            .exchange()
            .expectStatus()
            .isOk

        assertThat(received.get().query).isEqualTo("room=1")
        assertThat(received.get().headers.getFirst("X-User-Id")).isEqualTo("7")
    }

    @Test
    fun `경로 조작 요청은 400 공통 응답을 준다`() {
        client
            .get()
            .uri(URI.create("http://localhost:$port/shop/%2e%2e/admin"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("BAD_REQUEST")
    }

    @Test
    fun `제어문자가 든 헤더는 400으로 막는다`() {
        // HTTP 클라이언트는 제어문자 헤더를 보내기 전에 막으므로 소켓으로 직접 보냅니다.
        val response =
            Socket("localhost", port).use { socket ->
                socket.getOutputStream().write(
                    "GET /user/ping HTTP/1.1\r\nHost: localhost\r\nX-Test: a\u0001b\r\nConnection: close\r\n\r\n".toByteArray(),
                )
                socket.getInputStream().bufferedReader().readLine()
            }

        assertThat(response).startsWith("HTTP/1.1 400")
    }

    @Test
    fun `너무 큰 헤더는 431로 거부한다`() {
        client
            .get()
            .uri("/user/ping")
            .header("X-Large", "a".repeat(10 * 1024))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE)
    }

    private data class Received(
        val headers: org.springframework.http.HttpHeaders,
        val query: String?,
    )

    companion object {
        private val received = AtomicReference<Received>()

        private val echoServer =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                createContext("/") { exchange ->
                    val headers = org.springframework.http.HttpHeaders()
                    exchange.requestHeaders.forEach { (name, values) -> headers.addAll(name, values) }
                    received.set(Received(headers, exchange.requestURI.rawQuery))
                    exchange.sendResponseHeaders(200, -1)
                    exchange.close()
                }
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun routes(registry: DynamicPropertyRegistry) {
            registry.add("USER_URL") { "http://localhost:${echoServer.address.port}" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            echoServer.stop(0)
        }

        private fun token(subject: String): String =
            Jwts
                .builder()
                .subject(subject)
                .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()))
                .compact()
    }
}
