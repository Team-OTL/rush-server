package com.rush.gateway.security

import com.rush.gateway.config.GatewayProperties
import com.rush.gateway.error.ErrorResponseWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

/**
 * Spring Security는 경로 접근 규칙, CORS, 보안 응답 헤더만 맡습니다.
 * JWT 검증은 라우트에 걸리는 JwtAuthenticationFilter가 그대로 합니다.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val properties: GatewayProperties,
    private val clientIpResolver: ClientIpResolver,
    private val errorResponseWriter: ErrorResponseWriter,
) {
    private val adminAllowedIps = IpRangeMatcher(properties.adminAllowedIps)

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange {
                it
                    .pathMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .pathMatchers("/actuator/**")
                    .access { _, context ->
                        val ip = clientIpResolver.resolve(context.exchange.request)
                        Mono.just(AuthorizationDecision(adminAllowedIps.matches(ip)))
                    }.anyExchange()
                    .permitAll()
            }.exceptionHandling {
                it
                    .authenticationEntryPoint(forbiddenEntryPoint())
                    .accessDeniedHandler(forbiddenHandler())
            }.build()

    // 로그인 개념이 없으므로 인증 실패도 접근 거부(403)로 응답합니다.
    private fun forbiddenEntryPoint() =
        ServerAuthenticationEntryPoint { exchange, _ ->
            errorResponseWriter.write(exchange, HttpStatus.FORBIDDEN)
        }

    private fun forbiddenHandler() =
        ServerAccessDeniedHandler { exchange, _ ->
            errorResponseWriter.write(exchange, HttpStatus.FORBIDDEN)
        }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        if (properties.corsAllowedOrigins.isEmpty()) return source

        val config =
            CorsConfiguration().apply {
                allowedOrigins = properties.corsAllowedOrigins
                allowedMethods =
                    listOf(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .map { it.name() }
                allowedHeaders = listOf("*")
                exposedHeaders = listOf(HttpHeaders.LOCATION, "X-Request-Id")
                allowCredentials = true
                maxAge = 3600
            }
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
