package com.rush.gateway.error

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=test-secret-key-for-gateway-error-test-0123456789",
    ],
)
@AutoConfigureWebTestClient
class GatewayErrorTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `토큰이 없으면 401 공통 응답을 준다`() {
        client
            .get()
            .uri("/shop/items")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .exists("X-Request-Id")
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("UNAUTHORIZED")
    }

    @Test
    fun `없는 경로는 404 공통 응답을 준다`() {
        client
            .get()
            .uri("/unknown")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("NOT_FOUND")
            .jsonPath("$.message")
            .isEqualTo("요청한 경로를 찾을 수 없습니다.")
    }
}
