/*∆*/ public val org.w3c.dom.mediasource.ReadyState.Companion.CLOSED: org.w3c.dom.mediasource.ReadyState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.EndOfStreamError.Companion.DECODE: org.w3c.dom.mediasource.EndOfStreamError { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.ReadyState.Companion.ENDED: org.w3c.dom.mediasource.ReadyState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.EndOfStreamError.Companion.NETWORK: org.w3c.dom.mediasource.EndOfStreamError { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.ReadyState.Companion.OPEN: org.w3c.dom.mediasource.ReadyState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.AppendMode.Companion.SEGMENTS: org.w3c.dom.mediasource.AppendMode { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediasource.AppendMode.Companion.SEQUENCE: org.w3c.dom.mediasource.AppendMode { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.dom.mediasource.SourceBufferList.get(index: kotlin.Int): org.w3c.dom.mediasource.SourceBuffer?
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface AppendMode {
/*∆*/     public companion object of AppendMode {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface EndOfStreamError {
/*∆*/     public companion object of EndOfStreamError {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class MediaSource : org.w3c.dom.events.EventTarget, org.w3c.dom.MediaProvider {
/*∆*/     public constructor MediaSource()
/*∆*/ 
/*∆*/     public open val activeSourceBuffers: org.w3c.dom.mediasource.SourceBufferList { get; }
/*∆*/ 
/*∆*/     public final var duration: kotlin.Double { get; set; }
/*∆*/ 
/*∆*/     public final var onsourceclose: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onsourceended: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onsourceopen: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val readyState: org.w3c.dom.mediasource.ReadyState { get; }
/*∆*/ 
/*∆*/     public open val sourceBuffers: org.w3c.dom.mediasource.SourceBufferList { get; }
/*∆*/ 
/*∆*/     public final fun addSourceBuffer(type: kotlin.String): org.w3c.dom.mediasource.SourceBuffer
/*∆*/ 
/*∆*/     public final fun clearLiveSeekableRange(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun endOfStream(error: org.w3c.dom.mediasource.EndOfStreamError = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun removeSourceBuffer(sourceBuffer: org.w3c.dom.mediasource.SourceBuffer): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setLiveSeekableRange(start: kotlin.Double, end: kotlin.Double): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of MediaSource {
/*∆*/         public final fun isTypeSupported(type: kotlin.String): kotlin.Boolean
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface ReadyState {
/*∆*/     public companion object of ReadyState {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SourceBuffer : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor SourceBuffer()
/*∆*/ 
/*∆*/     public open var appendWindowEnd: kotlin.Double { get; set; }
/*∆*/ 
/*∆*/     public open var appendWindowStart: kotlin.Double { get; set; }
/*∆*/ 
/*∆*/     public open val audioTracks: org.w3c.dom.AudioTrackList { get; }
/*∆*/ 
/*∆*/     public open val buffered: org.w3c.dom.TimeRanges { get; }
/*∆*/ 
/*∆*/     public open var mode: org.w3c.dom.mediasource.AppendMode { get; set; }
/*∆*/ 
/*∆*/     public open var onabort: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onerror: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onupdate: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onupdateend: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onupdatestart: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val textTracks: org.w3c.dom.TextTrackList { get; }
/*∆*/ 
/*∆*/     public open var timestampOffset: kotlin.Double { get; set; }
/*∆*/ 
/*∆*/     public open val updating: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val videoTracks: org.w3c.dom.VideoTrackList { get; }
/*∆*/ 
/*∆*/     public final fun abort(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun appendBuffer(data: dynamic): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun remove(start: kotlin.Double, end: kotlin.Double): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class SourceBufferList : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor SourceBufferList()
/*∆*/ 
/*∆*/     public open val length: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open var onaddsourcebuffer: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onremovesourcebuffer: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ }