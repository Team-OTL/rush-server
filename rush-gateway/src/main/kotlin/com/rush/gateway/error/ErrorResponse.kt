package com.rush.gateway.error

import org.springframework.http.HttpStatus

data class ErrorResponse(
    val code: String,
    val message: String,
) {
    companion object {
        fun of(status: HttpStatus) = ErrorResponse(status.name, messageOf(status))

        private fun messageOf(status: HttpStatus) =
            when (status) {
                HttpStatus.UNAUTHORIZED -> "인증이 필요합니다."
                HttpStatus.NOT_FOUND -> "요청한 경로를 찾을 수 없습니다."
                HttpStatus.SERVICE_UNAVAILABLE -> "일시적으로 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
                HttpStatus.GATEWAY_TIMEOUT -> "서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
                else -> if (status.is5xxServerError) "서버 오류가 발생했습니다." else status.reasonPhrase
            }
    }
}
