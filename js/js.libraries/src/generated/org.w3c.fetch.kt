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
import org.w3c.dom.url.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external open class Headers(init: dynamic = noImpl) {
    fun append(name: String, value: String): Unit
    fun delete(name: String): Unit
    fun get(name: String): String?
    fun has(name: String): Boolean
    fun set(name: String, value: String): Unit
}

public external interface Body {
    val bodyUsed: Boolean
    fun arrayBuffer(): dynamic
    fun blob(): dynamic
    fun formData(): dynamic
    fun json(): dynamic
    fun text(): dynamic
}

public external open class Request(input: dynamic, init: RequestInit = noImpl) : Body {
    open val method: String
    open val url: String
    open val headers: Headers
    open val type: String
    open val destination: String
    open val referrer: String
    open val referrerPolicy: dynamic
    open val mode: String
    open val credentials: String
    open val cache: String
    open val redirect: String
    open val integrity: String
    open val keepalive: Boolean
    override val bodyUsed: Boolean
    fun clone(): Request
    override fun arrayBuffer(): dynamic
    override fun blob(): dynamic
    override fun formData(): dynamic
    override fun json(): dynamic
    override fun text(): dynamic
}

public external interface RequestInit {
    var method: String?
        get() = noImpl
        set(value) = noImpl
    var headers: dynamic
        get() = noImpl
        set(value) = noImpl
    var body: dynamic
        get() = noImpl
        set(value) = noImpl
    var referrer: String?
        get() = noImpl
        set(value) = noImpl
    var referrerPolicy: dynamic
        get() = noImpl
        set(value) = noImpl
    var mode: String?
        get() = noImpl
        set(value) = noImpl
    var credentials: String?
        get() = noImpl
        set(value) = noImpl
    var cache: String?
        get() = noImpl
        set(value) = noImpl
    var redirect: String?
        get() = noImpl
        set(value) = noImpl
    var integrity: String?
        get() = noImpl
        set(value) = noImpl
    var keepalive: Boolean?
        get() = noImpl
        set(value) = noImpl
    var window: Any?
        get() = noImpl
        set(value) = noImpl
}

public inline fun RequestInit(method: String? = null, headers: dynamic = null, body: dynamic = null, referrer: String? = null, referrerPolicy: dynamic = null, mode: String? = null, credentials: String? = null, cache: String? = null, redirect: String? = null, integrity: String? = null, keepalive: Boolean? = null, window: Any? = null): RequestInit {
    val o = js("({})")

    o["method"] = method
    o["headers"] = headers
    o["body"] = body
    o["referrer"] = referrer
    o["referrerPolicy"] = referrerPolicy
    o["mode"] = mode
    o["credentials"] = credentials
    o["cache"] = cache
    o["redirect"] = redirect
    o["integrity"] = integrity
    o["keepalive"] = keepalive
    o["window"] = window

    return o
}

public external open class Response(body: dynamic = noImpl, init: ResponseInit = noImpl) : Body {
    open val type: String
    open val url: String
    open val redirected: Boolean
    open val status: Short
    open val ok: Boolean
    open val statusText: String
    open val headers: Headers
    open val body: dynamic
    open val trailer: dynamic
    override val bodyUsed: Boolean
    fun clone(): Response
    override fun arrayBuffer(): dynamic
    override fun blob(): dynamic
    override fun formData(): dynamic
    override fun json(): dynamic
    override fun text(): dynamic

    companion object {
        fun error(): Response
        fun redirect(url: String, status: Short = noImpl): Response
    }
}

public external interface ResponseInit {
    var status: Short? /* = 200 */
        get() = noImpl
        set(value) = noImpl
    var statusText: String? /* = "OK" */
        get() = noImpl
        set(value) = noImpl
    var headers: dynamic
        get() = noImpl
        set(value) = noImpl
}

public inline fun ResponseInit(status: Short? = 200, statusText: String? = "OK", headers: dynamic = null): ResponseInit {
    val o = js("({})")

    o["status"] = status
    o["statusText"] = statusText
    o["headers"] = headers

    return o
}

