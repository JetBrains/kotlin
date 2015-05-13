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
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*

native public trait XMLHttpRequestEventTarget : EventTarget {
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

native public trait XMLHttpRequestUpload : XMLHttpRequestEventTarget {
}

native public open class XMLHttpRequest : XMLHttpRequestEventTarget {
    var onreadystatechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var timeout: Int
        get() = noImpl
        set(value) = noImpl
    var withCredentials: Boolean
        get() = noImpl
        set(value) = noImpl
    var upload: XMLHttpRequestUpload
        get() = noImpl
        set(value) = noImpl
    var responseURL: String
        get() = noImpl
        set(value) = noImpl
    var status: Short
        get() = noImpl
        set(value) = noImpl
    var statusText: String
        get() = noImpl
        set(value) = noImpl
    var responseType: String
        get() = noImpl
        set(value) = noImpl
    var response: Any?
        get() = noImpl
        set(value) = noImpl
    var responseText: String
        get() = noImpl
        set(value) = noImpl
    var responseXML: Document?
        get() = noImpl
        set(value) = noImpl
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

native public open class FormData(form: HTMLFormElement = noImpl) {
    fun append(name: String, value: dynamic): Unit = noImpl
    fun delete(name: String): Unit = noImpl
    fun get(name: String): dynamic = noImpl
    fun getAll(name: String): Array<dynamic> = noImpl
    fun has(name: String): Boolean = noImpl
    fun set(name: String, value: dynamic): Unit = noImpl
}

native public open class ProgressEvent(type: String, eventInitDict: ProgressEventInit = noImpl) : Event(type, eventInitDict) {
    var lengthComputable: Boolean
        get() = noImpl
        set(value) = noImpl
    var loaded: Long
        get() = noImpl
        set(value) = noImpl
    var total: Long
        get() = noImpl
        set(value) = noImpl
}

native public open class ProgressEventInit : EventInit() {
    var lengthComputable: Boolean = false
    var loaded: Long = 0
    var total: Long = 0
}

