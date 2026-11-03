package com.rush.gateway.filter

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RequestValidatorTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "/shop/../admin", "/shop/%2e%2e/admin", "/shop/%2E%2E/admin", "/shop%2fadmin",
            "/shop/%5cadmin", "//shop", "/shop/./a", "/shop/a%00",
        ],
    )
    fun `경로 조작이 있으면 거부한다`(path: String) {
        assertFalse(RequestValidator.isValidPath(path))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/shop/ping", "/shop/items/1", "/user/v3/api-docs", "/shop/a.b/c..d"])
    fun `정상 경로는 통과한다`(path: String) {
        assertTrue(RequestValidator.isValidPath(path))
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\rb", "a\nb", "a\u0000b", "a\u007fb", "a\u0001b"])
    fun `제어문자가 든 헤더 값은 거부한다`(value: String) {
        assertFalse(RequestValidator.isValidHeaderValue(value))
    }

    @ParameterizedTest
    @ValueSource(strings = ["Bearer abc.def", "a\tb", "한글 값"])
    fun `정상 헤더 값은 통과한다`(value: String) {
        assertTrue(RequestValidator.isValidHeaderValue(value))
    }
}
