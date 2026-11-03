package com.rush.gateway.response

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * 서비스가 돌려준 JSON body를 공통 형식으로 바꿉니다.
 */
@Component
class ApiResponseBodyWrapper(
    private val objectMapper: ObjectMapper,
) {
    /** 감싼 body를 돌려줍니다. 이미 공통 형식이거나 JSON으로 읽을 수 없으면 null입니다. */
    fun wrap(
        status: HttpStatus,
        body: ByteArray,
    ): ByteArray? {
        val node = if (body.isEmpty()) null else runCatching { objectMapper.readTree(body) }.getOrElse { return null }
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
