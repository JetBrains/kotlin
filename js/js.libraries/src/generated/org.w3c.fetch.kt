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

public external open class Headers(init: dynamic = definedExternally) {
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

public external open class Request(input: dynamic, init: RequestInit = definedExternally) : Body {
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
        get() = definedExternally
        set(value) = definedExternally
    var headers: dynamic
        get() = definedExternally
        set(value) = definedExternally
    var body: dynamic
        get() = definedExternally
        set(value) = definedExternally
    var referrer: String?
        get() = definedExternally
        set(value) = definedExternally
    var referrerPolicy: dynamic
        get() = definedExternally
        set(value) = definedExternally
    var mode: String?
        get() = definedExternally
        set(value) = definedExternally
    var credentials: String?
        get() = definedExternally
        set(value) = definedExternally
    var cache: String?
        get() = definedExternally
        set(value) = definedExternally
    var redirect: String?
        get() = definedExternally
        set(value) = definedExternally
    var integrity: String?
        get() = definedExternally
        set(value) = definedExternally
    var keepalive: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var window: Any?
        get() = definedExternally
        set(value) = definedExternally
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

public external open class Response(body: dynamic = definedExternally, init: ResponseInit = definedExternally) : Body {
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
        fun redirect(url: String, status: Short = definedExternally): Response
    }
}

public external interface ResponseInit {
    var status: Short? /* = 200 */
        get() = definedExternally
        set(value) = definedExternally
    var statusText: String? /* = "OK" */
        get() = definedExternally
        set(value) = definedExternally
    var headers: dynamic
        get() = definedExternally
        set(value) = definedExternally
}

public inline fun ResponseInit(status: Short? = 200, statusText: String? = "OK", headers: dynamic = null): ResponseInit {
    val o = js("({})")

    o["status"] = status
    o["statusText"] = statusText
    o["headers"] = headers

    return o
}

