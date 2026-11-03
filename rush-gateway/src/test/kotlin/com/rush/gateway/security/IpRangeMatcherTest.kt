package com.rush.gateway.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IpRangeMatcherTest {
    private val matcher = IpRangeMatcher(listOf("10.0.0.0/8", "::1/128"))

    @Test
    fun `허용 범위에 속한 IP는 맞는다`() {
        assertTrue(matcher.matches("10.1.2.3"))
        assertTrue(matcher.matches("::1"))
    }

    @Test
    fun `범위를 벗어난 IPv4 옥텟은 예외 없이 맞지 않는 것으로 본다`() {
        assertFalse(matcher.matches("999.999.999.999"))
        assertFalse(matcher.matches("10.0.0.256"))
    }

    @Test
    fun `형식이 잘못된 IPv6는 예외 없이 맞지 않는 것으로 본다`() {
        assertFalse(matcher.matches(":::::"))
        assertFalse(matcher.matches("1:2:3:4:5:6:7:8:9"))
    }

    @Test
    fun `IP가 아닌 값은 맞지 않는다`() {
        assertFalse(matcher.matches("localhost"))
        assertFalse(matcher.matches(null))
    }
}
