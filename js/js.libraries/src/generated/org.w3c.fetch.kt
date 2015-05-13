/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.fetch

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public open class Headers(init: dynamic = noImpl) {
    fun append(name: String, value: String): Unit = noImpl
    fun delete(name: String): Unit = noImpl
    fun get(name: String): String? = noImpl
    fun getAll(name: String): Array<String> = noImpl
    fun has(name: String): Boolean = noImpl
    fun set(name: String, value: String): Unit = noImpl
}

native public open class Request(input: dynamic, init: RequestInit = noImpl) {
    var method: String
        get() = noImpl
        set(value) = noImpl
    var url: String
        get() = noImpl
        set(value) = noImpl
    var headers: Headers
        get() = noImpl
        set(value) = noImpl
    var context: String
        get() = noImpl
        set(value) = noImpl
    var referrer: String
        get() = noImpl
        set(value) = noImpl
    var mode: String
        get() = noImpl
        set(value) = noImpl
    var credentials: String
        get() = noImpl
        set(value) = noImpl
    var cache: String
        get() = noImpl
        set(value) = noImpl
    var redirect: String
        get() = noImpl
        set(value) = noImpl
    var bodyUsed: Boolean
        get() = noImpl
        set(value) = noImpl
    fun clone(): Request = noImpl
    fun arrayBuffer(): dynamic = noImpl
    fun blob(): dynamic = noImpl
    fun formData(): dynamic = noImpl
    fun json(): dynamic = noImpl
    fun text(): dynamic = noImpl
}

native public open class RequestInit {
    var method: String
    var headers: dynamic
    var body: dynamic
    var mode: String
    var credentials: String
    var cache: String
    var redirect: String
}

native public open class Response(body: dynamic = noImpl, init: ResponseInit = noImpl) {
    var type: String
        get() = noImpl
        set(value) = noImpl
    var url: String
        get() = noImpl
        set(value) = noImpl
    var status: Short
        get() = noImpl
        set(value) = noImpl
    var ok: Boolean
        get() = noImpl
        set(value) = noImpl
    var statusText: String
        get() = noImpl
        set(value) = noImpl
    var headers: Headers
        get() = noImpl
        set(value) = noImpl
    var bodyUsed: Boolean
        get() = noImpl
        set(value) = noImpl
    fun clone(): Response = noImpl
    fun arrayBuffer(): dynamic = noImpl
    fun blob(): dynamic = noImpl
    fun formData(): dynamic = noImpl
    fun json(): dynamic = noImpl
    fun text(): dynamic = noImpl
}

native public open class ResponseInit {
    var status: Short = 200
    var statusText: String = "OK"
    var headers: dynamic
}

