package com.rush.gateway.response

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.util.pattern.PathPatternParser

/**
 * 서비스 응답을 공통 형식으로 감쌀지 정합니다. 응답 상태와 헤더가 정해진 뒤, body를 쓰기 직전에 판단합니다.
 */
@Component
class ApiResponseWrappingPolicy(
    properties: ApiResponseWrapperProperties,
) {
    private val maxBytes = properties.maxSize.toBytes()
    private val excludedPatterns = (DEFAULT_EXCLUDED_PATHS + properties.excludedPaths).map(PathPatternParser.defaultInstance::parse)

    fun decide(
        path: String,
        method: HttpMethod,
        requestHeaders: HttpHeaders,
        status: HttpStatus?,
        responseHeaders: HttpHeaders,
    ): Decision {
        val pathContainer = PathContainer.parsePath(path)
        return when {
            excludedPatterns.any { it.matches(pathContainer) } -> Decision.SKIP_EXCLUDED_PATH
            requestHeaders.upgrade.equals("websocket", ignoreCase = true) -> Decision.SKIP_WEBSOCKET
            method == HttpMethod.HEAD -> Decision.SKIP_NO_BODY
            status == HttpStatus.NO_CONTENT || status == HttpStatus.NOT_MODIFIED -> Decision.SKIP_NO_BODY
            // Content-Range는 원본 본문 기준이라 감싸면 범위가 맞지 않습니다.
            status == HttpStatus.PARTIAL_CONTENT || responseHeaders.containsKey(HttpHeaders.CONTENT_RANGE) -> Decision.SKIP_PARTIAL
            isEncoded(responseHeaders) -> Decision.SKIP_ENCODED
            !isJson(responseHeaders.contentType) -> Decision.SKIP_NOT_JSON
            responseHeaders.contentLength > maxBytes -> Decision.SKIP_TOO_LARGE
            else -> Decision.WRAP
        }
    }

    private fun isEncoded(headers: HttpHeaders): Boolean {
        val encoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING) ?: return false
        return !encoding.equals("identity", ignoreCase = true)
    }

    private fun isJson(contentType: MediaType?): Boolean =
        contentType != null &&
            (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) || contentType.subtype.endsWith("+json"))

    enum class Decision {
        WRAP,
        SKIP_EXCLUDED_PATH,
        SKIP_WEBSOCKET,
        SKIP_NO_BODY,
        SKIP_PARTIAL,
        SKIP_ENCODED,
        SKIP_NOT_JSON,
        SKIP_TOO_LARGE,
    }

    companion object {
        val DEFAULT_EXCLUDED_PATHS =
            listOf(
                "/actuator/**",
                "/fallback",
                "/v3/api-docs/**",
                "/*/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/webjars/**",
            )
    }
}
