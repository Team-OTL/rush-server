package com.rush.minigame.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/minigame")
class HealthController {
    @GetMapping("/ping")
    fun ping() = mapOf("service" to "rush-minigame", "status" to "ok")
}
