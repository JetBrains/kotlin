/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.xhr

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*

@native public interface XMLHttpRequestEventTarget : EventTarget {
    var onloadstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onabort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var ontimeout: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
}

@native public interface XMLHttpRequestUpload : XMLHttpRequestEventTarget {
}

@native public open class XMLHttpRequest : XMLHttpRequestEventTarget {
    var onreadystatechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open val readyState: Short
        get() = noImpl
    var timeout: Int
        get() = noImpl
        set(value) = noImpl
    var withCredentials: Boolean
        get() = noImpl
        set(value) = noImpl
    open val upload: XMLHttpRequestUpload
        get() = noImpl
    open val responseURL: String
        get() = noImpl
    open val status: Short
        get() = noImpl
    open val statusText: String
        get() = noImpl
    var responseType: String
        get() = noImpl
        set(value) = noImpl
    open val response: Any?
        get() = noImpl
    open val responseText: String
        get() = noImpl
    open val responseXML: Document?
        get() = noImpl
    fun open(method: String, url: String): Unit = noImpl
    fun open(method: String, url: String, async: Boolean, username: String? = null, password: String? = null): Unit = noImpl
    fun setRequestHeader(name: String, value: String): Unit = noImpl
    fun send(body: dynamic = null): Unit = noImpl
    fun abort(): Unit = noImpl
    fun getResponseHeader(name: String): String? = noImpl
    fun getAllResponseHeaders(): String = noImpl
    fun overrideMimeType(mime: String): Unit = noImpl

    companion object {
        val UNSENT: Short = 0
        val OPENED: Short = 1
        val HEADERS_RECEIVED: Short = 2
        val LOADING: Short = 3
        val DONE: Short = 4
    }
}

@native public open class FormData(form: HTMLFormElement = noImpl) {
    fun append(name: String, value: dynamic): Unit = noImpl
    fun delete(name: String): Unit = noImpl
    fun get(name: String): dynamic = noImpl
    fun getAll(name: String): Array<dynamic> = noImpl
    fun has(name: String): Boolean = noImpl
    fun set(name: String, value: dynamic): Unit = noImpl
}

@native public open class ProgressEvent(type: String, eventInitDict: ProgressEventInit = noImpl) : Event(type, eventInitDict) {
    open val lengthComputable: Boolean
        get() = noImpl
    open val loaded: Int
        get() = noImpl
    open val total: Int
        get() = noImpl
}

@native public interface ProgressEventInit : EventInit {
    var lengthComputable: Boolean
    var loaded: Int
    var total: Int
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ProgressEventInit(lengthComputable: Boolean = false, loaded: Int = 0, total: Int = 0, bubbles: Boolean = false, cancelable: Boolean = false): ProgressEventInit {
    val o = js("({})")

    o["lengthComputable"] = lengthComputable
    o["loaded"] = loaded
    o["total"] = total
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

