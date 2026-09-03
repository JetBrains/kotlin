/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

@file:Suppress(
    "NO_EXPLICIT_VISIBILITY_IN_API_MODE",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE",
    "DEPRECATION"
) // TODO: Fix in dukat: https://github.com/Kotlin/dukat/issues/124

package org.w3c.dom.clipboard

import kotlinx.browser.PLEASE_USE_KOTLINX_BROWSER_INSTEAD
import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import kotlin.internal.InlineOnly

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface ClipboardEventInit : EventInit {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var clipboardData: DataTransfer?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun ClipboardEventInit(
    clipboardData: DataTransfer? = null,
    bubbles: Boolean? = false,
    cancelable: Boolean? = false,
    composed: Boolean? = false
): ClipboardEventInit {
    val o = js("({})")
    o["clipboardData"] = clipboardData
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed
    return o
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public open external class ClipboardEvent(type: String, eventInitDict: ClipboardEventInit = definedExternally) : Event {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    open val clipboardData: DataTransfer?

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    companion object {
        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val NONE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val CAPTURING_PHASE: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val AT_TARGET: Short

        @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
        val BUBBLING_PHASE: Short
    }
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public abstract external class Clipboard : EventTarget {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun read(): Promise<DataTransfer>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun readText(): Promise<String>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun write(data: DataTransfer): Promise<Unit>

    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    fun writeText(data: String): Promise<Unit>
}

@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public external interface ClipboardPermissionDescriptor {
    @Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
    var allowWithoutGesture: Boolean?
        get() = definedExternally
        set(value) = definedExternally
}

@InlineOnly
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@Deprecated(message = PLEASE_USE_KOTLINX_BROWSER_INSTEAD, level = DeprecationLevel.WARNING)
public inline fun ClipboardPermissionDescriptor(allowWithoutGesture: Boolean? = false): ClipboardPermissionDescriptor {
    val o = js("({})")
    o["allowWithoutGesture"] = allowWithoutGesture
    return o
}
