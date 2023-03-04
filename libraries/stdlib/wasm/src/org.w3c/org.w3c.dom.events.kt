/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.dom.events

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*

/**
 * Exposes the JavaScript [UIEvent](https://developer.mozilla.org/en/docs/Web/API/UIEvent) to Kotlin
 */
public external open class UIEvent(type: String, eventInitDict: UIEventInit = definedExternally) : Event, JsAny {
    open val view: Window?
    open val detail: Int

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface UIEventInit : EventInit, JsAny {
    var view: Window? /* = null */
        get() = definedExternally
        set(value) = definedExternally
    var detail: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun UIEventInit(view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): UIEventInit { js("return { view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [FocusEvent](https://developer.mozilla.org/en/docs/Web/API/FocusEvent) to Kotlin
 */
public external open class FocusEvent(type: String, eventInitDict: FocusEventInit = definedExternally) : UIEvent, JsAny {
    open val relatedTarget: EventTarget?

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface FocusEventInit : UIEventInit, JsAny {
    var relatedTarget: EventTarget? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun FocusEventInit(relatedTarget: EventTarget? = null, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): FocusEventInit { js("return { relatedTarget, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [MouseEvent](https://developer.mozilla.org/en/docs/Web/API/MouseEvent) to Kotlin
 */
public external open class MouseEvent(type: String, eventInitDict: MouseEventInit = definedExternally) : UIEvent, UnionElementOrMouseEvent, JsAny {
    open val screenX: Int
    open val screenY: Int
    open val clientX: Int
    open val clientY: Int
    open val ctrlKey: Boolean
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val metaKey: Boolean
    open val button: Short
    open val buttons: Short
    open val relatedTarget: EventTarget?
    open val region: String?
    open val pageX: Double
    open val pageY: Double
    open val x: Double
    open val y: Double
    open val offsetX: Double
    open val offsetY: Double
    fun getModifierState(keyArg: String): Boolean

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface MouseEventInit : EventModifierInit, JsAny {
    var screenX: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var screenY: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var clientX: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var clientY: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var button: Short? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var buttons: Short? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var relatedTarget: EventTarget? /* = null */
        get() = definedExternally
        set(value) = definedExternally
    var region: String? /* = null */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun MouseEventInit(screenX: Int? = 0, screenY: Int? = 0, clientX: Int? = 0, clientY: Int? = 0, button: Short? = 0, buttons: Short? = 0, relatedTarget: EventTarget? = null, region: String? = null, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MouseEventInit { js("return { screenX, screenY, clientX, clientY, button, buttons, relatedTarget, region, ctrlKey, shiftKey, altKey, metaKey, modifierAltGraph, modifierCapsLock, modifierFn, modifierFnLock, modifierHyper, modifierNumLock, modifierScrollLock, modifierSuper, modifierSymbol, modifierSymbolLock, view, detail, bubbles, cancelable, composed };") }

public external interface EventModifierInit : UIEventInit, JsAny {
    var ctrlKey: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var shiftKey: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var altKey: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var metaKey: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierAltGraph: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierCapsLock: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierFn: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierFnLock: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierHyper: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierNumLock: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierScrollLock: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierSuper: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierSymbol: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var modifierSymbolLock: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun EventModifierInit(ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): EventModifierInit { js("return { ctrlKey, shiftKey, altKey, metaKey, modifierAltGraph, modifierCapsLock, modifierFn, modifierFnLock, modifierHyper, modifierNumLock, modifierScrollLock, modifierSuper, modifierSymbol, modifierSymbolLock, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [WheelEvent](https://developer.mozilla.org/en/docs/Web/API/WheelEvent) to Kotlin
 */
public external open class WheelEvent(type: String, eventInitDict: WheelEventInit = definedExternally) : MouseEvent, JsAny {
    open val deltaX: Double
    open val deltaY: Double
    open val deltaZ: Double
    open val deltaMode: Int

    companion object {
        val DOM_DELTA_PIXEL: Int
        val DOM_DELTA_LINE: Int
        val DOM_DELTA_PAGE: Int
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface WheelEventInit : MouseEventInit, JsAny {
    var deltaX: Double? /* = 0.0 */
        get() = definedExternally
        set(value) = definedExternally
    var deltaY: Double? /* = 0.0 */
        get() = definedExternally
        set(value) = definedExternally
    var deltaZ: Double? /* = 0.0 */
        get() = definedExternally
        set(value) = definedExternally
    var deltaMode: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun WheelEventInit(deltaX: Double? = 0.0, deltaY: Double? = 0.0, deltaZ: Double? = 0.0, deltaMode: Int? = 0, screenX: Int? = 0, screenY: Int? = 0, clientX: Int? = 0, clientY: Int? = 0, button: Short? = 0, buttons: Short? = 0, relatedTarget: EventTarget? = null, region: String? = null, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): WheelEventInit { js("return { deltaX, deltaY, deltaZ, deltaMode, screenX, screenY, clientX, clientY, button, buttons, relatedTarget, region, ctrlKey, shiftKey, altKey, metaKey, modifierAltGraph, modifierCapsLock, modifierFn, modifierFnLock, modifierHyper, modifierNumLock, modifierScrollLock, modifierSuper, modifierSymbol, modifierSymbolLock, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [InputEvent](https://developer.mozilla.org/en/docs/Web/API/InputEvent) to Kotlin
 */
public external open class InputEvent(type: String, eventInitDict: InputEventInit = definedExternally) : UIEvent, JsAny {
    open val data: String
    open val isComposing: Boolean

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface InputEventInit : UIEventInit, JsAny {
    var data: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var isComposing: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun InputEventInit(data: String? = "", isComposing: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): InputEventInit { js("return { data, isComposing, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [KeyboardEvent](https://developer.mozilla.org/en/docs/Web/API/KeyboardEvent) to Kotlin
 */
public external open class KeyboardEvent(type: String, eventInitDict: KeyboardEventInit = definedExternally) : UIEvent, JsAny {
    open val key: String
    open val code: String
    open val location: Int
    open val ctrlKey: Boolean
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val metaKey: Boolean
    open val repeat: Boolean
    open val isComposing: Boolean
    open val charCode: Int
    open val keyCode: Int
    open val which: Int
    fun getModifierState(keyArg: String): Boolean

    companion object {
        val DOM_KEY_LOCATION_STANDARD: Int
        val DOM_KEY_LOCATION_LEFT: Int
        val DOM_KEY_LOCATION_RIGHT: Int
        val DOM_KEY_LOCATION_NUMPAD: Int
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface KeyboardEventInit : EventModifierInit, JsAny {
    var key: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var code: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var location: Int? /* = 0 */
        get() = definedExternally
        set(value) = definedExternally
    var repeat: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var isComposing: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun KeyboardEventInit(key: String? = "", code: String? = "", location: Int? = 0, repeat: Boolean? = false, isComposing: Boolean? = false, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): KeyboardEventInit { js("return { key, code, location, repeat, isComposing, ctrlKey, shiftKey, altKey, metaKey, modifierAltGraph, modifierCapsLock, modifierFn, modifierFnLock, modifierHyper, modifierNumLock, modifierScrollLock, modifierSuper, modifierSymbol, modifierSymbolLock, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [CompositionEvent](https://developer.mozilla.org/en/docs/Web/API/CompositionEvent) to Kotlin
 */
public external open class CompositionEvent(type: String, eventInitDict: CompositionEventInit = definedExternally) : UIEvent, JsAny {
    open val data: String

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface CompositionEventInit : UIEventInit, JsAny {
    var data: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun CompositionEventInit(data: String? = "", view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): CompositionEventInit { js("return { data, view, detail, bubbles, cancelable, composed };") }

/**
 * Exposes the JavaScript [Event](https://developer.mozilla.org/en/docs/Web/API/Event) to Kotlin
 */
public external open class Event(type: String, eventInitDict: EventInit = definedExternally) : JsAny {
    open val type: String
    open val target: EventTarget?
    open val currentTarget: EventTarget?
    open val eventPhase: Short
    open val bubbles: Boolean
    open val cancelable: Boolean
    open val defaultPrevented: Boolean
    open val composed: Boolean
    open val isTrusted: Boolean
    open val timeStamp: JsNumber
    fun composedPath(): JsArray<EventTarget>
    fun stopPropagation()
    fun stopImmediatePropagation()
    fun preventDefault()
    fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean)

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

/**
 * Exposes the JavaScript [EventTarget](https://developer.mozilla.org/en/docs/Web/API/EventTarget) to Kotlin
 */
public external abstract class EventTarget : JsAny {
    fun addEventListener(type: String, callback: EventListener?, options: JsAny? = definedExternally)
    fun addEventListener(type: String, callback: ((Event) -> Unit)?, options: JsAny? = definedExternally)
    fun removeEventListener(type: String, callback: EventListener?, options: JsAny? = definedExternally)
    fun removeEventListener(type: String, callback: ((Event) -> Unit)?, options: JsAny? = definedExternally)
    fun dispatchEvent(event: Event): Boolean
}

/**
 * Exposes the JavaScript [EventListener](https://developer.mozilla.org/en/docs/Web/API/EventListener) to Kotlin
 */
public external interface EventListener : JsAny {
    fun handleEvent(event: Event)
}