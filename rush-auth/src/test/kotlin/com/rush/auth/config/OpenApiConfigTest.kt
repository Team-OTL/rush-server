package com.rush.auth.config

import io.swagger.v3.oas.models.security.SecurityScheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenApiConfigTest {
    @Test
    fun `Bearer 인증을 정의하고 전체 API에 적용한다`() {
        val openApi = OpenApiConfig().openApi()

        val scheme = openApi.components.securitySchemes.getValue(OpenApiConfig.BEARER)
        assertEquals(SecurityScheme.Type.HTTP, scheme.type)
        assertEquals("bearer", scheme.scheme)
        assertTrue(openApi.security.any { it.containsKey(OpenApiConfig.BEARER) })
    }
}
