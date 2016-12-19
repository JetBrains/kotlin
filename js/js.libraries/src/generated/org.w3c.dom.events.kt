/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.dom.events

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external open class UIEvent(type: String, eventInitDict: UIEventInit = noImpl) : Event(type, eventInitDict) {
    open val view: Window?
    open val detail: Int
}

public external interface UIEventInit : EventInit {
    var view: Window? /* = null */
        get() = noImpl
        set(value) = noImpl
    var detail: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun UIEventInit(view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): UIEventInit {
    val o = js("({})")

    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class FocusEvent(type: String, eventInitDict: FocusEventInit = noImpl) : UIEvent(type, eventInitDict) {
    open val relatedTarget: EventTarget?
}

public external interface FocusEventInit : UIEventInit {
    var relatedTarget: EventTarget? /* = null */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun FocusEventInit(relatedTarget: EventTarget? = null, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): FocusEventInit {
    val o = js("({})")

    o["relatedTarget"] = relatedTarget
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class MouseEvent(type: String, eventInitDict: MouseEventInit = noImpl) : UIEvent(type, eventInitDict), UnionElementOrMouseEvent {
    open val region: String?
    open val screenX: Int
    open val screenY: Int
    open val pageX: Double
    open val pageY: Double
    open val clientX: Int
    open val clientY: Int
    open val offsetX: Double
    open val offsetY: Double
    open val ctrlKey: Boolean
    open val shiftKey: Boolean
    open val altKey: Boolean
    open val metaKey: Boolean
    open val button: Short
    open val buttons: Short
    open val relatedTarget: EventTarget?
    fun getModifierState(keyArg: String): Boolean
}

public external interface MouseEventInit : EventModifierInit {
    var screenX: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var screenY: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var clientX: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var clientY: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var button: Short? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var buttons: Short? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var relatedTarget: EventTarget? /* = null */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun MouseEventInit(screenX: Int? = 0, screenY: Int? = 0, clientX: Int? = 0, clientY: Int? = 0, button: Short? = 0, buttons: Short? = 0, relatedTarget: EventTarget? = null, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): MouseEventInit {
    val o = js("({})")

    o["screenX"] = screenX
    o["screenY"] = screenY
    o["clientX"] = clientX
    o["clientY"] = clientY
    o["button"] = button
    o["buttons"] = buttons
    o["relatedTarget"] = relatedTarget
    o["ctrlKey"] = ctrlKey
    o["shiftKey"] = shiftKey
    o["altKey"] = altKey
    o["metaKey"] = metaKey
    o["modifierAltGraph"] = modifierAltGraph
    o["modifierCapsLock"] = modifierCapsLock
    o["modifierFn"] = modifierFn
    o["modifierFnLock"] = modifierFnLock
    o["modifierHyper"] = modifierHyper
    o["modifierNumLock"] = modifierNumLock
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external interface EventModifierInit : UIEventInit {
    var ctrlKey: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var shiftKey: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var altKey: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var metaKey: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierAltGraph: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierCapsLock: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierFn: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierFnLock: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierHyper: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierNumLock: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierScrollLock: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierSuper: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierSymbol: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var modifierSymbolLock: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun EventModifierInit(ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): EventModifierInit {
    val o = js("({})")

    o["ctrlKey"] = ctrlKey
    o["shiftKey"] = shiftKey
    o["altKey"] = altKey
    o["metaKey"] = metaKey
    o["modifierAltGraph"] = modifierAltGraph
    o["modifierCapsLock"] = modifierCapsLock
    o["modifierFn"] = modifierFn
    o["modifierFnLock"] = modifierFnLock
    o["modifierHyper"] = modifierHyper
    o["modifierNumLock"] = modifierNumLock
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class WheelEvent(type: String, eventInitDict: WheelEventInit = noImpl) : MouseEvent(type, eventInitDict) {
    open val deltaX: Double
    open val deltaY: Double
    open val deltaZ: Double
    open val deltaMode: Int

    companion object {
        val DOM_DELTA_PIXEL: Int
        val DOM_DELTA_LINE: Int
        val DOM_DELTA_PAGE: Int
    }
}

public external interface WheelEventInit : MouseEventInit {
    var deltaX: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var deltaY: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var deltaZ: Double? /* = 0.0 */
        get() = noImpl
        set(value) = noImpl
    var deltaMode: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun WheelEventInit(deltaX: Double? = 0.0, deltaY: Double? = 0.0, deltaZ: Double? = 0.0, deltaMode: Int? = 0, screenX: Int? = 0, screenY: Int? = 0, clientX: Int? = 0, clientY: Int? = 0, button: Short? = 0, buttons: Short? = 0, relatedTarget: EventTarget? = null, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): WheelEventInit {
    val o = js("({})")

    o["deltaX"] = deltaX
    o["deltaY"] = deltaY
    o["deltaZ"] = deltaZ
    o["deltaMode"] = deltaMode
    o["screenX"] = screenX
    o["screenY"] = screenY
    o["clientX"] = clientX
    o["clientY"] = clientY
    o["button"] = button
    o["buttons"] = buttons
    o["relatedTarget"] = relatedTarget
    o["ctrlKey"] = ctrlKey
    o["shiftKey"] = shiftKey
    o["altKey"] = altKey
    o["metaKey"] = metaKey
    o["modifierAltGraph"] = modifierAltGraph
    o["modifierCapsLock"] = modifierCapsLock
    o["modifierFn"] = modifierFn
    o["modifierFnLock"] = modifierFnLock
    o["modifierHyper"] = modifierHyper
    o["modifierNumLock"] = modifierNumLock
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class InputEvent(type: String, eventInitDict: InputEventInit = noImpl) : UIEvent(type, eventInitDict) {
    open val data: String
    open val isComposing: Boolean
}

public external interface InputEventInit : UIEventInit {
    var data: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var isComposing: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun InputEventInit(data: String? = "", isComposing: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): InputEventInit {
    val o = js("({})")

    o["data"] = data
    o["isComposing"] = isComposing
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class KeyboardEvent(type: String, eventInitDict: KeyboardEventInit = noImpl) : UIEvent(type, eventInitDict) {
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
    }
}

public external interface KeyboardEventInit : EventModifierInit {
    var key: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var code: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var location: Int? /* = 0 */
        get() = noImpl
        set(value) = noImpl
    var repeat: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var isComposing: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun KeyboardEventInit(key: String? = "", code: String? = "", location: Int? = 0, repeat: Boolean? = false, isComposing: Boolean? = false, ctrlKey: Boolean? = false, shiftKey: Boolean? = false, altKey: Boolean? = false, metaKey: Boolean? = false, modifierAltGraph: Boolean? = false, modifierCapsLock: Boolean? = false, modifierFn: Boolean? = false, modifierFnLock: Boolean? = false, modifierHyper: Boolean? = false, modifierNumLock: Boolean? = false, modifierScrollLock: Boolean? = false, modifierSuper: Boolean? = false, modifierSymbol: Boolean? = false, modifierSymbolLock: Boolean? = false, view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): KeyboardEventInit {
    val o = js("({})")

    o["key"] = key
    o["code"] = code
    o["location"] = location
    o["repeat"] = repeat
    o["isComposing"] = isComposing
    o["ctrlKey"] = ctrlKey
    o["shiftKey"] = shiftKey
    o["altKey"] = altKey
    o["metaKey"] = metaKey
    o["modifierAltGraph"] = modifierAltGraph
    o["modifierCapsLock"] = modifierCapsLock
    o["modifierFn"] = modifierFn
    o["modifierFnLock"] = modifierFnLock
    o["modifierHyper"] = modifierHyper
    o["modifierNumLock"] = modifierNumLock
    o["modifierScrollLock"] = modifierScrollLock
    o["modifierSuper"] = modifierSuper
    o["modifierSymbol"] = modifierSymbol
    o["modifierSymbolLock"] = modifierSymbolLock
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class CompositionEvent(type: String, eventInitDict: CompositionEventInit = noImpl) : UIEvent(type, eventInitDict) {
    open val data: String
}

public external interface CompositionEventInit : UIEventInit {
    var data: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CompositionEventInit(data: String? = "", view: Window? = null, detail: Int? = 0, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): CompositionEventInit {
    val o = js("({})")

    o["data"] = data
    o["view"] = view
    o["detail"] = detail
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class Event(type: String, eventInitDict: EventInit = noImpl) {
    open val type: String
    open val target: EventTarget?
    open val currentTarget: EventTarget?
    open val eventPhase: Short
    open val bubbles: Boolean
    open val cancelable: Boolean
    open val defaultPrevented: Boolean
    open val composed: Boolean
    open val isTrusted: Boolean
    open val timeStamp: Number
    fun composedPath(): Array<EventTarget>
    fun stopPropagation(): Unit
    fun stopImmediatePropagation(): Unit
    fun preventDefault(): Unit
    fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean): Unit

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external abstract class EventTarget {
    fun addEventListener(type: String, callback: EventListener?, options: dynamic = noImpl): Unit
    fun addEventListener(type: String, callback: ((Event) -> Unit)?, options: dynamic = noImpl): Unit
    fun removeEventListener(type: String, callback: EventListener?, options: dynamic = noImpl): Unit
    fun removeEventListener(type: String, callback: ((Event) -> Unit)?, options: dynamic = noImpl): Unit
    fun dispatchEvent(event: Event): Boolean
}

public external interface EventListener {
    fun handleEvent(event: Event): Unit
}

