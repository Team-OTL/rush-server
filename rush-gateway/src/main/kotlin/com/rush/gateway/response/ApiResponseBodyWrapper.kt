package com.rush.gateway.response

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * 서비스가 돌려준 JSON body를 공통 형식으로 바꿉니다.
 */
@Component
class ApiResponseBodyWrapper(
    private val objectMapper: ObjectMapper,
) {
    // 소수를 double로 읽으면 금액 같은 값의 자릿수가 바뀌므로 BigDecimal로 읽고, 끝자리 0도 원문대로 둡니다.
    private val reader =
        objectMapper
            .reader()
            .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .without(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)

    /** 감싼 body를 돌려줍니다. 이미 공통 형식이거나 JSON으로 읽을 수 없으면 null입니다. */
    fun wrap(
        status: HttpStatus,
        body: ByteArray,
    ): ByteArray? {
        val node = if (body.isEmpty()) null else runCatching { reader.readTree(body) }.getOrElse { return null }
        if (node != null && isAlreadyWrapped(node)) return null

        val wrapped =
            if (status.isError) {
                CommonApiResponse.error(
                    status = status.value(),
                    code = node.textField("code") ?: status.name,
                    message = node.textField("message") ?: status.reasonPhrase,
                )
            } else {
                CommonApiResponse.success(status, node)
            }
        return objectMapper.writeValueAsBytes(wrapped)
    }

    private fun isAlreadyWrapped(node: JsonNode) = node.isObject && WRAPPED_KEYS.all(node::has)

    private fun JsonNode?.textField(name: String): String? = this?.get(name)?.takeIf { it.isTextual }?.asText()

    companion object {
        private val WRAPPED_KEYS = listOf("status", "code", "message")
    }
}
