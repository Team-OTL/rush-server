package com.rush.gateway.response

import com.rush.gateway.response.ApiResponseWrappingPolicy.Decision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.unit.DataSize

class ApiResponseWrappingPolicyTest {
    private val policy = ApiResponseWrappingPolicy(ApiResponseWrapperProperties(DataSize.ofKilobytes(1), listOf("/shop/files/**")))

    private fun json(length: Long = -1) =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            if (length >= 0) contentLength = length
        }

    private fun decide(
        path: String = "/shop/items",
        method: HttpMethod = HttpMethod.GET,
        requestHeaders: HttpHeaders = HttpHeaders(),
        status: HttpStatus = HttpStatus.OK,
        responseHeaders: HttpHeaders = json(),
    ) = policy.decide(path, method, requestHeaders, status, responseHeaders)

    @Test
    fun `JSON 응답은 감싼다`() {
        assertEquals(Decision.WRAP, decide())
        assertEquals(
            Decision.WRAP,
            decide(
                responseHeaders =
                    HttpHeaders().apply {
                        contentType =
                            MediaType.parseMediaType("application/problem+json")
                    },
            ),
        )
    }

    @Test
    fun `제외 경로는 감싸지 않는다`() {
        assertEquals(Decision.SKIP_EXCLUDED_PATH, decide(path = "/actuator/health"))
        assertEquals(Decision.SKIP_EXCLUDED_PATH, decide(path = "/shop/v3/api-docs"))
        assertEquals(Decision.SKIP_EXCLUDED_PATH, decide(path = "/fallback"))
        assertEquals(Decision.SKIP_EXCLUDED_PATH, decide(path = "/shop/files/1"))
    }

    @Test
    fun `WebSocket 업그레이드는 감싸지 않는다`() {
        assertEquals(Decision.SKIP_WEBSOCKET, decide(requestHeaders = HttpHeaders().apply { upgrade = "websocket" }))
    }

    @Test
    fun `본문이 없는 응답은 감싸지 않는다`() {
        assertEquals(Decision.SKIP_NO_BODY, decide(method = HttpMethod.HEAD))
        assertEquals(Decision.SKIP_NO_BODY, decide(status = HttpStatus.NO_CONTENT))
        assertEquals(Decision.SKIP_NO_BODY, decide(status = HttpStatus.NOT_MODIFIED))
    }

    @Test
    fun `부분 응답은 감싸지 않는다`() {
        assertEquals(Decision.SKIP_PARTIAL, decide(status = HttpStatus.PARTIAL_CONTENT))
        assertEquals(Decision.SKIP_PARTIAL, decide(responseHeaders = json().apply { set(HttpHeaders.CONTENT_RANGE, "bytes 0-6/20") }))
    }

    @Test
    fun `압축된 응답은 감싸지 않는다`() {
        assertEquals(Decision.SKIP_ENCODED, decide(responseHeaders = json().apply { set(HttpHeaders.CONTENT_ENCODING, "gzip") }))
        assertEquals(Decision.WRAP, decide(responseHeaders = json().apply { set(HttpHeaders.CONTENT_ENCODING, "identity") }))
    }

    @Test
    fun `JSON이 아니면 감싸지 않는다`() {
        assertEquals(Decision.SKIP_NOT_JSON, decide(responseHeaders = HttpHeaders().apply { contentType = MediaType.TEXT_PLAIN }))
        assertEquals(Decision.SKIP_NOT_JSON, decide(responseHeaders = HttpHeaders()))
    }

    @Test
    fun `Content-Length가 최대 크기를 넘으면 감싸지 않는다`() {
        assertEquals(Decision.WRAP, decide(responseHeaders = json(1024)))
        assertEquals(Decision.SKIP_TOO_LARGE, decide(responseHeaders = json(1025)))
    }
}
