/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See libraries/tools/idl2k for details

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.dom.clipboard

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external interface ClipboardEventInit : EventInit {
    var clipboardData: DataTransfer? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun ClipboardEventInit(clipboardData: DataTransfer? = null, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ClipboardEventInit {
    val o = js("({})")

    o["clipboardData"] = clipboardData
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

/**
 * Exposes the JavaScript [ClipboardEvent](https://developer.mozilla.org/en/docs/Web/API/ClipboardEvent) to Kotlin
 */
public external open class ClipboardEvent(type: String, eventInitDict: ClipboardEventInit = definedExternally) : Event {
    open val clipboardData: DataTransfer?
}

/**
 * Exposes the JavaScript [Clipboard](https://developer.mozilla.org/en/docs/Web/API/Clipboard) to Kotlin
 */
public external abstract class Clipboard : EventTarget {
    fun read(): Promise<DataTransfer>
    fun readText(): Promise<String>
    fun write(data: DataTransfer): Promise<Unit>
    fun writeText(data: String): Promise<Unit>
}

public external interface ClipboardPermissionDescriptor {
    var allowWithoutGesture: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun ClipboardPermissionDescriptor(allowWithoutGesture: Boolean? = false): ClipboardPermissionDescriptor {
    val o = js("({})")

    o["allowWithoutGesture"] = allowWithoutGesture

    return o
}

