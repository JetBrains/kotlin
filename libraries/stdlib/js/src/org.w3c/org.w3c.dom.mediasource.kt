/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress(
    "NO_EXPLICIT_VISIBILITY_IN_API_MODE",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE",
    "DEPRECATION"
) // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.dom.mediasource

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.internal.InlineOnly

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class MediaSource : EventTarget, MediaProvider {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val sourceBuffers: SourceBufferList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val activeSourceBuffers: SourceBufferList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val readyState: ReadyState

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var duration: Double

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var onsourceopen: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var onsourceended: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var onsourceclose: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun addSourceBuffer(type: String): SourceBuffer

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun removeSourceBuffer(sourceBuffer: SourceBuffer)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun endOfStream(error: EndOfStreamError = definedExternally)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setLiveSeekableRange(start: Double, end: Double)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun clearLiveSeekableRange()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        fun isTypeSupported(type: String): Boolean
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SourceBuffer : EventTarget {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var mode: AppendMode

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val updating: Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val buffered: TimeRanges

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var timestampOffset: Double

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val audioTracks: AudioTrackList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val videoTracks: VideoTrackList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val textTracks: TextTrackList

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var appendWindowStart: Double

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var appendWindowEnd: Double

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onupdatestart: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onupdate: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onupdateend: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onerror: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onabort: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun appendBuffer(data: dynamic)

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun abort()

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun remove(start: Double, end: Double)
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class SourceBufferList : EventTarget {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val length: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onaddsourcebuffer: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onremovesourcebuffer: ((Event) -> dynamic)?
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline operator fun SourceBufferList.get(index: Int): SourceBuffer? = asDynamic()[index]

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface ReadyState {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val ReadyState.Companion.CLOSED: ReadyState get() = "closed".asDynamic().unsafeCast<ReadyState>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val ReadyState.Companion.OPEN: ReadyState get() = "open".asDynamic().unsafeCast<ReadyState>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val ReadyState.Companion.ENDED: ReadyState get() = "ended".asDynamic().unsafeCast<ReadyState>()

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface EndOfStreamError {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val EndOfStreamError.Companion.NETWORK: EndOfStreamError get() = "network".asDynamic().unsafeCast<EndOfStreamError>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val EndOfStreamError.Companion.DECODE: EndOfStreamError get() = "decode".asDynamic().unsafeCast<EndOfStreamError>()

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface AppendMode {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val AppendMode.Companion.SEGMENTS: AppendMode get() = "segments".asDynamic().unsafeCast<AppendMode>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val AppendMode.Companion.SEQUENCE: AppendMode get() = "sequence".asDynamic().unsafeCast<AppendMode>()
