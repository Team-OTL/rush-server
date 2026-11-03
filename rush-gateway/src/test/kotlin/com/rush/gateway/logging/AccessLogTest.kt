package com.rush.gateway.logging

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress
import java.net.ServerSocket

private const val SECRET = "test-secret-key-for-gateway-access-log-test-0123456789"

// clientIp를 확인하려면 remoteAddress가 필요하므로 실제 서버(RANDOM_PORT)로 요청합니다.
@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
    ],
)
class AccessLogTest {
    @Autowired
    lateinit var client: WebTestClient

    private val objectMapper = ObjectMapper()

    @Test
    fun `요청마다 JSON 접근 로그를 한 줄 남긴다`(output: CapturedOutput) {
        val requestId = requestIdOf("/user/ping", token("42"))

        val accessLogs = jsonLines(output).filter { it.path("logger_name").asText().endsWith("AccessLogFilter") }
        val line = accessLogs.single { it.path("requestId").asText() == requestId }
        assertThat(line.path("userId").asText()).isEqualTo("42")
        assertThat(line.path("clientIp").asText()).isEqualTo("127.0.0.1")
        assertThat(line.path("routeId").asText()).isEqualTo("rush-user")
        assertThat(line.path("status").asInt()).isEqualTo(200)
        assertThat(line.path("method").asText()).isEqualTo("GET")
        assertThat(line.path("path").asText()).isEqualTo("/user/ping")
    }

    @Test
    fun `게이트웨이가 거절한 요청도 접근 로그에 남는다`(output: CapturedOutput) {
        val requestId = requestIdOf("/user/ping", token = null)

        val line = jsonLines(output).single { it.path("requestId").asText() == requestId && it.has("status") }
        assertThat(line.path("status").asInt()).isEqualTo(401)
    }

    @Test
    fun `라우트 처리 중 남긴 로그에도 MDC가 이어진다`(output: CapturedOutput) {
        val requestId = requestIdOf("/sp/ping", token("7"))

        val fallbackLog = jsonLines(output).single { it.path("logger_name").asText().endsWith("FallbackController") }
        assertThat(fallbackLog.path("requestId").asText()).isEqualTo(requestId)
        assertThat(fallbackLog.path("userId").asText()).isEqualTo("7")
    }

    private fun requestIdOf(
        uri: String,
        token: String?,
    ): String =
        client
            .get()
            .uri(uri)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .exchange()
            .returnResult(String::class.java)
            .responseHeaders
            .getFirst("X-Request-Id")!!

    private fun jsonLines(output: CapturedOutput): List<JsonNode> =
        output.out
            .lines()
            .filter { it.startsWith("{") }
            .map { objectMapper.readTree(it) }

    companion object {
        private val okServer =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                createContext("/") { exchange ->
                    exchange.sendResponseHeaders(200, -1)
                    exchange.close()
                }
                start()
            }

        private val closedPort = ServerSocket(0).use { it.localPort }

        @JvmStatic
        @DynamicPropertySource
        fun routes(registry: DynamicPropertyRegistry) {
            registry.add("USER_URL") { "http://localhost:${okServer.address.port}" }
            registry.add("SP_URL") { "http://localhost:$closedPort" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            okServer.stop(0)
        }

        private fun token(subject: String): String =
            Jwts
                .builder()
                .subject(subject)
                .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()))
                .compact()
    }
}
