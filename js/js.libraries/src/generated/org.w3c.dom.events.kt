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

native public open class MouseEvent(typeArg: String, mouseEventInitDict: MouseEventInit = noImpl) : UIEvent(noImpl, noImpl), UnionElementOrMouseEvent {
    open val screenX: Int
        get() = noImpl
    open val screenY: Int
        get() = noImpl
    open val clientX: Int
        get() = noImpl
    open val clientY: Int
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
    open val relatedTarget: EventTarget?
        get() = noImpl
    open val buttons: Short
        get() = noImpl
    open val region: String?
        get() = noImpl
//    open val screenX: Double
//        get() = noImpl
//    open val screenY: Double
//        get() = noImpl
    open val pageX: Double
        get() = noImpl
    open val pageY: Double
        get() = noImpl
//    open val clientX: Double
//        get() = noImpl
//    open val clientY: Double
//        get() = noImpl
//    open val x: Double
//        get() = noImpl
//    open val y: Double
//        get() = noImpl
    open val offsetX: Double
        get() = noImpl
    open val offsetY: Double
        get() = noImpl
    fun getModifierState(keyArg: String): Boolean = noImpl
    fun initMouseEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, detailArg: Int, screenXArg: Int, screenYArg: Int, clientXArg: Int, clientYArg: Int, ctrlKeyArg: Boolean, altKeyArg: Boolean, shiftKeyArg: Boolean, metaKeyArg: Boolean, buttonArg: Short, relatedTargetArg: EventTarget?): Unit = noImpl
}

native public open class Event(type: String, eventInitDict: EventInit = noImpl) {
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
    open val isTrusted: Boolean
        get() = noImpl
    open val timeStamp: Number
        get() = noImpl
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

native public interface EventTarget {
    fun addEventListener(type: String, callback: EventListener?, capture: Boolean = false): Unit = noImpl
    fun addEventListener(type: String, callback: ((Event) -> Unit)?, capture: Boolean = false): Unit = noImpl
    fun removeEventListener(type: String, callback: EventListener?, capture: Boolean = false): Unit = noImpl
    fun removeEventListener(type: String, callback: ((Event) -> Unit)?, capture: Boolean = false): Unit = noImpl
    fun dispatchEvent(event: Event): Boolean = noImpl
}

native public interface EventListener {
    fun handleEvent(event: Event): Unit = noImpl
}

native public open class UIEvent(type: String, eventInitDict: UIEventInit = noImpl) : Event(type, eventInitDict) {
    open val view: Window?
        get() = noImpl
    open val detail: Int
        get() = noImpl
    fun initUIEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, detailArg: Int): Unit = noImpl
}

native public open class UIEventInit : EventInit() {
    var view: Window? = null
    var detail: Int = 0
}

native public open class FocusEvent(typeArg: String, focusEventInitDict: FocusEventInit = noImpl) : UIEvent(noImpl, noImpl) {
    open val relatedTarget: EventTarget?
        get() = noImpl
    fun initFocusEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, detailArg: Int, relatedTargetArg: EventTarget?): Unit = noImpl
}

native public open class FocusEventInit : UIEventInit() {
    var relatedTarget: EventTarget? = null
}

native public open class MouseEventInit : EventModifierInit() {
    var screenX: Int = 0
    var screenY: Int = 0
    var clientX: Int = 0
    var clientY: Int = 0
    var button: Short = 0
    var buttons: Short = 0
    var relatedTarget: EventTarget? = null
}

native public open class EventModifierInit : UIEventInit() {
    var ctrlKey: Boolean = false
    var shiftKey: Boolean = false
    var altKey: Boolean = false
    var metaKey: Boolean = false
    var modifierAltGraph: Boolean = false
    var modifierCapsLock: Boolean = false
    var modifierFn: Boolean = false
    var modifierFnLock: Boolean = false
    var modifierHyper: Boolean = false
    var modifierNumLock: Boolean = false
    var modifierOS: Boolean = false
    var modifierScrollLock: Boolean = false
    var modifierSuper: Boolean = false
    var modifierSymbol: Boolean = false
    var modifierSymbolLock: Boolean = false
}

native public open class WheelEvent(typeArg: String, wheelEventInitDict: WheelEventInit = noImpl) : MouseEvent(typeArg, noImpl) {
    open val deltaX: Double
        get() = noImpl
    open val deltaY: Double
        get() = noImpl
    open val deltaZ: Double
        get() = noImpl
    open val deltaMode: Int
        get() = noImpl
    fun initWheelEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, detailArg: Int, screenXArg: Int, screenYArg: Int, clientXArg: Int, clientYArg: Int, buttonArg: Short, relatedTargetArg: EventTarget?, modifiersListArg: String, deltaXArg: Double, deltaYArg: Double, deltaZArg: Double, deltaMode: Int): Unit = noImpl

    companion object {
        val DOM_DELTA_PIXEL: Int = 0x00
        val DOM_DELTA_LINE: Int = 0x01
        val DOM_DELTA_PAGE: Int = 0x02
    }
}

native public open class WheelEventInit : MouseEventInit() {
    var deltaX: Double = 0.0
    var deltaY: Double = 0.0
    var deltaZ: Double = 0.0
    var deltaMode: Int = 0
}

native public open class KeyboardEvent(typeArg: String, keyboardEventInitDict: KeyboardEventInit = noImpl) : UIEvent(noImpl, noImpl) {
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
    fun initKeyboardEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, keyArg: String, locationArg: Int, modifiersListArg: String, repeat: Boolean, locale: String): Unit = noImpl

    companion object {
        val DOM_KEY_LOCATION_STANDARD: Int = 0x00
        val DOM_KEY_LOCATION_LEFT: Int = 0x01
        val DOM_KEY_LOCATION_RIGHT: Int = 0x02
        val DOM_KEY_LOCATION_NUMPAD: Int = 0x03
    }
}

native public open class KeyboardEventInit : EventModifierInit() {
    var key: String = ""
    var code: String = ""
    var location: Int = 0
    var repeat: Boolean = false
    var isComposing: Boolean = false
}

native public open class CompositionEvent(typeArg: String, compositionEventInitDict: CompositionEventInit = noImpl) : UIEvent(noImpl, noImpl) {
    open val data: String
        get() = noImpl
    fun initCompositionEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, viewArg: Window?, dataArg: String, locale: String): Unit = noImpl
}

native public open class CompositionEventInit : UIEventInit() {
    var data: String = ""
}

native public open class MutationEvent : Event(noImpl, noImpl) {
    open val relatedNode: Node?
        get() = noImpl
    open val prevValue: String
        get() = noImpl
    open val newValue: String
        get() = noImpl
    open val attrName: String
        get() = noImpl
    open val attrChange: Short
        get() = noImpl
    fun initMutationEvent(typeArg: String, bubblesArg: Boolean, cancelableArg: Boolean, relatedNodeArg: Node?, prevValueArg: String, newValueArg: String, attrNameArg: String, attrChangeArg: Short): Unit = noImpl

    companion object {
        val MODIFICATION: Short = 1
        val ADDITION: Short = 2
        val REMOVAL: Short = 3
    }
}

