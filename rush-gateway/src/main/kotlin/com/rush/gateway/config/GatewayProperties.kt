package com.rush.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 게이트웨이 전용 설정입니다. 값은 rush-config의 rush-gateway.yml에서 오고,
 * config 서버 없이 뜰 때를 위해 기본값을 둡니다.
 */
@ConfigurationProperties("gateway")
data class GatewayProperties(
    // actuator 관리용 엔드포인트(health 제외)에 접근할 수 있는 IP 대역(CIDR)
    val adminAllowedIps: List<String> = listOf("127.0.0.1/32", "::1/128"),
    // 이 대역에서 온 요청만 X-Forwarded-For를 믿습니다. 비어 있으면 연결 IP만 씁니다.
    val trustedProxies: List<String> = emptyList(),
    // CORS를 허용할 Origin. 비어 있으면 CORS 헤더를 붙이지 않습니다.
    val corsAllowedOrigins: List<String> = emptyList(),
)
