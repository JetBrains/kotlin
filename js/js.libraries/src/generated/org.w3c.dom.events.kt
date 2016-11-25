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
        get() = noImpl
    open val detail: Int
        get() = noImpl
}

public external interface UIEventInit : EventInit {
    var view: Window? /* = null */
    var detail: Int? /* = 0 */
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
        get() = noImpl
}

public external interface FocusEventInit : UIEventInit {
    var relatedTarget: EventTarget? /* = null */
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
        get() = noImpl
    open val screenX: Int
        get() = noImpl
    open val screenY: Int
        get() = noImpl
    open val pageX: Double
        get() = noImpl
    open val pageY: Double
        get() = noImpl
    open val clientX: Int
        get() = noImpl
    open val clientY: Int
        get() = noImpl
    open val offsetX: Double
        get() = noImpl
    open val offsetY: Double
        get() = noImpl
    open val ctrlKey: Boolean
        get() = noImpl
    open val shiftKey: Boolean
        get() = noImpl
    open val altKey: Boolean
        get() = noImpl
    open val metaKey: Boolean
        get() = noImpl
    open val button: Short
        get() = noImpl
    open val buttons: Short
        get() = noImpl
    open val relatedTarget: EventTarget?
        get() = noImpl
    fun getModifierState(keyArg: String): Boolean = noImpl
}

public external interface MouseEventInit : EventModifierInit {
    var screenX: Int? /* = 0 */
    var screenY: Int? /* = 0 */
    var clientX: Int? /* = 0 */
    var clientY: Int? /* = 0 */
    var button: Short? /* = 0 */
    var buttons: Short? /* = 0 */
    var relatedTarget: EventTarget? /* = null */
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
    var shiftKey: Boolean? /* = false */
    var altKey: Boolean? /* = false */
    var metaKey: Boolean? /* = false */
    var modifierAltGraph: Boolean? /* = false */
    var modifierCapsLock: Boolean? /* = false */
    var modifierFn: Boolean? /* = false */
    var modifierFnLock: Boolean? /* = false */
    var modifierHyper: Boolean? /* = false */
    var modifierNumLock: Boolean? /* = false */
    var modifierScrollLock: Boolean? /* = false */
    var modifierSuper: Boolean? /* = false */
    var modifierSymbol: Boolean? /* = false */
    var modifierSymbolLock: Boolean? /* = false */
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
        get() = noImpl
    open val deltaY: Double
        get() = noImpl
    open val deltaZ: Double
        get() = noImpl
    open val deltaMode: Int
        get() = noImpl

    companion object {
        val DOM_DELTA_PIXEL: Int = 0x00
        val DOM_DELTA_LINE: Int = 0x01
        val DOM_DELTA_PAGE: Int = 0x02
    }
}

public external interface WheelEventInit : MouseEventInit {
    var deltaX: Double? /* = 0.0 */
    var deltaY: Double? /* = 0.0 */
    var deltaZ: Double? /* = 0.0 */
    var deltaMode: Int? /* = 0 */
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
        get() = noImpl
    open val isComposing: Boolean
        get() = noImpl
}

public external interface InputEventInit : UIEventInit {
    var data: String? /* = "" */
    var isComposing: Boolean? /* = false */
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
        get() = noImpl
    open val code: String
        get() = noImpl
    open val location: Int
        get() = noImpl
    open val ctrlKey: Boolean
        get() = noImpl
    open val shiftKey: Boolean
        get() = noImpl
    open val altKey: Boolean
        get() = noImpl
    open val metaKey: Boolean
        get() = noImpl
    open val repeat: Boolean
        get() = noImpl
    open val isComposing: Boolean
        get() = noImpl
    open val charCode: Int
        get() = noImpl
    open val keyCode: Int
        get() = noImpl
    open val which: Int
        get() = noImpl
    fun getModifierState(keyArg: String): Boolean = noImpl

    companion object {
        val DOM_KEY_LOCATION_STANDARD: Int = 0x00
        val DOM_KEY_LOCATION_LEFT: Int = 0x01
        val DOM_KEY_LOCATION_RIGHT: Int = 0x02
        val DOM_KEY_LOCATION_NUMPAD: Int = 0x03
    }
}

public external interface KeyboardEventInit : EventModifierInit {
    var key: String? /* = "" */
    var code: String? /* = "" */
    var location: Int? /* = 0 */
    var repeat: Boolean? /* = false */
    var isComposing: Boolean? /* = false */
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
        get() = noImpl
}

public external interface CompositionEventInit : UIEventInit {
    var data: String? /* = "" */
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
        get() = noImpl
    open val target: EventTarget?
        get() = noImpl
    open val currentTarget: EventTarget?
        get() = noImpl
    open val eventPhase: Short
        get() = noImpl
    open val bubbles: Boolean
        get() = noImpl
    open val cancelable: Boolean
        get() = noImpl
    open val defaultPrevented: Boolean
        get() = noImpl
    open val composed: Boolean
        get() = noImpl
    open val isTrusted: Boolean
        get() = noImpl
    open val timeStamp: Number
        get() = noImpl
    fun composedPath(): Array<EventTarget> = noImpl
    fun stopPropagation(): Unit = noImpl
    fun stopImmediatePropagation(): Unit = noImpl
    fun preventDefault(): Unit = noImpl
    fun initEvent(type: String, bubbles: Boolean, cancelable: Boolean): Unit = noImpl

    companion object {
        val NONE: Short = 0
        val CAPTURING_PHASE: Short = 1
        val AT_TARGET: Short = 2
        val BUBBLING_PHASE: Short = 3
    }
}

public external abstract class EventTarget {
    fun addEventListener(type: String, callback: EventListener?, options: dynamic = noImpl): Unit = noImpl
    fun addEventListener(type: String, callback: ((Event) -> Unit)?, options: dynamic = noImpl): Unit = noImpl
    fun removeEventListener(type: String, callback: EventListener?, options: dynamic = noImpl): Unit = noImpl
    fun removeEventListener(type: String, callback: ((Event) -> Unit)?, options: dynamic = noImpl): Unit = noImpl
    fun dispatchEvent(event: Event): Boolean = noImpl
}

public external interface EventListener {
    fun handleEvent(event: Event): Unit = noImpl
}

