package com.rush.gateway.response

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils

/**
 * 응답 body를 최대 [maxBytes]까지 모읍니다. 넘는 순간 모은 버퍼를 한꺼번에 내보내고,
 * 이후 버퍼는 모으지 않고 그대로 통과시킵니다. 요청 하나에서만 쓰는 상태 객체입니다.
 */
class BoundedBodyCollector(
    private val maxBytes: Long,
) {
    private val collected = mutableListOf<DataBuffer>()
    private var size = 0L

    /** 크기를 넘어 원문을 그대로 흘려보내는 중인지 여부입니다. */
    var relaying = false
        private set

    /** 버퍼 하나를 받아, 지금 내보낼 버퍼 목록을 돌려줍니다. 모으는 중이면 빈 목록입니다. */
    fun accept(buffer: DataBuffer): List<DataBuffer> {
        if (relaying) return listOf(buffer)

        collected += buffer
        size += buffer.readableByteCount()
        if (size <= maxBytes) return emptyList()

        relaying = true
        return drain()
    }

    /** 모은 버퍼를 넘겨주고 비웁니다. 넘겨받은 쪽이 해제합니다. */
    fun drain(): List<DataBuffer> = collected.toList().also { collected.clear() }

    /** 취소나 오류로 끝났을 때 아직 들고 있는 버퍼를 해제합니다. */
    fun release() {
        drain().forEach(DataBufferUtils::release)
    }
}
