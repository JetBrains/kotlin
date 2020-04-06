/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.mediasource

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.clipboard.*
import org.w3c.dom.css.*
import org.w3c.dom.encryptedmedia.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external open class MediaSource : EventTarget, MediaProvider {
    open val sourceBuffers: SourceBufferList
    open val activeSourceBuffers: SourceBufferList
    open val readyState: ReadyState
    var duration: Double
    var onsourceopen: ((Event) -> dynamic)?
    var onsourceended: ((Event) -> dynamic)?
    var onsourceclose: ((Event) -> dynamic)?
    fun addSourceBuffer(type: String): SourceBuffer
    fun removeSourceBuffer(sourceBuffer: SourceBuffer)
    fun endOfStream(error: EndOfStreamError = definedExternally)
    fun setLiveSeekableRange(start: Double, end: Double)
    fun clearLiveSeekableRange()

    companion object {
        fun isTypeSupported(type: String): Boolean
    }
}

public external abstract class SourceBuffer : EventTarget {
    open var mode: AppendMode
    open val updating: Boolean
    open val buffered: TimeRanges
    open var timestampOffset: Double
    open val audioTracks: AudioTrackList
    open val videoTracks: VideoTrackList
    open val textTracks: TextTrackList
    open var appendWindowStart: Double
    open var appendWindowEnd: Double
    open var onupdatestart: ((Event) -> dynamic)?
    open var onupdate: ((Event) -> dynamic)?
    open var onupdateend: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
    open var onabort: ((Event) -> dynamic)?
    fun appendBuffer(data: dynamic)
    fun abort()
    fun remove(start: Double, end: Double)
}

public external abstract class SourceBufferList : EventTarget {
    open val length: Int
    open var onaddsourcebuffer: ((Event) -> dynamic)?
    open var onremovesourcebuffer: ((Event) -> dynamic)?
}

@kotlin.internal.InlineOnly
public inline operator fun SourceBufferList.get(index: Int): SourceBuffer? = asDynamic()[index]

/* please, don't implement this interface! */
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface ReadyState {
    companion object
}

public inline val ReadyState.Companion.CLOSED: ReadyState get() = "closed".asDynamic().unsafeCast<ReadyState>()

public inline val ReadyState.Companion.OPEN: ReadyState get() = "open".asDynamic().unsafeCast<ReadyState>()

public inline val ReadyState.Companion.ENDED: ReadyState get() = "ended".asDynamic().unsafeCast<ReadyState>()

/* please, don't implement this interface! */
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface EndOfStreamError {
    companion object
}

public inline val EndOfStreamError.Companion.NETWORK: EndOfStreamError get() = "network".asDynamic().unsafeCast<EndOfStreamError>()

public inline val EndOfStreamError.Companion.DECODE: EndOfStreamError get() = "decode".asDynamic().unsafeCast<EndOfStreamError>()

/* please, don't implement this interface! */
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface AppendMode {
    companion object
}

public inline val AppendMode.Companion.SEGMENTS: AppendMode get() = "segments".asDynamic().unsafeCast<AppendMode>()

public inline val AppendMode.Companion.SEQUENCE: AppendMode get() = "sequence".asDynamic().unsafeCast<AppendMode>()