package com.rush.stage.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stage")
class HealthController {
    @GetMapping("/ping")
    fun ping() = mapOf("service" to "rush-stage", "status" to "ok")
}
