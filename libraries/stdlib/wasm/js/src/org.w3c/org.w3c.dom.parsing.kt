/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.parsing

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*

/**
 * Exposes the JavaScript [DOMParser](https://developer.mozilla.org/en/docs/Web/API/DOMParser) to Kotlin
 */
public external open class DOMParser : JsAny {
    fun parseFromString(str: String, type: JsAny?): Document
}

/**
 * Exposes the JavaScript [XMLSerializer](https://developer.mozilla.org/en/docs/Web/API/XMLSerializer) to Kotlin
 */
public external open class XMLSerializer : JsAny {
    fun serializeToString(root: Node): String
}