package com.rush.gateway.response

import com.sun.net.httpserver.HttpServer
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress

private const val SECRET = "test-secret-key-for-gateway-wrapper-test-0123456789"
private const val MAX_BYTES = 1024

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
        "gateway.response-wrapper.max-size=${MAX_BYTES}B",
    ],
)
@AutoConfigureWebTestClient
class ApiResponseWrapperTest {
    @Autowired
    lateinit var client: WebTestClient

    private fun get(path: String) =
        client
            .get()
            .uri("/user$path")
            .header("Authorization", "Bearer ${token()}")
            .exchange()

    @Test
    fun `성공 JSON은 data로 감싼다`() {
        val result =
            get("/item")
                .expectStatus()
                .isOk
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(200)
                .jsonPath("$.code")
                .isEqualTo("OK")
                .jsonPath("$.message")
                .isEqualTo(CommonApiResponse.SUCCESS_MESSAGE)
                .jsonPath("$.data.id")
                .isEqualTo(1)
                .jsonPath("$.data.name")
                .isEqualTo("사과")
                .returnResult()

        // 감싼 뒤에도 Content-Length가 실제 길이와 같아야 합니다.
        assertThat(result.responseHeaders.contentLength).isEqualTo(result.responseBody!!.size.toLong())
        assertThat(result.responseHeaders.eTag).isNull()
    }

    @Test
    fun `배열 응답도 data로 감싼다`() {
        get("/list")
            .expectBody()
            .jsonPath("$.data[0]")
            .isEqualTo(1)
            .jsonPath("$.data.length()")
            .isEqualTo(3)
    }

    @Test
    fun `오류 JSON은 상태와 서비스의 메시지로 감싼다`() {
        get("/missing")
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo(404)
            .jsonPath("$.code")
            .isEqualTo("ITEM_NOT_FOUND")
            .jsonPath("$.message")
            .isEqualTo("상품이 없습니다.")
            .jsonPath("$.data")
            .doesNotExist()
    }

    @Test
    fun `소수는 원문 자릿수 그대로 감싼다`() {
        val body = get("/decimal").expectBody(String::class.java).returnResult().responseBody!!

        assertThat(body).contains(""""data":{"amount":1234567890.123456789,"price":1.50}""")
    }

    @Test
    fun `부분 응답은 Content-Range와 함께 그대로 둔다`() {
        get("/partial")
            .expectStatus()
            .isEqualTo(206)
            .expectHeader()
            .valueEquals("Content-Range", "bytes 0-6/20")
            .expectBody(String::class.java)
            .isEqualTo("""{"a":1}""")
    }

    @Test
    fun `이미 공통 형식인 응답은 그대로 둔다`() {
        get("/wrapped")
            .expectBody()
            .json("""{"status":201,"code":"CREATED","message":"생성됨","data":{"id":9}}""", true)
    }

    @Test
    fun `JSON이 아닌 응답은 그대로 둔다`() {
        get("/text")
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String::class.java)
            .isEqualTo("plain")
    }

    @Test
    fun `최대 크기를 넘는 chunked 응답은 원문 그대로 보낸다`() {
        val body = get("/large").expectBody(String::class.java).returnResult().responseBody!!

        assertThat(body).isEqualTo(largeJson)
    }

    companion object {
        private val largeJson = """{"items":"${"a".repeat(MAX_BYTES * 2)}"}"""

        private val server =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                createContext("/") { exchange ->
                    val (status, type, body) =
                        when (exchange.requestURI.path.removePrefix("/user")) {
                            "/item" -> Triple(200, "application/json", """{"id":1,"name":"사과"}""")
                            "/list" -> Triple(200, "application/json", "[1,2,3]")
                            "/missing" -> Triple(404, "application/json", """{"code":"ITEM_NOT_FOUND","message":"상품이 없습니다."}""")
                            "/wrapped" ->
                                Triple(
                                    201,
                                    "application/json",
                                    """{"status":201,"code":"CREATED","message":"생성됨","data":{"id":9}}""",
                                )
                            "/text" -> Triple(200, "text/plain", "plain")
                            "/decimal" -> Triple(200, "application/json", """{"amount":1234567890.123456789,"price":1.50}""")
                            "/partial" -> Triple(206, "application/json", """{"a":1}""")
                            "/large" -> Triple(200, "application/json", largeJson)
                            else -> Triple(404, "text/plain", "")
                        }
                    val bytes = body.toByteArray()
                    exchange.responseHeaders.set("Content-Type", type)
                    exchange.responseHeaders.set("ETag", "\"v1\"")
                    if (exchange.requestURI.path.endsWith("/partial")) exchange.responseHeaders.set("Content-Range", "bytes 0-6/20")
                    // /large는 길이를 모르는 chunked 응답으로 보냅니다.
                    val length = if (exchange.requestURI.path.endsWith("/large")) 0L else bytes.size.toLong()
                    exchange.sendResponseHeaders(status, length)
                    exchange.responseBody.use { it.write(bytes) }
                }
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun routes(registry: DynamicPropertyRegistry) {
            registry.add("USER_URL") { "http://localhost:${server.address.port}" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop(0)
        }

        private fun token(): String =
            Jwts
                .builder()
                .subject("1")
                .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray()))
                .compact()
    }
}
