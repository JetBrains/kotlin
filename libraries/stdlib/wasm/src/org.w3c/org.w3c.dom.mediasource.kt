/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.mediasource

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Exposes the JavaScript [MediaSource](https://developer.mozilla.org/en/docs/Web/API/MediaSource) to Kotlin
 */
public external open class MediaSource : EventTarget, MediaProvider, JsAny {
    open val sourceBuffers: SourceBufferList
    open val activeSourceBuffers: SourceBufferList
    open val readyState: ReadyState
    var duration: Double
    var onsourceopen: ((Event) -> JsAny?)?
    var onsourceended: ((Event) -> JsAny?)?
    var onsourceclose: ((Event) -> JsAny?)?
    fun addSourceBuffer(type: String): SourceBuffer
    fun removeSourceBuffer(sourceBuffer: SourceBuffer)
    fun endOfStream(error: EndOfStreamError = definedExternally)
    fun setLiveSeekableRange(start: Double, end: Double)
    fun clearLiveSeekableRange()

    companion object {
        fun isTypeSupported(type: String): Boolean
    }
}

/**
 * Exposes the JavaScript [SourceBuffer](https://developer.mozilla.org/en/docs/Web/API/SourceBuffer) to Kotlin
 */
public external abstract class SourceBuffer : EventTarget, JsAny {
    open var mode: AppendMode
    open val updating: Boolean
    open val buffered: TimeRanges
    open var timestampOffset: Double
    open val audioTracks: AudioTrackList
    open val videoTracks: VideoTrackList
    open val textTracks: TextTrackList
    open var appendWindowStart: Double
    open var appendWindowEnd: Double
    open var onupdatestart: ((Event) -> JsAny?)?
    open var onupdate: ((Event) -> JsAny?)?
    open var onupdateend: ((Event) -> JsAny?)?
    open var onerror: ((Event) -> JsAny?)?
    open var onabort: ((Event) -> JsAny?)?
    fun appendBuffer(data: JsAny?)
    fun abort()
    fun remove(start: Double, end: Double)
}

/**
 * Exposes the JavaScript [SourceBufferList](https://developer.mozilla.org/en/docs/Web/API/SourceBufferList) to Kotlin
 */
public external abstract class SourceBufferList : EventTarget, JsAny {
    open val length: Int
    open var onaddsourcebuffer: ((Event) -> JsAny?)?
    open var onremovesourcebuffer: ((Event) -> JsAny?)?
}

@Suppress("UNUSED_PARAMETER")
internal fun getMethodImplForSourceBufferList(obj: SourceBufferList, index: Int): SourceBuffer? { js("return obj[index];") }

public operator fun SourceBufferList.get(index: Int): SourceBuffer? = getMethodImplForSourceBufferList(this, index)

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface ReadyState : JsAny {
    companion object
}

public inline val ReadyState.Companion.CLOSED: ReadyState get() = "closed".toJsString().unsafeCast<ReadyState>()

public inline val ReadyState.Companion.OPEN: ReadyState get() = "open".toJsString().unsafeCast<ReadyState>()

public inline val ReadyState.Companion.ENDED: ReadyState get() = "ended".toJsString().unsafeCast<ReadyState>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface EndOfStreamError : JsAny {
    companion object
}

public inline val EndOfStreamError.Companion.NETWORK: EndOfStreamError get() = "network".toJsString().unsafeCast<EndOfStreamError>()

public inline val EndOfStreamError.Companion.DECODE: EndOfStreamError get() = "decode".toJsString().unsafeCast<EndOfStreamError>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface AppendMode : JsAny {
    companion object
}

public inline val AppendMode.Companion.SEGMENTS: AppendMode get() = "segments".toJsString().unsafeCast<AppendMode>()

public inline val AppendMode.Companion.SEQUENCE: AppendMode get() = "sequence".toJsString().unsafeCast<AppendMode>()