package com.rush.shop.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/shop")
class HealthController {
    @GetMapping("/ping")
    fun ping() = mapOf("service" to "rush-shop", "status" to "ok")
}
