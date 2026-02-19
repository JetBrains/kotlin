/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress("NO_EXPLICIT_VISIBILITY_IN_API_MODE", "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE") // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.dom.encryptedmedia

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*

/**
 * Exposes the JavaScript [MediaKeySystemConfiguration](https://developer.mozilla.org/en/docs/Web/API/MediaKeySystemConfiguration) to Kotlin
 */
public external interface MediaKeySystemConfiguration {
    var label: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var initDataTypes: Array<String>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var audioCapabilities: Array<MediaKeySystemMediaCapability>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var videoCapabilities: Array<MediaKeySystemMediaCapability>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var distinctiveIdentifier: MediaKeysRequirement? /* = MediaKeysRequirement.OPTIONAL */
        get() = definedExternally
        set(value) = definedExternally
    var persistentState: MediaKeysRequirement? /* = MediaKeysRequirement.OPTIONAL */
        get() = definedExternally
        set(value) = definedExternally
    var sessionTypes: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaKeySystemConfiguration(label: String? = "", initDataTypes: Array<String>? = arrayOf(), audioCapabilities: Array<MediaKeySystemMediaCapability>? = arrayOf(), videoCapabilities: Array<MediaKeySystemMediaCapability>? = arrayOf(), distinctiveIdentifier: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL, persistentState: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL, sessionTypes: Array<String>? = undefined): MediaKeySystemConfiguration {
    val o = js("({})")
    o["label"] = label
    o["initDataTypes"] = initDataTypes
    o["audioCapabilities"] = audioCapabilities
    o["videoCapabilities"] = videoCapabilities
    o["distinctiveIdentifier"] = distinctiveIdentifier
    o["persistentState"] = persistentState
    o["sessionTypes"] = sessionTypes
    return o
}

public external interface MediaKeySystemMediaCapability {
    var contentType: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var robustness: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaKeySystemMediaCapability(contentType: String? = "", robustness: String? = ""): MediaKeySystemMediaCapability {
    val o = js("({})")
    o["contentType"] = contentType
    o["robustness"] = robustness
    return o
}

/**
 * Exposes the JavaScript [MediaKeySystemAccess](https://developer.mozilla.org/en/docs/Web/API/MediaKeySystemAccess) to Kotlin
 */
public external abstract class MediaKeySystemAccess {
    open val keySystem: String
    fun getConfiguration(): MediaKeySystemConfiguration
    fun createMediaKeys(): Promise<MediaKeys>
}

/**
 * Exposes the JavaScript [MediaKeys](https://developer.mozilla.org/en/docs/Web/API/MediaKeys) to Kotlin
 */
public external abstract class MediaKeys {
    fun createSession(sessionType: MediaKeySessionType = definedExternally): MediaKeySession
    fun setServerCertificate(serverCertificate: dynamic): Promise<Boolean>
}

/**
 * Exposes the JavaScript [MediaKeySession](https://developer.mozilla.org/en/docs/Web/API/MediaKeySession) to Kotlin
 */
public external abstract class MediaKeySession : EventTarget {
    open val sessionId: String
    open val expiration: Double
    open val closed: Promise<Unit>
    open val keyStatuses: MediaKeyStatusMap
    open var onkeystatuseschange: ((Event) -> dynamic)?
    open var onmessage: ((MessageEvent) -> dynamic)?
    fun generateRequest(initDataType: String, initData: dynamic): Promise<Unit>
    fun load(sessionId: String): Promise<Boolean>
    fun update(response: dynamic): Promise<Unit>
    fun close(): Promise<Unit>
    fun remove(): Promise<Unit>
}

/**
 * Exposes the JavaScript [MediaKeyStatusMap](https://developer.mozilla.org/en/docs/Web/API/MediaKeyStatusMap) to Kotlin
 */
public external abstract class MediaKeyStatusMap {
    open val size: Int
    fun has(keyId: dynamic): Boolean
    fun get(keyId: dynamic): Any?
}

/**
 * Exposes the JavaScript [MediaKeyMessageEvent](https://developer.mozilla.org/en/docs/Web/API/MediaKeyMessageEvent) to Kotlin
 */
public external open class MediaKeyMessageEvent(type: String, eventInitDict: MediaKeyMessageEventInit) : Event {
    open val messageType: MediaKeyMessageType
    open val message: ArrayBuffer

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaKeyMessageEventInit : EventInit {
    var messageType: MediaKeyMessageType?
    var message: ArrayBuffer?
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaKeyMessageEventInit(messageType: MediaKeyMessageType?, message: ArrayBuffer?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaKeyMessageEventInit {
    val o = js("({})")
    o["messageType"] = messageType
    o["message"] = message
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    return o
}

public external open class MediaEncryptedEvent(type: String, eventInitDict: MediaEncryptedEventInit = definedExternally) : Event {
    open val initDataType: String
    open val initData: ArrayBuffer?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaEncryptedEventInit : EventInit {
    var initDataType: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var initData: ArrayBuffer? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun MediaEncryptedEventInit(initDataType: String? = "", initData: ArrayBuffer? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaEncryptedEventInit {
    val o = js("({})")
    o["initDataType"] = initDataType
    o["initData"] = initData
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    return o
}

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeysRequirement {
    companion object
}

public inline val MediaKeysRequirement.Companion.REQUIRED: MediaKeysRequirement get() = "required".asDynamic().unsafeCast<MediaKeysRequirement>()

public inline val MediaKeysRequirement.Companion.OPTIONAL: MediaKeysRequirement get() = "optional".asDynamic().unsafeCast<MediaKeysRequirement>()

public inline val MediaKeysRequirement.Companion.NOT_ALLOWED: MediaKeysRequirement get() = "not-allowed".asDynamic().unsafeCast<MediaKeysRequirement>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeySessionType {
    companion object
}

public inline val MediaKeySessionType.Companion.TEMPORARY: MediaKeySessionType get() = "temporary".asDynamic().unsafeCast<MediaKeySessionType>()

public inline val MediaKeySessionType.Companion.PERSISTENT_LICENSE: MediaKeySessionType get() = "persistent-license".asDynamic().unsafeCast<MediaKeySessionType>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeyStatus {
    companion object
}

public inline val MediaKeyStatus.Companion.USABLE: MediaKeyStatus get() = "usable".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.EXPIRED: MediaKeyStatus get() = "expired".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.RELEASED: MediaKeyStatus get() = "released".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.OUTPUT_RESTRICTED: MediaKeyStatus get() = "output-restricted".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.OUTPUT_DOWNSCALED: MediaKeyStatus get() = "output-downscaled".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.STATUS_PENDING: MediaKeyStatus get() = "status-pending".asDynamic().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.INTERNAL_ERROR: MediaKeyStatus get() = "internal-error".asDynamic().unsafeCast<MediaKeyStatus>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeyMessageType {
    companion object
}

public inline val MediaKeyMessageType.Companion.LICENSE_REQUEST: MediaKeyMessageType get() = "license-request".asDynamic().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.LICENSE_RENEWAL: MediaKeyMessageType get() = "license-renewal".asDynamic().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.LICENSE_RELEASE: MediaKeyMessageType get() = "license-release".asDynamic().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.INDIVIDUALIZATION_REQUEST: MediaKeyMessageType get() = "individualization-request".asDynamic().unsafeCast<MediaKeyMessageType>()