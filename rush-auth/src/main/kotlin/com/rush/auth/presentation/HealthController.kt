package com.rush.auth.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class HealthController {
    @GetMapping("/ping")
    fun ping() = mapOf("service" to "rush-auth", "status" to "ok")
}
