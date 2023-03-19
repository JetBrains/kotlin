/*∆*/ public val org.w3c.dom.mediacapture.MediaDeviceKind.Companion.AUDIOINPUT: org.w3c.dom.mediacapture.MediaDeviceKind { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.MediaDeviceKind.Companion.AUDIOOUTPUT: org.w3c.dom.mediacapture.MediaDeviceKind { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoResizeModeEnum.Companion.CROP_AND_SCALE: org.w3c.dom.mediacapture.VideoResizeModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.MediaStreamTrackState.Companion.ENDED: org.w3c.dom.mediacapture.MediaStreamTrackState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoFacingModeEnum.Companion.ENVIRONMENT: org.w3c.dom.mediacapture.VideoFacingModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoFacingModeEnum.Companion.LEFT: org.w3c.dom.mediacapture.VideoFacingModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.MediaStreamTrackState.Companion.LIVE: org.w3c.dom.mediacapture.MediaStreamTrackState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoResizeModeEnum.Companion.NONE: org.w3c.dom.mediacapture.VideoResizeModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoFacingModeEnum.Companion.RIGHT: org.w3c.dom.mediacapture.VideoFacingModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.VideoFacingModeEnum.Companion.USER: org.w3c.dom.mediacapture.VideoFacingModeEnum { get; }
/*∆*/ 
/*∆*/ public val org.w3c.dom.mediacapture.MediaDeviceKind.Companion.VIDEOINPUT: org.w3c.dom.mediacapture.MediaDeviceKind { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun Capabilities(): org.w3c.dom.mediacapture.Capabilities
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ConstrainBooleanParameters(exact: kotlin.Boolean? = ..., ideal: kotlin.Boolean? = ...): org.w3c.dom.mediacapture.ConstrainBooleanParameters
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ConstrainDOMStringParameters(exact: dynamic = ..., ideal: dynamic = ...): org.w3c.dom.mediacapture.ConstrainDOMStringParameters
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ConstrainDoubleRange(exact: kotlin.Double? = ..., ideal: kotlin.Double? = ..., max: kotlin.Double? = ..., min: kotlin.Double? = ...): org.w3c.dom.mediacapture.ConstrainDoubleRange
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ConstrainULongRange(exact: kotlin.Int? = ..., ideal: kotlin.Int? = ..., max: kotlin.Int? = ..., min: kotlin.Int? = ...): org.w3c.dom.mediacapture.ConstrainULongRange
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ConstraintSet(): org.w3c.dom.mediacapture.ConstraintSet
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun Constraints(advanced: kotlin.Array<org.w3c.dom.mediacapture.ConstraintSet>? = ...): org.w3c.dom.mediacapture.Constraints
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun DoubleRange(max: kotlin.Double? = ..., min: kotlin.Double? = ...): org.w3c.dom.mediacapture.DoubleRange
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaStreamConstraints(video: dynamic = ..., audio: dynamic = ...): org.w3c.dom.mediacapture.MediaStreamConstraints
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaStreamTrackEventInit(track: org.w3c.dom.mediacapture.MediaStreamTrack?, bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.mediacapture.MediaStreamTrackEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaTrackCapabilities(width: org.w3c.dom.mediacapture.ULongRange? = ..., height: org.w3c.dom.mediacapture.ULongRange? = ..., aspectRatio: org.w3c.dom.mediacapture.DoubleRange? = ..., frameRate: org.w3c.dom.mediacapture.DoubleRange? = ..., facingMode: kotlin.Array<kotlin.String>? = ..., resizeMode: kotlin.Array<kotlin.String>? = ..., volume: org.w3c.dom.mediacapture.DoubleRange? = ..., sampleRate: org.w3c.dom.mediacapture.ULongRange? = ..., sampleSize: org.w3c.dom.mediacapture.ULongRange? = ..., echoCancellation: kotlin.Array<kotlin.Boolean>? = ..., autoGainControl: kotlin.Array<kotlin.Boolean>? = ..., noiseSuppression: kotlin.Array<kotlin.Boolean>? = ..., latency: org.w3c.dom.mediacapture.DoubleRange? = ..., channelCount: org.w3c.dom.mediacapture.ULongRange? = ..., deviceId: kotlin.String? = ..., groupId: kotlin.String? = ...): org.w3c.dom.mediacapture.MediaTrackCapabilities
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaTrackConstraintSet(width: dynamic = ..., height: dynamic = ..., aspectRatio: dynamic = ..., frameRate: dynamic = ..., facingMode: dynamic = ..., resizeMode: dynamic = ..., volume: dynamic = ..., sampleRate: dynamic = ..., sampleSize: dynamic = ..., echoCancellation: dynamic = ..., autoGainControl: dynamic = ..., noiseSuppression: dynamic = ..., latency: dynamic = ..., channelCount: dynamic = ..., deviceId: dynamic = ..., groupId: dynamic = ...): org.w3c.dom.mediacapture.MediaTrackConstraintSet
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaTrackConstraints(advanced: kotlin.Array<org.w3c.dom.mediacapture.MediaTrackConstraintSet>? = ..., width: dynamic = ..., height: dynamic = ..., aspectRatio: dynamic = ..., frameRate: dynamic = ..., facingMode: dynamic = ..., resizeMode: dynamic = ..., volume: dynamic = ..., sampleRate: dynamic = ..., sampleSize: dynamic = ..., echoCancellation: dynamic = ..., autoGainControl: dynamic = ..., noiseSuppression: dynamic = ..., latency: dynamic = ..., channelCount: dynamic = ..., deviceId: dynamic = ..., groupId: dynamic = ...): org.w3c.dom.mediacapture.MediaTrackConstraints
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaTrackSettings(width: kotlin.Int? = ..., height: kotlin.Int? = ..., aspectRatio: kotlin.Double? = ..., frameRate: kotlin.Double? = ..., facingMode: kotlin.String? = ..., resizeMode: kotlin.String? = ..., volume: kotlin.Double? = ..., sampleRate: kotlin.Int? = ..., sampleSize: kotlin.Int? = ..., echoCancellation: kotlin.Boolean? = ..., autoGainControl: kotlin.Boolean? = ..., noiseSuppression: kotlin.Boolean? = ..., latency: kotlin.Double? = ..., channelCount: kotlin.Int? = ..., deviceId: kotlin.String? = ..., groupId: kotlin.String? = ...): org.w3c.dom.mediacapture.MediaTrackSettings
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun MediaTrackSupportedConstraints(width: kotlin.Boolean? = ..., height: kotlin.Boolean? = ..., aspectRatio: kotlin.Boolean? = ..., frameRate: kotlin.Boolean? = ..., facingMode: kotlin.Boolean? = ..., resizeMode: kotlin.Boolean? = ..., volume: kotlin.Boolean? = ..., sampleRate: kotlin.Boolean? = ..., sampleSize: kotlin.Boolean? = ..., echoCancellation: kotlin.Boolean? = ..., autoGainControl: kotlin.Boolean? = ..., noiseSuppression: kotlin.Boolean? = ..., latency: kotlin.Boolean? = ..., channelCount: kotlin.Boolean? = ..., deviceId: kotlin.Boolean? = ..., groupId: kotlin.Boolean? = ...): org.w3c.dom.mediacapture.MediaTrackSupportedConstraints
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun OverconstrainedErrorEventInit(error: dynamic = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.dom.mediacapture.OverconstrainedErrorEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun Settings(): org.w3c.dom.mediacapture.Settings
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ULongRange(max: kotlin.Int? = ..., min: kotlin.Int? = ...): org.w3c.dom.mediacapture.ULongRange
/*∆*/ 
/*∆*/ public external interface Capabilities {
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstrainBooleanParameters {
/*∆*/     public open var exact: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var ideal: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstrainDOMStringParameters {
/*∆*/     public open var exact: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var ideal: dynamic { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstrainDoubleRange : org.w3c.dom.mediacapture.DoubleRange {
/*∆*/     public open var exact: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var ideal: kotlin.Double? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstrainULongRange : org.w3c.dom.mediacapture.ULongRange {
/*∆*/     public open var exact: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var ideal: kotlin.Int? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstrainablePattern {
/*∆*/     public open var onoverconstrained: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public abstract fun applyConstraints(constraints: org.w3c.dom.mediacapture.Constraints = ...): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public abstract fun getCapabilities(): org.w3c.dom.mediacapture.Capabilities
/*∆*/ 
/*∆*/     public abstract fun getConstraints(): org.w3c.dom.mediacapture.Constraints
/*∆*/ 
/*∆*/     public abstract fun getSettings(): org.w3c.dom.mediacapture.Settings
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ConstraintSet {
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface Constraints : org.w3c.dom.mediacapture.ConstraintSet {
/*∆*/     public open var advanced: kotlin.Array<org.w3c.dom.mediacapture.ConstraintSet>? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface DoubleRange {
/*∆*/     public open var max: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var min: kotlin.Double? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class InputDeviceInfo : org.w3c.dom.mediacapture.MediaDeviceInfo {
/*∆*/     public constructor InputDeviceInfo()
/*∆*/ 
/*∆*/     public final fun getCapabilities(): org.w3c.dom.mediacapture.MediaTrackCapabilities
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaDeviceInfo {
/*∆*/     public constructor MediaDeviceInfo()
/*∆*/ 
/*∆*/     public open val deviceId: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val groupId: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val kind: org.w3c.dom.mediacapture.MediaDeviceKind { get; }
/*∆*/ 
/*∆*/     public open val label: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun toJSON(): dynamic
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaDeviceKind {
/*∆*/     public companion object of MediaDeviceKind {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaDevices : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor MediaDevices()
/*∆*/ 
/*∆*/     public open var ondevicechange: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final fun enumerateDevices(): kotlin.js.Promise<kotlin.Array<org.w3c.dom.mediacapture.MediaDeviceInfo>>
/*∆*/ 
/*∆*/     public final fun getSupportedConstraints(): org.w3c.dom.mediacapture.MediaTrackSupportedConstraints
/*∆*/ 
/*∆*/     public final fun getUserMedia(constraints: org.w3c.dom.mediacapture.MediaStreamConstraints = ...): kotlin.js.Promise<org.w3c.dom.mediacapture.MediaStream>
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class MediaStream : org.w3c.dom.events.EventTarget, org.w3c.dom.MediaProvider {
/*∆*/     public constructor MediaStream()
/*∆*/ 
/*∆*/     public constructor MediaStream(tracks: kotlin.Array<org.w3c.dom.mediacapture.MediaStreamTrack>)
/*∆*/ 
/*∆*/     public constructor MediaStream(stream: org.w3c.dom.mediacapture.MediaStream)
/*∆*/ 
/*∆*/     public open val active: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val id: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final var onaddtrack: ((org.w3c.dom.mediacapture.MediaStreamTrackEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onremovetrack: ((org.w3c.dom.mediacapture.MediaStreamTrackEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final fun addTrack(track: org.w3c.dom.mediacapture.MediaStreamTrack): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun clone(): org.w3c.dom.mediacapture.MediaStream
/*∆*/ 
/*∆*/     public final fun getAudioTracks(): kotlin.Array<org.w3c.dom.mediacapture.MediaStreamTrack>
/*∆*/ 
/*∆*/     public final fun getTrackById(trackId: kotlin.String): org.w3c.dom.mediacapture.MediaStreamTrack?
/*∆*/ 
/*∆*/     public final fun getTracks(): kotlin.Array<org.w3c.dom.mediacapture.MediaStreamTrack>
/*∆*/ 
/*∆*/     public final fun getVideoTracks(): kotlin.Array<org.w3c.dom.mediacapture.MediaStreamTrack>
/*∆*/ 
/*∆*/     public final fun removeTrack(track: org.w3c.dom.mediacapture.MediaStreamTrack): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaStreamConstraints {
/*∆*/     public open var audio: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var video: dynamic { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class MediaStreamTrack : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor MediaStreamTrack()
/*∆*/ 
/*∆*/     public open var enabled: kotlin.Boolean { get; set; }
/*∆*/ 
/*∆*/     public open val id: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val kind: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val label: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val muted: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open var onended: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onmute: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onoverconstrained: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onunmute: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val readyState: org.w3c.dom.mediacapture.MediaStreamTrackState { get; }
/*∆*/ 
/*∆*/     public final fun applyConstraints(constraints: org.w3c.dom.mediacapture.MediaTrackConstraints = ...): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun clone(): org.w3c.dom.mediacapture.MediaStreamTrack
/*∆*/ 
/*∆*/     public final fun getCapabilities(): org.w3c.dom.mediacapture.MediaTrackCapabilities
/*∆*/ 
/*∆*/     public final fun getConstraints(): org.w3c.dom.mediacapture.MediaTrackConstraints
/*∆*/ 
/*∆*/     public final fun getSettings(): org.w3c.dom.mediacapture.MediaTrackSettings
/*∆*/ 
/*∆*/     public final fun stop(): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class MediaStreamTrackEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor MediaStreamTrackEvent(type: kotlin.String, eventInitDict: org.w3c.dom.mediacapture.MediaStreamTrackEventInit)
/*∆*/ 
/*∆*/     public open val track: org.w3c.dom.mediacapture.MediaStreamTrack { get; }
/*∆*/ 
/*∆*/     public companion object of MediaStreamTrackEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaStreamTrackEventInit : org.w3c.dom.EventInit {
/*∆*/     public abstract var track: org.w3c.dom.mediacapture.MediaStreamTrack? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface MediaStreamTrackState {
/*∆*/     public companion object of MediaStreamTrackState {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaTrackCapabilities {
/*∆*/     public open var aspectRatio: org.w3c.dom.mediacapture.DoubleRange? { get; set; }
/*∆*/ 
/*∆*/     public open var autoGainControl: kotlin.Array<kotlin.Boolean>? { get; set; }
/*∆*/ 
/*∆*/     public open var channelCount: org.w3c.dom.mediacapture.ULongRange? { get; set; }
/*∆*/ 
/*∆*/     public open var deviceId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var echoCancellation: kotlin.Array<kotlin.Boolean>? { get; set; }
/*∆*/ 
/*∆*/     public open var facingMode: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public open var frameRate: org.w3c.dom.mediacapture.DoubleRange? { get; set; }
/*∆*/ 
/*∆*/     public open var groupId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var height: org.w3c.dom.mediacapture.ULongRange? { get; set; }
/*∆*/ 
/*∆*/     public open var latency: org.w3c.dom.mediacapture.DoubleRange? { get; set; }
/*∆*/ 
/*∆*/     public open var noiseSuppression: kotlin.Array<kotlin.Boolean>? { get; set; }
/*∆*/ 
/*∆*/     public open var resizeMode: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleRate: org.w3c.dom.mediacapture.ULongRange? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleSize: org.w3c.dom.mediacapture.ULongRange? { get; set; }
/*∆*/ 
/*∆*/     public open var volume: org.w3c.dom.mediacapture.DoubleRange? { get; set; }
/*∆*/ 
/*∆*/     public open var width: org.w3c.dom.mediacapture.ULongRange? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaTrackConstraintSet {
/*∆*/     public open var aspectRatio: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var autoGainControl: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var channelCount: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var deviceId: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var echoCancellation: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var facingMode: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var frameRate: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var groupId: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var height: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var latency: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var noiseSuppression: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var resizeMode: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var sampleRate: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var sampleSize: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var volume: dynamic { get; set; }
/*∆*/ 
/*∆*/     public open var width: dynamic { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaTrackConstraints : org.w3c.dom.mediacapture.MediaTrackConstraintSet {
/*∆*/     public open var advanced: kotlin.Array<org.w3c.dom.mediacapture.MediaTrackConstraintSet>? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaTrackSettings {
/*∆*/     public open var aspectRatio: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var autoGainControl: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var channelCount: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var deviceId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var echoCancellation: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var facingMode: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var frameRate: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var groupId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var height: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var latency: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var noiseSuppression: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var resizeMode: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleRate: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleSize: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var volume: kotlin.Double? { get; set; }
/*∆*/ 
/*∆*/     public open var width: kotlin.Int? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface MediaTrackSupportedConstraints {
/*∆*/     public open var aspectRatio: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var autoGainControl: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var channelCount: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var deviceId: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var echoCancellation: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var facingMode: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var frameRate: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var groupId: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var height: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var latency: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var noiseSuppression: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var resizeMode: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleRate: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var sampleSize: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var volume: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var width: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class OverconstrainedErrorEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor OverconstrainedErrorEvent(type: kotlin.String, eventInitDict: org.w3c.dom.mediacapture.OverconstrainedErrorEventInit)
/*∆*/ 
/*∆*/     public open val error: dynamic { get; }
/*∆*/ 
/*∆*/     public companion object of OverconstrainedErrorEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface OverconstrainedErrorEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var error: dynamic { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface Settings {
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ULongRange {
/*∆*/     public open var max: kotlin.Int? { get; set; }
/*∆*/ 
/*∆*/     public open var min: kotlin.Int? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface VideoFacingModeEnum {
/*∆*/     public companion object of VideoFacingModeEnum {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface VideoResizeModeEnum {
/*∆*/     public companion object of VideoResizeModeEnum {
/*∆*/     }
/*∆*/ }