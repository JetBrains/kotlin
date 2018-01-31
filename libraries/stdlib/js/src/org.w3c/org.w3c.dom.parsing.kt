/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.dom.parsing

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [DOMParser](https://developer.mozilla.org/en/docs/Web/API/DOMParser) to Kotlin
 */
public external open class DOMParser {
    fun parseFromString(str: String, type: dynamic): Document
}

/**
 * Exposes the JavaScript [XMLSerializer](https://developer.mozilla.org/en/docs/Web/API/XMLSerializer) to Kotlin
 */
public external open class XMLSerializer {
    fun serializeToString(root: Node): String
}

