/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
public external interface MediaKeySystemConfiguration : JsAny {
    var label: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var initDataTypes: JsArray<JsString>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var audioCapabilities: JsArray<MediaKeySystemMediaCapability>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var videoCapabilities: JsArray<MediaKeySystemMediaCapability>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
    var distinctiveIdentifier: MediaKeysRequirement? /* = MediaKeysRequirement.OPTIONAL */
        get() = definedExternally
        set(value) = definedExternally
    var persistentState: MediaKeysRequirement? /* = MediaKeysRequirement.OPTIONAL */
        get() = definedExternally
        set(value) = definedExternally
    var sessionTypes: JsArray<JsString>?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaKeySystemConfiguration(label: String? = "", initDataTypes: JsArray<JsString>? = JsArray(), audioCapabilities: JsArray<MediaKeySystemMediaCapability>? = JsArray(), videoCapabilities: JsArray<MediaKeySystemMediaCapability>? = JsArray(), distinctiveIdentifier: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL, persistentState: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL, sessionTypes: JsArray<JsString>? = undefined): MediaKeySystemConfiguration { js("return { label, initDataTypes, audioCapabilities, videoCapabilities, distinctiveIdentifier, persistentState, sessionTypes };") }

public external interface MediaKeySystemMediaCapability : JsAny {
    var contentType: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var robustness: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaKeySystemMediaCapability(contentType: String? = "", robustness: String? = ""): MediaKeySystemMediaCapability { js("return { contentType, robustness };") }

/**
 * Exposes the JavaScript [MediaKeySystemAccess](https://developer.mozilla.org/en/docs/Web/API/MediaKeySystemAccess) to Kotlin
 */
public external abstract class MediaKeySystemAccess : JsAny {
    open val keySystem: String
    fun getConfiguration(): MediaKeySystemConfiguration
    fun createMediaKeys(): Promise<MediaKeys>
}

/**
 * Exposes the JavaScript [MediaKeys](https://developer.mozilla.org/en/docs/Web/API/MediaKeys) to Kotlin
 */
public external abstract class MediaKeys : JsAny {
    fun createSession(sessionType: MediaKeySessionType = definedExternally): MediaKeySession
    fun setServerCertificate(serverCertificate: JsAny?): Promise<JsBoolean>
}

/**
 * Exposes the JavaScript [MediaKeySession](https://developer.mozilla.org/en/docs/Web/API/MediaKeySession) to Kotlin
 */
public external abstract class MediaKeySession : EventTarget, JsAny {
    open val sessionId: String
    open val expiration: Double
    open val closed: Promise<Nothing?>
    open val keyStatuses: MediaKeyStatusMap
    open var onkeystatuseschange: ((Event) -> Unit)?
    open var onmessage: ((MessageEvent) -> Unit)?
    fun generateRequest(initDataType: String, initData: JsAny?): Promise<Nothing?>
    fun load(sessionId: String): Promise<JsBoolean>
    fun update(response: JsAny?): Promise<Nothing?>
    fun close(): Promise<Nothing?>
    fun remove(): Promise<Nothing?>
}

/**
 * Exposes the JavaScript [MediaKeyStatusMap](https://developer.mozilla.org/en/docs/Web/API/MediaKeyStatusMap) to Kotlin
 */
public external abstract class MediaKeyStatusMap : JsAny {
    open val size: Int
    fun has(keyId: JsAny?): Boolean
    fun get(keyId: JsAny?): JsAny?
}

/**
 * Exposes the JavaScript [MediaKeyMessageEvent](https://developer.mozilla.org/en/docs/Web/API/MediaKeyMessageEvent) to Kotlin
 */
public external open class MediaKeyMessageEvent(type: String, eventInitDict: MediaKeyMessageEventInit) : Event, JsAny {
    open val messageType: MediaKeyMessageType
    open val message: ArrayBuffer

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaKeyMessageEventInit : EventInit, JsAny {
    var messageType: MediaKeyMessageType?
    var message: ArrayBuffer?
}

@Suppress("UNUSED_PARAMETER")
public fun MediaKeyMessageEventInit(messageType: MediaKeyMessageType?, message: ArrayBuffer?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaKeyMessageEventInit { js("return { messageType, message, bubbles, cancelable, composed };") }

public external open class MediaEncryptedEvent(type: String, eventInitDict: MediaEncryptedEventInit = definedExternally) : Event, JsAny {
    open val initDataType: String
    open val initData: ArrayBuffer?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MediaEncryptedEventInit : EventInit, JsAny {
    var initDataType: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var initData: ArrayBuffer? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MediaEncryptedEventInit(initDataType: String? = "", initData: ArrayBuffer? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MediaEncryptedEventInit { js("return { initDataType, initData, bubbles, cancelable, composed };") }

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeysRequirement : JsAny {
    companion object
}

public inline val MediaKeysRequirement.Companion.REQUIRED: MediaKeysRequirement get() = "required".toJsString().unsafeCast<MediaKeysRequirement>()

public inline val MediaKeysRequirement.Companion.OPTIONAL: MediaKeysRequirement get() = "optional".toJsString().unsafeCast<MediaKeysRequirement>()

public inline val MediaKeysRequirement.Companion.NOT_ALLOWED: MediaKeysRequirement get() = "not-allowed".toJsString().unsafeCast<MediaKeysRequirement>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeySessionType : JsAny {
    companion object
}

public inline val MediaKeySessionType.Companion.TEMPORARY: MediaKeySessionType get() = "temporary".toJsString().unsafeCast<MediaKeySessionType>()

public inline val MediaKeySessionType.Companion.PERSISTENT_LICENSE: MediaKeySessionType get() = "persistent-license".toJsString().unsafeCast<MediaKeySessionType>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeyStatus : JsAny {
    companion object
}

public inline val MediaKeyStatus.Companion.USABLE: MediaKeyStatus get() = "usable".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.EXPIRED: MediaKeyStatus get() = "expired".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.RELEASED: MediaKeyStatus get() = "released".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.OUTPUT_RESTRICTED: MediaKeyStatus get() = "output-restricted".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.OUTPUT_DOWNSCALED: MediaKeyStatus get() = "output-downscaled".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.STATUS_PENDING: MediaKeyStatus get() = "status-pending".toJsString().unsafeCast<MediaKeyStatus>()

public inline val MediaKeyStatus.Companion.INTERNAL_ERROR: MediaKeyStatus get() = "internal-error".toJsString().unsafeCast<MediaKeyStatus>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface MediaKeyMessageType : JsAny {
    companion object
}

public inline val MediaKeyMessageType.Companion.LICENSE_REQUEST: MediaKeyMessageType get() = "license-request".toJsString().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.LICENSE_RENEWAL: MediaKeyMessageType get() = "license-renewal".toJsString().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.LICENSE_RELEASE: MediaKeyMessageType get() = "license-release".toJsString().unsafeCast<MediaKeyMessageType>()

public inline val MediaKeyMessageType.Companion.INDIVIDUALIZATION_REQUEST: MediaKeyMessageType get() = "individualization-request".toJsString().unsafeCast<MediaKeyMessageType>()