package com.rush.gateway.fallback

import com.sun.net.httpserver.HttpServer
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.Executors

private const val SECRET = "test-secret-key-for-gateway-fallback-test-0123456789"

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
        "resilience4j.timelimiter.configs.default.timeout-duration=3s",
    ],
)
@AutoConfigureWebTestClient(timeout = "10s")
class FallbackTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `서비스에 연결할 수 없으면 fallback 응답을 준다`() {
        client
            .get()
            .uri("/sp/ping")
            .header("Authorization", "Bearer ${token()}")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("SERVICE_UNAVAILABLE")
    }

    @Test
    fun `Eureka에 인스턴스가 없으면 fallback 응답을 준다`() {
        client
            .get()
            .uri("/auth/ping")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("SERVICE_UNAVAILABLE")
    }

    @Test
    fun `타임아웃을 넘기면 504 fallback 응답을 준다`() {
        client
            .get()
            .uri("/user/very-slow")
            .header("Authorization", "Bearer ${token()}")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.GATEWAY_TIMEOUT)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("GATEWAY_TIMEOUT")
    }

    @Test
    fun `타임아웃보다 빠른 느린 응답은 그대로 전달한다`() {
        client
            .get()
            .uri("/user/ping")
            .header("Authorization", "Bearer ${token()}")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(String::class.java)
            .isEqualTo("slow-ok")
    }

    companion object {
        private val slowServer =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                executor = Executors.newCachedThreadPool()
                createContext("/") { exchange ->
                    val delay = if (exchange.requestURI.path.endsWith("very-slow")) 4L else 2L
                    Thread.sleep(Duration.ofSeconds(delay))
                    val body = "slow-ok".toByteArray()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                start()
            }

        private val closedPort = ServerSocket(0).use { it.localPort }

        @JvmStatic
        @DynamicPropertySource
        fun routes(registry: DynamicPropertyRegistry) {
            registry.add("USER_URL") { "http://localhost:${slowServer.address.port}" }
            registry.add("SP_URL") { "http://localhost:$closedPort" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            slowServer.stop(0)
        }

        private fun token(): String =
            Jwts
                .builder()
                .subject("1")
                .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()))
                .compact()
    }
}
