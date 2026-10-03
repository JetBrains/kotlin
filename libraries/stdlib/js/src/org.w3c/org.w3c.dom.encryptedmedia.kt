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

package org.w3c.dom.encryptedmedia

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.internal.InlineOnly

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeySystemConfiguration {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var label: String?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var initDataTypes: Array<String>?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var audioCapabilities: Array<MediaKeySystemMediaCapability>?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var videoCapabilities: Array<MediaKeySystemMediaCapability>?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var distinctiveIdentifier: MediaKeysRequirement?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var persistentState: MediaKeysRequirement?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var sessionTypes: Array<String>?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun MediaKeySystemConfiguration(
    label: String? = "",
    initDataTypes: Array<String>? = arrayOf(),
    audioCapabilities: Array<MediaKeySystemMediaCapability>? = arrayOf(),
    videoCapabilities: Array<MediaKeySystemMediaCapability>? = arrayOf(),
    distinctiveIdentifier: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL,
    persistentState: MediaKeysRequirement? = MediaKeysRequirement.OPTIONAL,
    sessionTypes: Array<String>? = undefined
): MediaKeySystemConfiguration {
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

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeySystemMediaCapability {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var contentType: String?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var robustness: String?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun MediaKeySystemMediaCapability(contentType: String? = "", robustness: String? = ""): MediaKeySystemMediaCapability {
    val o = js("({})")
    o["contentType"] = contentType
    o["robustness"] = robustness
    return o
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class MediaKeySystemAccess {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val keySystem: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun getConfiguration(): MediaKeySystemConfiguration

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createMediaKeys(): Promise<MediaKeys>
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class MediaKeys {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun createSession(sessionType: MediaKeySessionType = definedExternally): MediaKeySession

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun setServerCertificate(serverCertificate: dynamic): Promise<Boolean>
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class MediaKeySession : EventTarget {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val sessionId: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val expiration: Double

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val closed: Promise<Unit>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val keyStatuses: MediaKeyStatusMap

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onkeystatuseschange: ((Event) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open var onmessage: ((MessageEvent) -> dynamic)?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun generateRequest(initDataType: String, initData: dynamic): Promise<Unit>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun load(sessionId: String): Promise<Boolean>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun update(response: dynamic): Promise<Unit>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun close(): Promise<Unit>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun remove(): Promise<Unit>
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class MediaKeyStatusMap {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val size: Int

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun has(keyId: dynamic): Boolean

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun get(keyId: dynamic): Any?
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class MediaKeyMessageEvent(type: String, eventInitDict: MediaKeyMessageEventInit) : Event {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val messageType: MediaKeyMessageType

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val message: ArrayBuffer

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NONE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CAPTURING_PHASE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val AT_TARGET: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val BUBBLING_PHASE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeyMessageEventInit : EventInit {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var messageType: MediaKeyMessageType?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var message: ArrayBuffer?
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun MediaKeyMessageEventInit(
    messageType: MediaKeyMessageType?,
    message: ArrayBuffer?,
    bubbles: Boolean? = false,
    cancelable: Boolean? = false,
    composed: Boolean? = false
): MediaKeyMessageEventInit {
    val o = js("({})")
    o["messageType"] = messageType
    o["message"] = message
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    return o
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class MediaEncryptedEvent(type: String, eventInitDict: MediaEncryptedEventInit = definedExternally) : Event {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val initDataType: String

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val initData: ArrayBuffer?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NONE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CAPTURING_PHASE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val AT_TARGET: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val BUBBLING_PHASE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaEncryptedEventInit : EventInit {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var initDataType: String?
        get() = definedExternally
        set(value) = definedExternally

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var initData: ArrayBuffer?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun MediaEncryptedEventInit(
    initDataType: String? = "",
    initData: ArrayBuffer? = null,
    bubbles: Boolean? = false,
    cancelable: Boolean? = false,
    composed: Boolean? = false
): MediaEncryptedEventInit {
    val o = js("({})")
    o["initDataType"] = initDataType
    o["initData"] = initData
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    return o
}

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeysRequirement {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeysRequirement.Companion.REQUIRED: MediaKeysRequirement
    get() = "required".asDynamic().unsafeCast<MediaKeysRequirement>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeysRequirement.Companion.OPTIONAL: MediaKeysRequirement
    get() = "optional".asDynamic().unsafeCast<MediaKeysRequirement>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeysRequirement.Companion.NOT_ALLOWED: MediaKeysRequirement
    get() = "not-allowed".asDynamic().unsafeCast<MediaKeysRequirement>()

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeySessionType {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeySessionType.Companion.TEMPORARY: MediaKeySessionType
    get() = "temporary".asDynamic().unsafeCast<MediaKeySessionType>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeySessionType.Companion.PERSISTENT_LICENSE: MediaKeySessionType
    get() = "persistent-license".asDynamic().unsafeCast<MediaKeySessionType>()

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeyStatus {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.USABLE: MediaKeyStatus get() = "usable".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.EXPIRED: MediaKeyStatus get() = "expired".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.RELEASED: MediaKeyStatus get() = "released".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.OUTPUT_RESTRICTED: MediaKeyStatus
    get() = "output-restricted".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.OUTPUT_DOWNSCALED: MediaKeyStatus
    get() = "output-downscaled".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.STATUS_PENDING: MediaKeyStatus get() = "status-pending".asDynamic().unsafeCast<MediaKeyStatus>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyStatus.Companion.INTERNAL_ERROR: MediaKeyStatus get() = "internal-error".asDynamic().unsafeCast<MediaKeyStatus>()

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
@JsName("null")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface MediaKeyMessageType {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyMessageType.Companion.LICENSE_REQUEST: MediaKeyMessageType
    get() = "license-request".asDynamic().unsafeCast<MediaKeyMessageType>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyMessageType.Companion.LICENSE_RENEWAL: MediaKeyMessageType
    get() = "license-renewal".asDynamic().unsafeCast<MediaKeyMessageType>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyMessageType.Companion.LICENSE_RELEASE: MediaKeyMessageType
    get() = "license-release".asDynamic().unsafeCast<MediaKeyMessageType>()

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public val MediaKeyMessageType.Companion.INDIVIDUALIZATION_REQUEST: MediaKeyMessageType
    get() = "individualization-request".asDynamic().unsafeCast<MediaKeyMessageType>()
