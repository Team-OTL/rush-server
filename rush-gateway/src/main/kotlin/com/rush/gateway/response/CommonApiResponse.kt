package com.rush.gateway.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus

/**
 * 클라이언트가 받는 공통 응답 형식입니다. 서비스 응답은 ApiResponseWrapperFilter가,
 * 게이트웨이가 직접 만드는 오류는 ErrorResponseWriter가 이 형식으로 씁니다. data가 없으면 필드를 생략합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommonApiResponse(
    val status: Int,
    val code: String,
    val message: String,
    val data: Any? = null,
) {
    companion object {
        const val SUCCESS_MESSAGE = "요청에 성공했습니다."

        fun success(
            status: HttpStatus,
            data: Any?,
        ) = CommonApiResponse(status.value(), status.name, SUCCESS_MESSAGE, data)

        fun error(status: HttpStatus) = error(status.value(), status.name, messageOf(status))

        fun error(
            status: Int,
            code: String,
            message: String,
        ) = CommonApiResponse(status, code, message)

        private fun messageOf(status: HttpStatus) =
            when (status) {
                HttpStatus.BAD_REQUEST -> "잘못된 요청입니다."
                HttpStatus.UNAUTHORIZED -> "인증이 필요합니다."
                HttpStatus.FORBIDDEN -> "접근 권한이 없습니다."
                HttpStatus.NOT_FOUND -> "요청한 경로를 찾을 수 없습니다."
                HttpStatus.BAD_GATEWAY -> "서비스에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                HttpStatus.SERVICE_UNAVAILABLE -> "일시적으로 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."
                HttpStatus.GATEWAY_TIMEOUT -> "서비스 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
                else -> if (status.is5xxServerError) "서버 오류가 발생했습니다." else status.reasonPhrase
            }
    }
}
