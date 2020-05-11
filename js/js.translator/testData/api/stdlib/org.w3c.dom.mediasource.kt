package org.w3c.dom.mediasource

public val org.w3c.dom.mediasource.ReadyState.Companion.CLOSED: org.w3c.dom.mediasource.ReadyState
    public inline fun org.w3c.dom.mediasource.ReadyState.Companion.<get-CLOSED>(): org.w3c.dom.mediasource.ReadyState
public val org.w3c.dom.mediasource.EndOfStreamError.Companion.DECODE: org.w3c.dom.mediasource.EndOfStreamError
    public inline fun org.w3c.dom.mediasource.EndOfStreamError.Companion.<get-DECODE>(): org.w3c.dom.mediasource.EndOfStreamError
public val org.w3c.dom.mediasource.ReadyState.Companion.ENDED: org.w3c.dom.mediasource.ReadyState
    public inline fun org.w3c.dom.mediasource.ReadyState.Companion.<get-ENDED>(): org.w3c.dom.mediasource.ReadyState
public val org.w3c.dom.mediasource.EndOfStreamError.Companion.NETWORK: org.w3c.dom.mediasource.EndOfStreamError
    public inline fun org.w3c.dom.mediasource.EndOfStreamError.Companion.<get-NETWORK>(): org.w3c.dom.mediasource.EndOfStreamError
public val org.w3c.dom.mediasource.ReadyState.Companion.OPEN: org.w3c.dom.mediasource.ReadyState
    public inline fun org.w3c.dom.mediasource.ReadyState.Companion.<get-OPEN>(): org.w3c.dom.mediasource.ReadyState
public val org.w3c.dom.mediasource.AppendMode.Companion.SEGMENTS: org.w3c.dom.mediasource.AppendMode
    public inline fun org.w3c.dom.mediasource.AppendMode.Companion.<get-SEGMENTS>(): org.w3c.dom.mediasource.AppendMode
public val org.w3c.dom.mediasource.AppendMode.Companion.SEQUENCE: org.w3c.dom.mediasource.AppendMode
    public inline fun org.w3c.dom.mediasource.AppendMode.Companion.<get-SEQUENCE>(): org.w3c.dom.mediasource.AppendMode
@kotlin.internal.InlineOnly public inline operator fun org.w3c.dom.mediasource.SourceBufferList.get(/*0*/ index: kotlin.Int): org.w3c.dom.mediasource.SourceBuffer?

public external interface AppendMode {

    public companion object Companion {
    }
}

public external interface EndOfStreamError {

    public companion object Companion {
    }
}

public open external class MediaSource : org.w3c.dom.events.EventTarget, org.w3c.dom.MediaProvider {
    /*primary*/ public constructor MediaSource()
    public open val activeSourceBuffers: org.w3c.dom.mediasource.SourceBufferList
        public open fun <get-activeSourceBuffers>(): org.w3c.dom.mediasource.SourceBufferList
    public final var duration: kotlin.Double
        public final fun <get-duration>(): kotlin.Double
        public final fun <set-duration>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public final var onsourceclose: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onsourceclose>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onsourceclose>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onsourceended: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onsourceended>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onsourceended>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onsourceopen: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onsourceopen>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onsourceopen>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val readyState: org.w3c.dom.mediasource.ReadyState
        public open fun <get-readyState>(): org.w3c.dom.mediasource.ReadyState
    public open val sourceBuffers: org.w3c.dom.mediasource.SourceBufferList
        public open fun <get-sourceBuffers>(): org.w3c.dom.mediasource.SourceBufferList
    public final fun addSourceBuffer(/*0*/ type: kotlin.String): org.w3c.dom.mediasource.SourceBuffer
    public final fun clearLiveSeekableRange(): kotlin.Unit
    public final fun endOfStream(/*0*/ error: org.w3c.dom.mediasource.EndOfStreamError = ...): kotlin.Unit
    public final fun removeSourceBuffer(/*0*/ sourceBuffer: org.w3c.dom.mediasource.SourceBuffer): kotlin.Unit
    public final fun setLiveSeekableRange(/*0*/ start: kotlin.Double, /*1*/ end: kotlin.Double): kotlin.Unit

    public companion object Companion {
        public final fun isTypeSupported(/*0*/ type: kotlin.String): kotlin.Boolean
    }
}

public external interface ReadyState {

    public companion object Companion {
    }
}

public abstract external class SourceBuffer : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor SourceBuffer()
    public open var appendWindowEnd: kotlin.Double
        public open fun <get-appendWindowEnd>(): kotlin.Double
        public open fun <set-appendWindowEnd>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open var appendWindowStart: kotlin.Double
        public open fun <get-appendWindowStart>(): kotlin.Double
        public open fun <set-appendWindowStart>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val audioTracks: org.w3c.dom.AudioTrackList
        public open fun <get-audioTracks>(): org.w3c.dom.AudioTrackList
    public open val buffered: org.w3c.dom.TimeRanges
        public open fun <get-buffered>(): org.w3c.dom.TimeRanges
    public open var mode: org.w3c.dom.mediasource.AppendMode
        public open fun <get-mode>(): org.w3c.dom.mediasource.AppendMode
        public open fun <set-mode>(/*0*/ <set-?>: org.w3c.dom.mediasource.AppendMode): kotlin.Unit
    public open var onabort: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onabort>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onabort>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onupdate: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onupdate>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onupdate>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onupdateend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onupdateend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onupdateend>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onupdatestart: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onupdatestart>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onupdatestart>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val textTracks: org.w3c.dom.TextTrackList
        public open fun <get-textTracks>(): org.w3c.dom.TextTrackList
    public open var timestampOffset: kotlin.Double
        public open fun <get-timestampOffset>(): kotlin.Double
        public open fun <set-timestampOffset>(/*0*/ <set-?>: kotlin.Double): kotlin.Unit
    public open val updating: kotlin.Boolean
        public open fun <get-updating>(): kotlin.Boolean
    public open val videoTracks: org.w3c.dom.VideoTrackList
        public open fun <get-videoTracks>(): org.w3c.dom.VideoTrackList
    public final fun abort(): kotlin.Unit
    public final fun appendBuffer(/*0*/ data: dynamic): kotlin.Unit
    public final fun remove(/*0*/ start: kotlin.Double, /*1*/ end: kotlin.Double): kotlin.Unit
}

public abstract external class SourceBufferList : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor SourceBufferList()
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public open var onaddsourcebuffer: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onaddsourcebuffer>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onaddsourcebuffer>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onremovesourcebuffer: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onremovesourcebuffer>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onremovesourcebuffer>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
}