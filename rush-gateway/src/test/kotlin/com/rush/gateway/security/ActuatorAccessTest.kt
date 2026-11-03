package com.rush.gateway.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

// @AutoConfigureWebTestClient는 mock 바인딩이라 remoteAddress가 없으므로, 실제 서버(RANDOM_PORT)로 요청합니다.
private const val SECRET = "test-secret-key-for-gateway-actuator-test-0123456789"

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
        "gateway.admin-allowed-ips=10.0.0.0/8",
    ],
)
class ActuatorDeniedIpTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `health는 누구나 볼 수 있다`() {
        client
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `gateway 엔드포인트는 403 공통 오류 응답을 준다`() {
        client
            .get()
            .uri("/actuator/gateway/routes")
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody()
            .jsonPath("$.code")
            .isEqualTo("FORBIDDEN")
            .jsonPath("$.message")
            .isEqualTo("접근 권한이 없습니다.")
    }

    @Test
    fun `X-Forwarded-For를 위조해도 허용되지 않는다`() {
        client
            .get()
            .uri("/actuator/circuitbreakers")
            .header("X-Forwarded-For", "10.0.0.1")
            .exchange()
            .expectStatus()
            .isForbidden
    }
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=$SECRET",
    ],
)
class ActuatorAllowedIpTest {
    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `기본 허용 대역인 localhost는 gateway 엔드포인트를 볼 수 있다`() {
        client
            .get()
            .uri("/actuator/gateway/routes")
            .exchange()
            .expectStatus()
            .isOk
    }
}
