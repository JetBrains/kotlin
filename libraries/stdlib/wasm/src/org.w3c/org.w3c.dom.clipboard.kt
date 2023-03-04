/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.clipboard

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*

public external interface ClipboardEventInit : EventInit, JsAny {
    var clipboardData: DataTransfer? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ClipboardEventInit(clipboardData: DataTransfer? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ClipboardEventInit { js("return { clipboardData, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [ClipboardEvent](https://developer.mozilla.org/en/docs/Web/API/ClipboardEvent) to Kotlin
 */
public external open class ClipboardEvent(type: String, eventInitDict: ClipboardEventInit = definedExternally) : Event, JsAny {
    open val clipboardData: DataTransfer?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

/**
 * Exposes the JavaScript [Clipboard](https://developer.mozilla.org/en/docs/Web/API/Clipboard) to Kotlin
 */
public external abstract class Clipboard : EventTarget, JsAny {
    fun read(): Promise<DataTransfer>
    fun readText(): Promise<JsString>
    fun write(data: DataTransfer): Promise<Nothing?>
    fun writeText(data: String): Promise<Nothing?>
}

public external interface ClipboardPermissionDescriptor : JsAny {
    var allowWithoutGesture: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun ClipboardPermissionDescriptor(allowWithoutGesture: Boolean? = false): ClipboardPermissionDescriptor { js("return { allowWithoutGesture };") }