package com.rush.gateway.filter

import org.springframework.http.HttpHeaders

/**
 * 서비스로 넘기면 안 되는 형식의 요청을 골라냅니다.
 * 경로 조작(.., 인코딩된 /, \ 등)과 제어문자가 섞인 헤더를 거부합니다.
 */
object RequestValidator {
    // 디코딩 후 경로 구분자나 상위 경로로 바뀔 수 있는 인코딩, NUL
    private val forbiddenEncodings = listOf("%2e", "%2f", "%5c", "%00")

    fun isValidPath(rawPath: String): Boolean {
        val lower = rawPath.lowercase()
        if (forbiddenEncodings.any { it in lower }) return false
        if ("//" in rawPath || '\\' in rawPath) return false
        return rawPath.split('/').none { it == ".." || it == "." }
    }

    fun isValidHeaders(headers: HttpHeaders): Boolean =
        headers.all { (name, values) -> isValidHeaderValue(name) && values.all(::isValidHeaderValue) }

    // 탭을 제외한 제어문자(CR, LF, NUL 등)와 DEL을 거부합니다.
    fun isValidHeaderValue(value: String): Boolean = value.none { (it < ' ' && it != '\t') || it == '\u007f' }
}
