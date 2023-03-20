/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.xhr

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.files.*

/**
 * Exposes the JavaScript [XMLHttpRequestEventTarget](https://developer.mozilla.org/en/docs/Web/API/XMLHttpRequestEventTarget) to Kotlin
 */
public external abstract class XMLHttpRequestEventTarget : EventTarget, JsAny {
    open var onloadstart: ((ProgressEvent) -> JsAny?)?
    open var onprogress: ((ProgressEvent) -> JsAny?)?
    open var onabort: ((Event) -> JsAny?)?
    open var onerror: ((Event) -> JsAny?)?
    open var onload: ((Event) -> JsAny?)?
    open var ontimeout: ((Event) -> JsAny?)?
    open var onloadend: ((Event) -> JsAny?)?
}

public external abstract class XMLHttpRequestUpload : XMLHttpRequestEventTarget, JsAny

/**
 * Exposes the JavaScript [XMLHttpRequest](https://developer.mozilla.org/en/docs/Web/API/XMLHttpRequest) to Kotlin
 */
public external open class XMLHttpRequest : XMLHttpRequestEventTarget, JsAny {
    var onreadystatechange: ((Event) -> JsAny?)?
    open val readyState: Short
    var timeout: Int
    var withCredentials: Boolean
    open val upload: XMLHttpRequestUpload
    open val responseURL: String
    open val status: Short
    open val statusText: String
    var responseType: XMLHttpRequestResponseType
    open val response: JsAny?
    open val responseText: String
    open val responseXML: Document?
    fun open(method: String, url: String)
    fun open(method: String, url: String, async: Boolean, username: String? = definedExternally, password: String? = definedExternally)
    fun setRequestHeader(name: String, value: String)
    fun send(body: JsAny? = definedExternally)
    fun abort()
    fun getResponseHeader(name: String): String?
    fun getAllResponseHeaders(): String
    fun overrideMimeType(mime: String)

    companion object {
        val UNSENT: Short
        val OPENED: Short
        val HEADERS_RECEIVED: Short
        val LOADING: Short
        val DONE: Short
    }
}

/**
 * Exposes the JavaScript [FormData](https://developer.mozilla.org/en/docs/Web/API/FormData) to Kotlin
 */
public external open class FormData(form: HTMLFormElement = definedExternally) : JsAny {
    fun append(name: String, value: String)
    fun append(name: String, value: Blob, filename: String = definedExternally)
    fun delete(name: String)
    fun get(name: String): JsAny?
    fun getAll(name: String): JsArray<JsAny?>
    fun has(name: String): Boolean
    fun set(name: String, value: String)
    fun set(name: String, value: Blob, filename: String = definedExternally)
}

/**
 * Exposes the JavaScript [ProgressEvent](https://developer.mozilla.org/en/docs/Web/API/ProgressEvent) to Kotlin
 */
public external open class ProgressEvent(type: String, eventInitDict: ProgressEventInit = definedExternally) : Event, JsAny {
    open val lengthComputable: Boolean
    open val loaded: JsNumber
    open val total: JsNumber

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface ProgressEventInit : EventInit, JsAny {
    var lengthComputable: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var loaded: JsNumber? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var total: JsNumber? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ProgressEventInit(lengthComputable: Boolean? = false, loaded: JsNumber? = 0.toJsNumber(), total: JsNumber? = 0.toJsNumber(), bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ProgressEventInit { js("return { lengthComputable, loaded, total, bubbles, cancelable, composed };") }

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface XMLHttpRequestResponseType : JsAny {
    companion object
}

public inline val XMLHttpRequestResponseType.Companion.EMPTY: XMLHttpRequestResponseType get() = "".toJsString().unsafeCast<XMLHttpRequestResponseType>()

public inline val XMLHttpRequestResponseType.Companion.ARRAYBUFFER: XMLHttpRequestResponseType get() = "arraybuffer".toJsString().unsafeCast<XMLHttpRequestResponseType>()

public inline val XMLHttpRequestResponseType.Companion.BLOB: XMLHttpRequestResponseType get() = "blob".toJsString().unsafeCast<XMLHttpRequestResponseType>()

public inline val XMLHttpRequestResponseType.Companion.DOCUMENT: XMLHttpRequestResponseType get() = "document".toJsString().unsafeCast<XMLHttpRequestResponseType>()

public inline val XMLHttpRequestResponseType.Companion.JSON: XMLHttpRequestResponseType get() = "json".toJsString().unsafeCast<XMLHttpRequestResponseType>()

public inline val XMLHttpRequestResponseType.Companion.TEXT: XMLHttpRequestResponseType get() = "text".toJsString().unsafeCast<XMLHttpRequestResponseType>()