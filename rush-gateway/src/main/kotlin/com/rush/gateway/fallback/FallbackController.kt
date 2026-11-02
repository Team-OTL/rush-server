package com.rush.gateway.fallback

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class FallbackController {
    @RequestMapping("/fallback")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun fallback() =
        mapOf(
            "code" to "SERVICE_UNAVAILABLE",
            "message" to "일시적으로 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.",
        )
}
