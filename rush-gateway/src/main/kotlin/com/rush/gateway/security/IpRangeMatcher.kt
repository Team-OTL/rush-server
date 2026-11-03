package com.rush.gateway.security

import org.springframework.security.web.util.matcher.IpAddressMatcher

/**
 * CIDR 목록 중 하나라도 IP에 맞는지 봅니다.
 * IP 리터럴이 아닌 값은 DNS 조회를 하지 않도록 매칭 전에 걸러 냅니다.
 */
class IpRangeMatcher(
    ranges: List<String>,
) {
    private val matchers = ranges.map { it.trim() }.filter { it.isNotEmpty() }.map(::IpAddressMatcher)

    fun matches(ip: String?): Boolean = ip != null && isIpLiteral(ip) && matchers.any { it.matches(ip) }

    companion object {
        private val IPV4 = Regex("""^(\d{1,3})(\.\d{1,3}){3}$""")
        private val IPV6 = Regex("""^[0-9a-fA-F:.]+$""")

        fun isIpLiteral(value: String): Boolean = IPV4.matches(value) || (value.contains(':') && IPV6.matches(value))
    }
}
