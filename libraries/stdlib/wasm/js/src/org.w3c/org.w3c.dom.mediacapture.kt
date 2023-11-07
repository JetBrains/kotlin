/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE") // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.dom.mediacapture

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Exposes the JavaScript [MediaStream](https://developer.mozilla.org/en/docs/Web/API/MediaStream) to Kotlin
 */
public external open class MediaStream() : EventTarget, MediaProvider, JsAny {
    constructor(stream: MediaStream)
    constructor(tracks: JsArray<MediaStreamTrack>)
    open val id: String
    open val active: Boolean
    var onaddtrack: ((MediaStreamTrackEvent) -> Unit)?
    var onremovetrack: ((MediaStreamTrackEvent) -> Unit)?
    fun getAudioTracks(): JsArray<MediaStreamTrack>
    fun getVideoTracks(): JsArray<MediaStreamTrack>
    fun getTracks(): JsArray<MediaStreamTrack>
    fun getTrackById(trackId: String): MediaStreamTrack?
    fun addTrack(track: MediaStreamTrack)
    fun removeTrack(track: MediaStreamTrack)
    fun clone(): MediaStream
}

/**
 * Exposes the JavaScript [MediaStreamTrack](https://developer.mozilla.org/en/docs/Web/API/MediaStreamTrack) to Kotlin
 */
public external abstract class MediaStreamTrack : EventTarget, JsAny {
    open val kind: String
    open val id: String
    open val label: String
    open var enabled: Boolean
    open val muted: Boolean
    open var onmute: ((Event) -> Unit)?
    open var onunmute: ((Event) -> Unit)?
    open val readyState: MediaStreamTrackState
    open var onended: ((Event) -> Unit)?
    open var onoverconstrained: ((Event) -> Unit)?
    fun clone(): MediaStreamTrack
    fun stop()
    fun getCapabilities(): MediaTrackCapabilities
    fun getConstraints(): MediaTrackConstraints
    fun getSettings(): MediaTrackSettings
    fun applyConstraints(constraints: MediaTrackConstraints = definedExternally): Promise<Nothing?>
}

/**
 * Exposes the JavaScript [MediaTrackSupportedConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaTrackSupportedConstraints) to Kotlin
 */
public external interface MediaTrackSupportedConstraints : JsAny {
    var width: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var height: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var aspectRatio: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var frameRate: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var facingMode: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var volume: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var sampleRate: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var sampleSize: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var echoCancellation: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var latency: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var channelCount: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var deviceId: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var groupId: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaTrackSupportedConstraints(width: Boolean? = true, height: Boolean? = true, aspectRatio: Boolean? = true, frameRate: Boolean? = true, facingMode: Boolean? = true, resizeMode: Boolean? = true, volume: Boolean? = true, sampleRate: Boolean? = true, sampleSize: Boolean? = true, echoCancellation: Boolean? = true, autoGainControl: Boolean? = true, noiseSuppression: Boolean? = true, latency: Boolean? = true, channelCount: Boolean? = true, deviceId: Boolean? = true, groupId: Boolean? = true): MediaTrackSupportedConstraints { js("return { width, height, aspectRatio, frameRate, facingMode, resizeMode, volume, sampleRate, sampleSize, echoCancellation, autoGainControl, noiseSuppression, latency, channelCount, deviceId, groupId };") }

public external interface MediaTrackCapabilities : JsAny {
    var width: ULongRange?
        get() = definedExternally
        set(value) = definedExternally
    var height: ULongRange?
        get() = definedExternally
        set(value) = definedExternally
    var aspectRatio: DoubleRange?
        get() = definedExternally
        set(value) = definedExternally
    var frameRate: DoubleRange?
        get() = definedExternally
        set(value) = definedExternally
    var facingMode: JsArray<JsString>?
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: JsArray<JsString>?
        get() = definedExternally
        set(value) = definedExternally
    var volume: DoubleRange?
        get() = definedExternally
        set(value) = definedExternally
    var sampleRate: ULongRange?
        get() = definedExternally
        set(value) = definedExternally
    var sampleSize: ULongRange?
        get() = definedExternally
        set(value) = definedExternally
    var echoCancellation: JsArray<JsBoolean>?
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: JsArray<JsBoolean>?
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: JsArray<JsBoolean>?
        get() = definedExternally
        set(value) = definedExternally
    var latency: DoubleRange?
        get() = definedExternally
        set(value) = definedExternally
    var channelCount: ULongRange?
        get() = definedExternally
        set(value) = definedExternally
    var deviceId: String?
        get() = definedExternally
        set(value) = definedExternally
    var groupId: String?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaTrackCapabilities(width: ULongRange? = undefined, height: ULongRange? = undefined, aspectRatio: DoubleRange? = undefined, frameRate: DoubleRange? = undefined, facingMode: JsArray<JsString>? = undefined, resizeMode: JsArray<JsString>? = undefined, volume: DoubleRange? = undefined, sampleRate: ULongRange? = undefined, sampleSize: ULongRange? = undefined, echoCancellation: JsArray<JsBoolean>? = undefined, autoGainControl: JsArray<JsBoolean>? = undefined, noiseSuppression: JsArray<JsBoolean>? = undefined, latency: DoubleRange? = undefined, channelCount: ULongRange? = undefined, deviceId: String? = undefined, groupId: String? = undefined): MediaTrackCapabilities { js("return { width, height, aspectRatio, frameRate, facingMode, resizeMode, volume, sampleRate, sampleSize, echoCancellation, autoGainControl, noiseSuppression, latency, channelCount, deviceId, groupId };") }

/**
 * Exposes the JavaScript [MediaTrackConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaTrackConstraints) to Kotlin
 */
public external interface MediaTrackConstraints : MediaTrackConstraintSet, JsAny {
    var advanced: JsArray<MediaTrackConstraintSet>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaTrackConstraints(advanced: JsArray<MediaTrackConstraintSet>? = undefined, width: JsAny? /* Int|ConstrainULongRange */ = undefined, height: JsAny? /* Int|ConstrainULongRange */ = undefined, aspectRatio: JsAny? /* Double|ConstrainDoubleRange */ = undefined, frameRate: JsAny? /* Double|ConstrainDoubleRange */ = undefined, facingMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, resizeMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, volume: JsAny? /* Double|ConstrainDoubleRange */ = undefined, sampleRate: JsAny? /* Int|ConstrainULongRange */ = undefined, sampleSize: JsAny? /* Int|ConstrainULongRange */ = undefined, echoCancellation: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, autoGainControl: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, noiseSuppression: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, latency: JsAny? /* Double|ConstrainDoubleRange */ = undefined, channelCount: JsAny? /* Int|ConstrainULongRange */ = undefined, deviceId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, groupId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined): MediaTrackConstraints { js("return { advanced, width, height, aspectRatio, frameRate, facingMode, resizeMode, volume, sampleRate, sampleSize, echoCancellation, autoGainControl, noiseSuppression, latency, channelCount, deviceId, groupId };") }

public external interface MediaTrackConstraintSet : JsAny {
    var width: JsAny? /* Int|ConstrainULongRange */
        get() = definedExternally
        set(value) = definedExternally
    var height: JsAny? /* Int|ConstrainULongRange */
        get() = definedExternally
        set(value) = definedExternally
    var aspectRatio: JsAny? /* Double|ConstrainDoubleRange */
        get() = definedExternally
        set(value) = definedExternally
    var frameRate: JsAny? /* Double|ConstrainDoubleRange */
        get() = definedExternally
        set(value) = definedExternally
    var facingMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */
        get() = definedExternally
        set(value) = definedExternally
    var volume: JsAny? /* Double|ConstrainDoubleRange */
        get() = definedExternally
        set(value) = definedExternally
    var sampleRate: JsAny? /* Int|ConstrainULongRange */
        get() = definedExternally
        set(value) = definedExternally
    var sampleSize: JsAny? /* Int|ConstrainULongRange */
        get() = definedExternally
        set(value) = definedExternally
    var echoCancellation: JsAny? /* Boolean|ConstrainBooleanParameters */
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: JsAny? /* Boolean|ConstrainBooleanParameters */
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: JsAny? /* Boolean|ConstrainBooleanParameters */
        get() = definedExternally
        set(value) = definedExternally
    var latency: JsAny? /* Double|ConstrainDoubleRange */
        get() = definedExternally
        set(value) = definedExternally
    var channelCount: JsAny? /* Int|ConstrainULongRange */
        get() = definedExternally
        set(value) = definedExternally
    var deviceId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */
        get() = definedExternally
        set(value) = definedExternally
    var groupId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaTrackConstraintSet(width: JsAny? /* Int|ConstrainULongRange */ = undefined, height: JsAny? /* Int|ConstrainULongRange */ = undefined, aspectRatio: JsAny? /* Double|ConstrainDoubleRange */ = undefined, frameRate: JsAny? /* Double|ConstrainDoubleRange */ = undefined, facingMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, resizeMode: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, volume: JsAny? /* Double|ConstrainDoubleRange */ = undefined, sampleRate: JsAny? /* Int|ConstrainULongRange */ = undefined, sampleSize: JsAny? /* Int|ConstrainULongRange */ = undefined, echoCancellation: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, autoGainControl: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, noiseSuppression: JsAny? /* Boolean|ConstrainBooleanParameters */ = undefined, latency: JsAny? /* Double|ConstrainDoubleRange */ = undefined, channelCount: JsAny? /* Int|ConstrainULongRange */ = undefined, deviceId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined, groupId: JsAny? /* String|JsArray<JsString>|ConstrainDOMStringParameters */ = undefined): MediaTrackConstraintSet { js("return { width, height, aspectRatio, frameRate, facingMode, resizeMode, volume, sampleRate, sampleSize, echoCancellation, autoGainControl, noiseSuppression, latency, channelCount, deviceId, groupId };") }

/**
 * Exposes the JavaScript [MediaTrackSettings](https://developer.mozilla.org/en/docs/Web/API/MediaTrackSettings) to Kotlin
 */
public external interface MediaTrackSettings : JsAny {
    var width: Int?
        get() = definedExternally
        set(value) = definedExternally
    var height: Int?
        get() = definedExternally
        set(value) = definedExternally
    var aspectRatio: Double?
        get() = definedExternally
        set(value) = definedExternally
    var frameRate: Double?
        get() = definedExternally
        set(value) = definedExternally
    var facingMode: String?
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: String?
        get() = definedExternally
        set(value) = definedExternally
    var volume: Double?
        get() = definedExternally
        set(value) = definedExternally
    var sampleRate: Int?
        get() = definedExternally
        set(value) = definedExternally
    var sampleSize: Int?
        get() = definedExternally
        set(value) = definedExternally
    var echoCancellation: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var latency: Double?
        get() = definedExternally
        set(value) = definedExternally
    var channelCount: Int?
        get() = definedExternally
        set(value) = definedExternally
    var deviceId: String?
        get() = definedExternally
        set(value) = definedExternally
    var groupId: String?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaTrackSettings(width: Int? = undefined, height: Int? = undefined, aspectRatio: Double? = undefined, frameRate: Double? = undefined, facingMode: String? = undefined, resizeMode: String? = undefined, volume: Double? = undefined, sampleRate: Int? = undefined, sampleSize: Int? = undefined, echoCancellation: Boolean? = undefined, autoGainControl: Boolean? = undefined, noiseSuppression: Boolean? = undefined, latency: Double? = undefined, channelCount: Int? = undefined, deviceId: String? = undefined, groupId: String? = undefined): MediaTrackSettings { js("return { width, height, aspectRatio, frameRate, facingMode, resizeMode, volume, sampleRate, sampleSize, echoCancellation, autoGainControl, noiseSuppression, latency, channelCount, deviceId, groupId };") }

/**
 * Exposes the JavaScript [MediaStreamTrackEvent](https://developer.mozilla.org/en/docs/Web/API/MediaStreamTrackEvent) to Kotlin
 */
public external open class MediaStreamTrackEvent(type: String, eventInitDict: MediaStreamTrackEventInit) : Event, JsAny {
    open val track: MediaStreamTrack

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaStreamTrackEventInit : EventInit, JsAny {
    var track: MediaStreamTrack?
}

@Suppress("UNUSED_PARAMETER")
public fun MediaStreamTrackEventInit(track: MediaStreamTrack?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaStreamTrackEventInit { js("return { track, bubbles, cancelable, composed };") }

public external open class OverconstrainedErrorEvent(type: String, eventInitDict: OverconstrainedErrorEventInit) : Event, JsAny {
    open val error: JsAny?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface OverconstrainedErrorEventInit : EventInit, JsAny {
    var error: JsAny? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun OverconstrainedErrorEventInit(error: JsAny? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): OverconstrainedErrorEventInit { js("return { error, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [MediaDevices](https://developer.mozilla.org/en/docs/Web/API/MediaDevices) to Kotlin
 */
public external abstract class MediaDevices : EventTarget, JsAny {
    open var ondevicechange: ((Event) -> Unit)?
    fun enumerateDevices(): Promise<JsArray<MediaDeviceInfo>>
    fun getSupportedConstraints(): MediaTrackSupportedConstraints
    fun getUserMedia(constraints: MediaStreamConstraints = definedExternally): Promise<MediaStream>
}

/**
 * Exposes the JavaScript [MediaDeviceInfo](https://developer.mozilla.org/en/docs/Web/API/MediaDeviceInfo) to Kotlin
 */
public external abstract class MediaDeviceInfo : JsAny {
    open val deviceId: String
    open val kind: MediaDeviceKind
    open val label: String
    open val groupId: String
    fun toJSON(): JsAny
}

public external abstract class InputDeviceInfo : MediaDeviceInfo, JsAny {
    fun getCapabilities(): MediaTrackCapabilities
}

/**
 * Exposes the JavaScript [MediaStreamConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaStreamConstraints) to Kotlin
 */
public external interface MediaStreamConstraints : JsAny {
    var video: JsAny? /* Boolean|MediaTrackConstraints */
        get() = definedExternally
        set(value) = definedExternally
    var audio: JsAny? /* Boolean|MediaTrackConstraints */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaStreamConstraints(video: JsAny? /* Boolean|MediaTrackConstraints */ = false.toJsBoolean(), audio: JsAny? /* Boolean|MediaTrackConstraints */ = false.toJsBoolean()): MediaStreamConstraints { js("return { video, audio };") }

public external interface ConstrainablePattern : JsAny {
    var onoverconstrained: ((Event) -> Unit)?
        get() = definedExternally
        set(value) = definedExternally
    fun getCapabilities(): Capabilities
    fun getConstraints(): Constraints
    fun getSettings(): Settings
    fun applyConstraints(constraints: Constraints = definedExternally): Promise<Nothing?>
}

/**
 * Exposes the JavaScript [DoubleRange](https://developer.mozilla.org/en/docs/Web/API/DoubleRange) to Kotlin
 */
public external interface DoubleRange : JsAny {
    var max: Double?
        get() = definedExternally
        set(value) = definedExternally
    var min: Double?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun DoubleRange(max: Double? = undefined, min: Double? = undefined): DoubleRange { js("return { max, min };") }

public external interface ConstrainDoubleRange : DoubleRange, JsAny {
    var exact: Double?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Double?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ConstrainDoubleRange(exact: Double? = undefined, ideal: Double? = undefined, max: Double? = undefined, min: Double? = undefined): ConstrainDoubleRange { js("return { exact, ideal, max, min };") }

public external interface ULongRange : JsAny {
    var max: Int?
        get() = definedExternally
        set(value) = definedExternally
    var min: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ULongRange(max: Int? = undefined, min: Int? = undefined): ULongRange { js("return { max, min };") }

public external interface ConstrainULongRange : ULongRange, JsAny {
    var exact: Int?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ConstrainULongRange(exact: Int? = undefined, ideal: Int? = undefined, max: Int? = undefined, min: Int? = undefined): ConstrainULongRange { js("return { exact, ideal, max, min };") }

/**
 * Exposes the JavaScript [ConstrainBooleanParameters](https://developer.mozilla.org/en/docs/Web/API/ConstrainBooleanParameters) to Kotlin
 */
public external interface ConstrainBooleanParameters : JsAny {
    var exact: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ConstrainBooleanParameters(exact: Boolean? = undefined, ideal: Boolean? = undefined): ConstrainBooleanParameters { js("return { exact, ideal };") }

/**
 * Exposes the JavaScript [ConstrainDOMStringParameters](https://developer.mozilla.org/en/docs/Web/API/ConstrainDOMStringParameters) to Kotlin
 */
public external interface ConstrainDOMStringParameters : JsAny {
    var exact: JsAny? /* String|JsArray<JsString> */
        get() = definedExternally
        set(value) = definedExternally
    var ideal: JsAny? /* String|JsArray<JsString> */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ConstrainDOMStringParameters(exact: JsAny? /* String|JsArray<JsString> */ = undefined, ideal: JsAny? /* String|JsArray<JsString> */ = undefined): ConstrainDOMStringParameters { js("return { exact, ideal };") }

public external interface Capabilities : JsAny

@Suppress("UNUSED_PARAMETER")
public fun Capabilities(): Capabilities { js("return {  };") }

public external interface Settings : JsAny

@Suppress("UNUSED_PARAMETER")
public fun Settings(): Settings { js("return {  };") }

public external interface ConstraintSet : JsAny

@Suppress("UNUSED_PARAMETER")
public fun ConstraintSet(): ConstraintSet { js("return {  };") }

public external interface Constraints : ConstraintSet, JsAny {
    var advanced: JsArray<ConstraintSet>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun Constraints(advanced: JsArray<ConstraintSet>? = undefined): Constraints { js("return { advanced };") }

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaStreamTrackState : JsAny {
    companion object
}

public inline val MediaStreamTrackState.Companion.LIVE: MediaStreamTrackState get() = "live".toJsString().unsafeCast<MediaStreamTrackState>()

public inline val MediaStreamTrackState.Companion.ENDED: MediaStreamTrackState get() = "ended".toJsString().unsafeCast<MediaStreamTrackState>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface VideoFacingModeEnum : JsAny {
    companion object
}

public inline val VideoFacingModeEnum.Companion.USER: VideoFacingModeEnum get() = "user".toJsString().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.ENVIRONMENT: VideoFacingModeEnum get() = "environment".toJsString().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.LEFT: VideoFacingModeEnum get() = "left".toJsString().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.RIGHT: VideoFacingModeEnum get() = "right".toJsString().unsafeCast<VideoFacingModeEnum>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface VideoResizeModeEnum : JsAny {
    companion object
}

public inline val VideoResizeModeEnum.Companion.NONE: VideoResizeModeEnum get() = "none".toJsString().unsafeCast<VideoResizeModeEnum>()

public inline val VideoResizeModeEnum.Companion.CROP_AND_SCALE: VideoResizeModeEnum get() = "crop-and-scale".toJsString().unsafeCast<VideoResizeModeEnum>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaDeviceKind : JsAny {
    companion object
}

public inline val MediaDeviceKind.Companion.AUDIOINPUT: MediaDeviceKind get() = "audioinput".toJsString().unsafeCast<MediaDeviceKind>()

public inline val MediaDeviceKind.Companion.AUDIOOUTPUT: MediaDeviceKind get() = "audiooutput".toJsString().unsafeCast<MediaDeviceKind>()

public inline val MediaDeviceKind.Companion.VIDEOINPUT: MediaDeviceKind get() = "videoinput".toJsString().unsafeCast<MediaDeviceKind>()