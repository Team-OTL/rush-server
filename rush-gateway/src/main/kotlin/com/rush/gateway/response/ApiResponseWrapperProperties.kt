package com.rush.gateway.response

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize

/**
 * 서비스 응답 래핑 설정입니다.
 */
@ConfigurationProperties("gateway.response-wrapper")
data class ApiResponseWrapperProperties(
    // 이보다 큰 응답은 감싸지 않고 원문을 그대로 보냅니다.
    val maxSize: DataSize = DataSize.ofMegabytes(1),
    // 기본 제외 경로에 더해 감싸지 않을 경로 패턴
    val excludedPaths: List<String> = emptyList(),
)
