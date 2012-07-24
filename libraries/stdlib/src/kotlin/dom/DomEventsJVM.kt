package kotlin.dom

import org.w3c.dom.Node
import org.w3c.dom.events.*

// JavaScript style properties for JVM : TODO could auto-generate these
val Event.bubbles: Boolean
    get() = getBubbles()

val Event.cancelable: Boolean
    get() = getCancelable()

val Event.getCurrentTarget: EventTarget?
    get() = getCurrentTarget()

val Event.eventPhase: Short
    get() = getEventPhase()

val Event.target: EventTarget?
    get() = getTarget()

val Event.timeStamp: Long
    get() = getTimeStamp()

// TODO we can't use 'type' as the property name in Kotlin so we should fix it in JS
val Event.eventType: String
    get() = getType()!!


val MouseEvent.altKey: Boolean
    get() = getAltKey()

val MouseEvent.button: Short
    get() = getButton()

val MouseEvent.clientX: Int
    get() = getClientX()

val MouseEvent.clientY: Int
    get() = getClientY()

val MouseEvent.ctrlKey: Boolean
    get() = getCtrlKey()

val MouseEvent.metaKey: Boolean
    get() = getMetaKey()

val MouseEvent.relatedTarget: EventTarget?
    get() = getRelatedTarget()

val MouseEvent.screenX: Int
    get() = getScreenX()

val MouseEvent.screenY: Int
    get() = getScreenY()

val MouseEvent.shiftKey: Boolean
    get() = getShiftKey()
