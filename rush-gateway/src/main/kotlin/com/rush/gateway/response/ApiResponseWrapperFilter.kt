package com.rush.gateway.response

import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * 서비스 응답을 CommonApiResponse로 감쌉니다.
 *
 * NettyWriteResponseFilter 바로 앞에서 응답을 가로채, 쓰기 직전에 [ApiResponseWrappingPolicy]로 감쌀지 정합니다.
 * 감쌀 때는 body를 최대 크기까지 모았다가 바꾸고, 크기를 넘으면 원문을 그대로 흘려보냅니다.
 * SSE처럼 writeAndFlushWith로 나가는 스트리밍 응답은 감싸지 않습니다.
 */
@Component
class ApiResponseWrapperFilter(
    private val policy: ApiResponseWrappingPolicy,
    private val bodyWrapper: ApiResponseBodyWrapper,
    properties: ApiResponseWrapperProperties,
) : GlobalFilter,
    Ordered {
    private val maxBytes = properties.maxSize.toBytes()

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> = chain.filter(exchange.mutate().response(WrappingResponse(exchange)).build())

    private inner class WrappingResponse(
        private val exchange: ServerWebExchange,
    ) : ServerHttpResponseDecorator(exchange.response) {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            val request = exchange.request
            val decision = policy.decide(request.path.value(), request.method, request.headers, httpStatus(), headers)
            if (decision != ApiResponseWrappingPolicy.Decision.WRAP) return super.writeWith(body)

            val collector = BoundedBodyCollector(maxBytes)
            val relayed = Flux.from(body).concatMapIterable(collector::accept)
            val wrapped = Mono.defer { if (collector.relaying) Mono.empty() else Mono.justOrEmpty(wrappedBody(collector.drain())) }
            return super.writeWith(
                relayed
                    .concatWith(wrapped)
                    .doOnCancel(collector::release)
                    .doOnError { collector.release() },
            )
        }

        private fun httpStatus(): HttpStatus? = statusCode?.let { HttpStatus.resolve(it.value()) }

        // 모은 버퍼를 하나로 합쳐 감싸고, 감쌀 수 없으면 원문 그대로 돌려줍니다.
        private fun wrappedBody(buffers: List<DataBuffer>): DataBuffer? {
            if (buffers.isEmpty()) return null
            val original =
                bufferFactory().join(buffers).let { joined ->
                    ByteArray(joined.readableByteCount()).also {
                        joined.read(it)
                        DataBufferUtils.release(joined)
                    }
                }
            val status = httpStatus() ?: HttpStatus.OK
            val bytes = bodyWrapper.wrap(status, original) ?: return bufferFactory().wrap(original)

            headers.remove(HttpHeaders.TRANSFER_ENCODING)
            HEADERS_INVALIDATED_BY_WRAPPING.forEach(headers::remove)
            headers.contentType = MediaType.APPLICATION_JSON
            headers.contentLength = bytes.size.toLong()
            return bufferFactory().wrap(bytes)
        }
    }

    override fun getOrder() = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1

    companion object {
        // body가 바뀌면 의미가 없어지는 헤더
        private val HEADERS_INVALIDATED_BY_WRAPPING = listOf(HttpHeaders.ETAG, "Content-MD5", "Digest")
    }
}
