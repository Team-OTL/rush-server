package com.rush.gateway.swagger

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=test-secret-key-for-gateway-swagger-test-0123456789",
    ],
)
@AutoConfigureWebTestClient
class SwaggerAggregationTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `Swagger UI가 열린다`() {
        client
            .get()
            .uri("/swagger-ui.html")
            .exchange()
            .expectStatus()
            .is3xxRedirection
    }

    @Test
    fun `Swagger UI 설정에 6개 서비스 문서가 등록된다`() {
        client
            .get()
            .uri("/v3/api-docs/swagger-config")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.urls.length()")
            .isEqualTo(6)
            .jsonPath("$.urls[?(@.name == 'shop')].url")
            .isEqualTo("/shop/v3/api-docs")
    }

    @Test
    fun `서비스 문서는 토큰 없이 원문 그대로 가져온다`() {
        client
            .get()
            .uri("/user/v3/api-docs")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(API_DOCS, true)
    }

    companion object {
        private const val API_DOCS = """{"openapi":"3.0.0","info":{"title":"rush-user","version":"1.0"},"paths":{}}"""

        private val docsServer =
            HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
                createContext("/user/v3/api-docs") { exchange ->
                    val body = API_DOCS.toByteArray()
                    exchange.responseHeaders.set("Content-Type", "application/json")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                start()
            }

        @JvmStatic
        @DynamicPropertySource
        fun routes(registry: DynamicPropertyRegistry) {
            registry.add("USER_URL") { "http://localhost:${docsServer.address.port}" }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            docsServer.stop(0)
        }
    }
}
