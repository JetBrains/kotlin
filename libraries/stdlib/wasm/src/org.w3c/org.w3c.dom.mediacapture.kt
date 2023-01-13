/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.mediacapture

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Exposes the JavaScript [MediaStream](https://developer.mozilla.org/en/docs/Web/API/MediaStream) to Kotlin
 */
public external open class MediaStream() : EventTarget, MediaProvider {
    constructor(stream: MediaStream)
    constructor(tracks: Array<MediaStreamTrack>)
    open val id: String
    open val active: Boolean
    var onaddtrack: ((MediaStreamTrackEvent) -> Dynamic?)?
    var onremovetrack: ((MediaStreamTrackEvent) -> Dynamic?)?
    fun getAudioTracks(): Array<MediaStreamTrack>
    fun getVideoTracks(): Array<MediaStreamTrack>
    fun getTracks(): Array<MediaStreamTrack>
    fun getTrackById(trackId: String): MediaStreamTrack?
    fun addTrack(track: MediaStreamTrack)
    fun removeTrack(track: MediaStreamTrack)
    fun clone(): MediaStream
}

/**
 * Exposes the JavaScript [MediaStreamTrack](https://developer.mozilla.org/en/docs/Web/API/MediaStreamTrack) to Kotlin
 */
public external abstract class MediaStreamTrack : EventTarget {
    open val kind: String
    open val id: String
    open val label: String
    open var enabled: Boolean
    open val muted: Boolean
    open var onmute: ((Event) -> Dynamic?)?
    open var onunmute: ((Event) -> Dynamic?)?
    open val readyState: MediaStreamTrackState
    open var onended: ((Event) -> Dynamic?)?
    open var onoverconstrained: ((Event) -> Dynamic?)?
    fun clone(): MediaStreamTrack
    fun stop()
    fun getCapabilities(): MediaTrackCapabilities
    fun getConstraints(): MediaTrackConstraints
    fun getSettings(): MediaTrackSettings
    fun applyConstraints(constraints: MediaTrackConstraints = definedExternally): Promise<Unit>
}

/**
 * Exposes the JavaScript [MediaTrackSupportedConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaTrackSupportedConstraints) to Kotlin
 */
public external interface MediaTrackSupportedConstraints {
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

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaTrackSupportedConstraints(width: Boolean? = true, height: Boolean? = true, aspectRatio: Boolean? = true, frameRate: Boolean? = true, facingMode: Boolean? = true, resizeMode: Boolean? = true, volume: Boolean? = true, sampleRate: Boolean? = true, sampleSize: Boolean? = true, echoCancellation: Boolean? = true, autoGainControl: Boolean? = true, noiseSuppression: Boolean? = true, latency: Boolean? = true, channelCount: Boolean? = true, deviceId: Boolean? = true, groupId: Boolean? = true): MediaTrackSupportedConstraints {
    val o = newJsObject()
    o["width"] = width
    o["height"] = height
    o["aspectRatio"] = aspectRatio
    o["frameRate"] = frameRate
    o["facingMode"] = facingMode
    o["resizeMode"] = resizeMode
    o["volume"] = volume
    o["sampleRate"] = sampleRate
    o["sampleSize"] = sampleSize
    o["echoCancellation"] = echoCancellation
    o["autoGainControl"] = autoGainControl
    o["noiseSuppression"] = noiseSuppression
    o["latency"] = latency
    o["channelCount"] = channelCount
    o["deviceId"] = deviceId
    o["groupId"] = groupId
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaTrackSupportedConstraints
}

public external interface MediaTrackCapabilities {
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
    var facingMode: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: Array<String>?
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
    var echoCancellation: Array<Boolean>?
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: Array<Boolean>?
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: Array<Boolean>?
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

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaTrackCapabilities(width: ULongRange? = undefined, height: ULongRange? = undefined, aspectRatio: DoubleRange? = undefined, frameRate: DoubleRange? = undefined, facingMode: Array<String>? = undefined, resizeMode: Array<String>? = undefined, volume: DoubleRange? = undefined, sampleRate: ULongRange? = undefined, sampleSize: ULongRange? = undefined, echoCancellation: Array<Boolean>? = undefined, autoGainControl: Array<Boolean>? = undefined, noiseSuppression: Array<Boolean>? = undefined, latency: DoubleRange? = undefined, channelCount: ULongRange? = undefined, deviceId: String? = undefined, groupId: String? = undefined): MediaTrackCapabilities {
    val o = newJsObject()
    o["width"] = width
    o["height"] = height
    o["aspectRatio"] = aspectRatio
    o["frameRate"] = frameRate
    o["facingMode"] = facingMode
    o["resizeMode"] = resizeMode
    o["volume"] = volume
    o["sampleRate"] = sampleRate
    o["sampleSize"] = sampleSize
    o["echoCancellation"] = echoCancellation
    o["autoGainControl"] = autoGainControl
    o["noiseSuppression"] = noiseSuppression
    o["latency"] = latency
    o["channelCount"] = channelCount
    o["deviceId"] = deviceId
    o["groupId"] = groupId
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaTrackCapabilities
}

/**
 * Exposes the JavaScript [MediaTrackConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaTrackConstraints) to Kotlin
 */
public external interface MediaTrackConstraints : MediaTrackConstraintSet {
    var advanced: Array<MediaTrackConstraintSet>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaTrackConstraints(advanced: Array<MediaTrackConstraintSet>? = undefined, width: Dynamic? = undefined, height: Dynamic? = undefined, aspectRatio: Dynamic? = undefined, frameRate: Dynamic? = undefined, facingMode: Dynamic? = undefined, resizeMode: Dynamic? = undefined, volume: Dynamic? = undefined, sampleRate: Dynamic? = undefined, sampleSize: Dynamic? = undefined, echoCancellation: Dynamic? = undefined, autoGainControl: Dynamic? = undefined, noiseSuppression: Dynamic? = undefined, latency: Dynamic? = undefined, channelCount: Dynamic? = undefined, deviceId: Dynamic? = undefined, groupId: Dynamic? = undefined): MediaTrackConstraints {
    val o = newJsObject()
    o["advanced"] = advanced
    o["width"] = width
    o["height"] = height
    o["aspectRatio"] = aspectRatio
    o["frameRate"] = frameRate
    o["facingMode"] = facingMode
    o["resizeMode"] = resizeMode
    o["volume"] = volume
    o["sampleRate"] = sampleRate
    o["sampleSize"] = sampleSize
    o["echoCancellation"] = echoCancellation
    o["autoGainControl"] = autoGainControl
    o["noiseSuppression"] = noiseSuppression
    o["latency"] = latency
    o["channelCount"] = channelCount
    o["deviceId"] = deviceId
    o["groupId"] = groupId
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaTrackConstraints
}

public external interface MediaTrackConstraintSet {
    var width: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var height: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var aspectRatio: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var frameRate: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var facingMode: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var resizeMode: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var volume: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var sampleRate: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var sampleSize: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var echoCancellation: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var autoGainControl: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var noiseSuppression: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var latency: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var channelCount: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var deviceId: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var groupId: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaTrackConstraintSet(width: Dynamic? = undefined, height: Dynamic? = undefined, aspectRatio: Dynamic? = undefined, frameRate: Dynamic? = undefined, facingMode: Dynamic? = undefined, resizeMode: Dynamic? = undefined, volume: Dynamic? = undefined, sampleRate: Dynamic? = undefined, sampleSize: Dynamic? = undefined, echoCancellation: Dynamic? = undefined, autoGainControl: Dynamic? = undefined, noiseSuppression: Dynamic? = undefined, latency: Dynamic? = undefined, channelCount: Dynamic? = undefined, deviceId: Dynamic? = undefined, groupId: Dynamic? = undefined): MediaTrackConstraintSet {
    val o = newJsObject()
    o["width"] = width
    o["height"] = height
    o["aspectRatio"] = aspectRatio
    o["frameRate"] = frameRate
    o["facingMode"] = facingMode
    o["resizeMode"] = resizeMode
    o["volume"] = volume
    o["sampleRate"] = sampleRate
    o["sampleSize"] = sampleSize
    o["echoCancellation"] = echoCancellation
    o["autoGainControl"] = autoGainControl
    o["noiseSuppression"] = noiseSuppression
    o["latency"] = latency
    o["channelCount"] = channelCount
    o["deviceId"] = deviceId
    o["groupId"] = groupId
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaTrackConstraintSet
}

/**
 * Exposes the JavaScript [MediaTrackSettings](https://developer.mozilla.org/en/docs/Web/API/MediaTrackSettings) to Kotlin
 */
public external interface MediaTrackSettings {
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

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaTrackSettings(width: Int? = undefined, height: Int? = undefined, aspectRatio: Double? = undefined, frameRate: Double? = undefined, facingMode: String? = undefined, resizeMode: String? = undefined, volume: Double? = undefined, sampleRate: Int? = undefined, sampleSize: Int? = undefined, echoCancellation: Boolean? = undefined, autoGainControl: Boolean? = undefined, noiseSuppression: Boolean? = undefined, latency: Double? = undefined, channelCount: Int? = undefined, deviceId: String? = undefined, groupId: String? = undefined): MediaTrackSettings {
    val o = newJsObject()
    o["width"] = width
    o["height"] = height
    o["aspectRatio"] = aspectRatio
    o["frameRate"] = frameRate
    o["facingMode"] = facingMode
    o["resizeMode"] = resizeMode
    o["volume"] = volume
    o["sampleRate"] = sampleRate
    o["sampleSize"] = sampleSize
    o["echoCancellation"] = echoCancellation
    o["autoGainControl"] = autoGainControl
    o["noiseSuppression"] = noiseSuppression
    o["latency"] = latency
    o["channelCount"] = channelCount
    o["deviceId"] = deviceId
    o["groupId"] = groupId
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaTrackSettings
}

/**
 * Exposes the JavaScript [MediaStreamTrackEvent](https://developer.mozilla.org/en/docs/Web/API/MediaStreamTrackEvent) to Kotlin
 */
public external open class MediaStreamTrackEvent(type: String, eventInitDict: MediaStreamTrackEventInit) : Event {
    open val track: MediaStreamTrack

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaStreamTrackEventInit : EventInit {
    var track: MediaStreamTrack?
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaStreamTrackEventInit(track: MediaStreamTrack?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaStreamTrackEventInit {
    val o = newJsObject()
    o["track"] = track
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaStreamTrackEventInit
}

public external open class OverconstrainedErrorEvent(type: String, eventInitDict: OverconstrainedErrorEventInit) : Event {
    open val error: Dynamic?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface OverconstrainedErrorEventInit : EventInit {
    var error: Dynamic? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun OverconstrainedErrorEventInit(error: Dynamic? = null.unsafeCast<Dynamic?>(), bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): OverconstrainedErrorEventInit {
    val o = newJsObject()
    o["error"] = error
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as OverconstrainedErrorEventInit
}

/**
 * Exposes the JavaScript [MediaDevices](https://developer.mozilla.org/en/docs/Web/API/MediaDevices) to Kotlin
 */
public external abstract class MediaDevices : EventTarget {
    open var ondevicechange: ((Event) -> Dynamic?)?
    fun enumerateDevices(): Promise<Array<MediaDeviceInfo>>
    fun getSupportedConstraints(): MediaTrackSupportedConstraints
    fun getUserMedia(constraints: MediaStreamConstraints = definedExternally): Promise<MediaStream>
}

/**
 * Exposes the JavaScript [MediaDeviceInfo](https://developer.mozilla.org/en/docs/Web/API/MediaDeviceInfo) to Kotlin
 */
public external abstract class MediaDeviceInfo {
    open val deviceId: String
    open val kind: MediaDeviceKind
    open val label: String
    open val groupId: String
    fun toJSON(): Dynamic?
}

public external abstract class InputDeviceInfo : MediaDeviceInfo {
    fun getCapabilities(): MediaTrackCapabilities
}

/**
 * Exposes the JavaScript [MediaStreamConstraints](https://developer.mozilla.org/en/docs/Web/API/MediaStreamConstraints) to Kotlin
 */
public external interface MediaStreamConstraints {
    var video: Dynamic? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var audio: Dynamic? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaStreamConstraints(video: Dynamic? = false.unsafeCast<Dynamic?>(), audio: Dynamic? = false.unsafeCast<Dynamic?>()): MediaStreamConstraints {
    val o = newJsObject()
    o["video"] = video
    o["audio"] = audio
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as MediaStreamConstraints
}

public external interface ConstrainablePattern {
    var onoverconstrained: ((Event) -> Dynamic?)?
        get() = definedExternally
        set(value) = definedExternally
    fun getCapabilities(): Capabilities
    fun getConstraints(): Constraints
    fun getSettings(): Settings
    fun applyConstraints(constraints: Constraints = definedExternally): Promise<Unit>
}

/**
 * Exposes the JavaScript [DoubleRange](https://developer.mozilla.org/en/docs/Web/API/DoubleRange) to Kotlin
 */
public external interface DoubleRange {
    var max: Double?
        get() = definedExternally
        set(value) = definedExternally
    var min: Double?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun DoubleRange(max: Double? = undefined, min: Double? = undefined): DoubleRange {
    val o = newJsObject()
    o["max"] = max
    o["min"] = min
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as DoubleRange
}

public external interface ConstrainDoubleRange : DoubleRange {
    var exact: Double?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Double?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ConstrainDoubleRange(exact: Double? = undefined, ideal: Double? = undefined, max: Double? = undefined, min: Double? = undefined): ConstrainDoubleRange {
    val o = newJsObject()
    o["exact"] = exact
    o["ideal"] = ideal
    o["max"] = max
    o["min"] = min
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ConstrainDoubleRange
}

public external interface ULongRange {
    var max: Int?
        get() = definedExternally
        set(value) = definedExternally
    var min: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ULongRange(max: Int? = undefined, min: Int? = undefined): ULongRange {
    val o = newJsObject()
    o["max"] = max
    o["min"] = min
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ULongRange
}

public external interface ConstrainULongRange : ULongRange {
    var exact: Int?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ConstrainULongRange(exact: Int? = undefined, ideal: Int? = undefined, max: Int? = undefined, min: Int? = undefined): ConstrainULongRange {
    val o = newJsObject()
    o["exact"] = exact
    o["ideal"] = ideal
    o["max"] = max
    o["min"] = min
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ConstrainULongRange
}

/**
 * Exposes the JavaScript [ConstrainBooleanParameters](https://developer.mozilla.org/en/docs/Web/API/ConstrainBooleanParameters) to Kotlin
 */
public external interface ConstrainBooleanParameters {
    var exact: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ConstrainBooleanParameters(exact: Boolean? = undefined, ideal: Boolean? = undefined): ConstrainBooleanParameters {
    val o = newJsObject()
    o["exact"] = exact
    o["ideal"] = ideal
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ConstrainBooleanParameters
}

/**
 * Exposes the JavaScript [ConstrainDOMStringParameters](https://developer.mozilla.org/en/docs/Web/API/ConstrainDOMStringParameters) to Kotlin
 */
public external interface ConstrainDOMStringParameters {
    var exact: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
    var ideal: Dynamic?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ConstrainDOMStringParameters(exact: Dynamic? = undefined, ideal: Dynamic? = undefined): ConstrainDOMStringParameters {
    val o = newJsObject()
    o["exact"] = exact
    o["ideal"] = ideal
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ConstrainDOMStringParameters
}

public external interface Capabilities

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun Capabilities(): Capabilities {
    val o = newJsObject()
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as Capabilities
}

public external interface Settings

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun Settings(): Settings {
    val o = newJsObject()
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as Settings
}

public external interface ConstraintSet

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun ConstraintSet(): ConstraintSet {
    val o = newJsObject()
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as ConstraintSet
}

public external interface Constraints : ConstraintSet {
    var advanced: Array<ConstraintSet>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun Constraints(advanced: Array<ConstraintSet>? = undefined): Constraints {
    val o = newJsObject()
    o["advanced"] = advanced
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return o as Constraints
}

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaStreamTrackState {
    companion object
}

public inline val MediaStreamTrackState.Companion.LIVE: MediaStreamTrackState get() = "live".asDynamic().unsafeCast<MediaStreamTrackState>()

public inline val MediaStreamTrackState.Companion.ENDED: MediaStreamTrackState get() = "ended".asDynamic().unsafeCast<MediaStreamTrackState>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface VideoFacingModeEnum {
    companion object
}

public inline val VideoFacingModeEnum.Companion.USER: VideoFacingModeEnum get() = "user".asDynamic().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.ENVIRONMENT: VideoFacingModeEnum get() = "environment".asDynamic().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.LEFT: VideoFacingModeEnum get() = "left".asDynamic().unsafeCast<VideoFacingModeEnum>()

public inline val VideoFacingModeEnum.Companion.RIGHT: VideoFacingModeEnum get() = "right".asDynamic().unsafeCast<VideoFacingModeEnum>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface VideoResizeModeEnum {
    companion object
}

public inline val VideoResizeModeEnum.Companion.NONE: VideoResizeModeEnum get() = "none".asDynamic().unsafeCast<VideoResizeModeEnum>()

public inline val VideoResizeModeEnum.Companion.CROP_AND_SCALE: VideoResizeModeEnum get() = "crop-and-scale".asDynamic().unsafeCast<VideoResizeModeEnum>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaDeviceKind {
    companion object
}

public inline val MediaDeviceKind.Companion.AUDIOINPUT: MediaDeviceKind get() = "audioinput".asDynamic().unsafeCast<MediaDeviceKind>()

public inline val MediaDeviceKind.Companion.AUDIOOUTPUT: MediaDeviceKind get() = "audiooutput".asDynamic().unsafeCast<MediaDeviceKind>()

public inline val MediaDeviceKind.Companion.VIDEOINPUT: MediaDeviceKind get() = "videoinput".asDynamic().unsafeCast<MediaDeviceKind>()