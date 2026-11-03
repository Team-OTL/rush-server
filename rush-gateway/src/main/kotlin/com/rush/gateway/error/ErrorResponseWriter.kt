package com.rush.gateway.error

import com.fasterxml.jackson.databind.ObjectMapper
import com.rush.gateway.response.CommonApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class ErrorResponseWriter(
    private val objectMapper: ObjectMapper,
) {
    fun write(
        exchange: ServerWebExchange,
        status: HttpStatus,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = objectMapper.writeValueAsBytes(CommonApiResponse.error(status))
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)))
    }
}
