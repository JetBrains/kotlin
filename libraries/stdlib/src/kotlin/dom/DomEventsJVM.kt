package kotlin.dom

import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent

// JavaScript style properties for JVM : TODO could auto-generate these
@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("bubbles"))
public val Event.bubbles: Boolean
    get() = getBubbles()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("cancelable"))
public val Event.cancelable: Boolean
    get() = getCancelable()

public val Event.getCurrentTarget: EventTarget?
    get() = getCurrentTarget()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("eventPhase"))
public val Event.eventPhase: Short
    get() = getEventPhase()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("target"))
public val Event.target: EventTarget?
    get() = getTarget()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("timeStamp"))
public val Event.timeStamp: Long
    get() = getTimeStamp()

// TODO we can't use 'type' as the property name in Kotlin so we should fix it in JS
public val Event.eventType: String
    get() = getType()!!


@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("altKey"))
public val MouseEvent.altKey: Boolean
    get() = getAltKey()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("button"))
public val MouseEvent.button: Short
    get() = getButton()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("clientX"))
public val MouseEvent.clientX: Int
    get() = getClientX()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("clientY"))
public val MouseEvent.clientY: Int
    get() = getClientY()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("ctrlKey"))
public val MouseEvent.ctrlKey: Boolean
    get() = getCtrlKey()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("metaKey"))
public val MouseEvent.metaKey: Boolean
    get() = getMetaKey()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("relatedTarget"))
public val MouseEvent.relatedTarget: EventTarget?
    get() = getRelatedTarget()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("screenX"))
public val MouseEvent.screenX: Int
    get() = getScreenX()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("screenY"))
public val MouseEvent.screenY: Int
    get() = getScreenY()

@HiddenDeclaration
@deprecated("Is replaced with automatic synthetic extension", ReplaceWith("shiftKey"))
public val MouseEvent.shiftKey: Boolean
    get() = getShiftKey()
