/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom.url

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
import org.w3c.xhr.*

@native public open class URL(url: String, base: String = noImpl) {
    var href: String
        get() = noImpl
        set(value) = noImpl
    open val origin: String
        get() = noImpl
    var protocol: String
        get() = noImpl
        set(value) = noImpl
    var username: String
        get() = noImpl
        set(value) = noImpl
    var password: String
        get() = noImpl
        set(value) = noImpl
    var host: String
        get() = noImpl
        set(value) = noImpl
    var hostname: String
        get() = noImpl
        set(value) = noImpl
    var port: String
        get() = noImpl
        set(value) = noImpl
    var pathname: String
        get() = noImpl
        set(value) = noImpl
    var search: String
        get() = noImpl
        set(value) = noImpl
    open val searchParams: URLSearchParams
        get() = noImpl
    var hash: String
        get() = noImpl
        set(value) = noImpl

    companion object {
        fun createObjectURL(blob: Blob): String = noImpl
        fun createFor(blob: Blob): String = noImpl
        fun revokeObjectURL(url: String): Unit = noImpl
        fun domainToASCII(domain: String): String = noImpl
        fun domainToUnicode(domain: String): String = noImpl
    }
}

@native public open class URLSearchParams(init: dynamic = "") {
    fun append(name: String, value: String): Unit = noImpl
    fun delete(name: String): Unit = noImpl
    fun get(name: String): String? = noImpl
    fun getAll(name: String): Array<String> = noImpl
    fun has(name: String): Boolean = noImpl
    fun set(name: String, value: String): Unit = noImpl
}

