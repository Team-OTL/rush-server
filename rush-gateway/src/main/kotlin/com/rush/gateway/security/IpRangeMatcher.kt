package com.rush.gateway.security

import org.springframework.security.web.util.matcher.IpAddressMatcher
import java.net.InetAddress

/**
 * CIDR 목록 중 하나라도 IP에 맞는지 봅니다.
 * IP 리터럴이 아닌 값은 DNS 조회를 하지 않도록 매칭 전에 걸러 내고, 잘못된 IP는 맞지 않는 것으로 봅니다.
 */
class IpRangeMatcher(
    ranges: List<String>,
) {
    private val matchers = ranges.map { it.trim() }.filter { it.isNotEmpty() }.map(::IpAddressMatcher)

    fun matches(ip: String?): Boolean =
        ip != null &&
            isIpLiteral(ip) &&
            runCatching { matchers.any { it.matches(ip) } }.getOrDefault(false)

    companion object {
        private val IPV4 = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
        private val IPV6 = Regex("""^[0-9a-fA-F:.]+$""")

        fun isIpLiteral(value: String): Boolean = isIpv4(value) || isIpv6(value)

        private fun isIpv4(value: String): Boolean = IPV4.matches(value) && value.split('.').all { it.toInt() <= 255 }

        // ':'가 들어간 값은 InetAddress가 IPv6 리터럴로만 해석하므로 DNS 조회가 일어나지 않습니다.
        private fun isIpv6(value: String): Boolean =
            value.contains(':') && IPV6.matches(value) && runCatching { InetAddress.getByName(value) }.isSuccess
    }
}
