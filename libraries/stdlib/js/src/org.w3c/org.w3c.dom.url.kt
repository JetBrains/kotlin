/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See libraries/tools/idl2k for details

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.dom.url

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.clipboard.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [URL](https://developer.mozilla.org/en/docs/Web/API/URL) to Kotlin
 */
public external open class URL(url: String, base: String = definedExternally) {
    var href: String
    open val origin: String
    var protocol: String
    var username: String
    var password: String
    var host: String
    var hostname: String
    var port: String
    var pathname: String
    var search: String
    open val searchParams: URLSearchParams
    var hash: String

    companion object {
        fun createObjectURL(blob: Blob): String
        fun createFor(blob: Blob): String
        fun revokeObjectURL(url: String): Unit
        fun domainToASCII(domain: String): String
        fun domainToUnicode(domain: String): String
    }
}

/**
 * Exposes the JavaScript [URLSearchParams](https://developer.mozilla.org/en/docs/Web/API/URLSearchParams) to Kotlin
 */
public external open class URLSearchParams(init: dynamic = definedExternally) {
    fun append(name: String, value: String): Unit
    fun delete(name: String): Unit
    fun get(name: String): String?
    fun getAll(name: String): Array<String>
    fun has(name: String): Boolean
    fun set(name: String, value: String): Unit
}

