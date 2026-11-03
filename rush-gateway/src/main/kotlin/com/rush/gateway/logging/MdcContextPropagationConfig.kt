package com.rush.gateway.logging

import io.micrometer.context.ContextRegistry
import org.slf4j.MDC
import org.springframework.context.annotation.Configuration

/**
 * Reactor context에 넣은 요청 추적 값을 로그 MDC로 옮기도록 등록합니다.
 * spring.reactor.context-propagation=auto와 함께 쓰여, 스레드가 바뀌어도 MDC가 이어집니다.
 */
@Configuration(proxyBeanMethods = false)
class MdcContextPropagationConfig {
    init {
        RequestContext.KEYS.forEach { key ->
            ContextRegistry.getInstance().registerThreadLocalAccessor(
                key,
                { MDC.get(key) },
                { value: String -> MDC.put(key, value) },
                { MDC.remove(key) },
            )
        }
    }
}
