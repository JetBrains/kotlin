@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("DomEventsKt")
package kotlin.dom

import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent

// JavaScript style properties for JVM : TODO could auto-generate these
@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("bubbles"), level = DeprecationLevel.HIDDEN)
public val Event.bubbles: Boolean
    get() = getBubbles()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("cancelable"), level = DeprecationLevel.HIDDEN)
public val Event.cancelable: Boolean
    get() = getCancelable()

public val Event.getCurrentTarget: EventTarget?
    get() = getCurrentTarget()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("eventPhase"), level = DeprecationLevel.HIDDEN)
public val Event.eventPhase: Short
    get() = getEventPhase()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("target"), level = DeprecationLevel.HIDDEN)
public val Event.target: EventTarget?
    get() = getTarget()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("timeStamp"), level = DeprecationLevel.HIDDEN)
public val Event.timeStamp: Long
    get() = getTimeStamp()

// TODO we can't use 'type' as the property name in Kotlin so we should fix it in JS
public val Event.eventType: String
    get() = getType()!!


@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("altKey"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.altKey: Boolean
    get() = getAltKey()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("button"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.button: Short
    get() = getButton()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("clientX"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.clientX: Int
    get() = getClientX()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("clientY"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.clientY: Int
    get() = getClientY()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("ctrlKey"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.ctrlKey: Boolean
    get() = getCtrlKey()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("metaKey"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.metaKey: Boolean
    get() = getMetaKey()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("relatedTarget"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.relatedTarget: EventTarget?
    get() = getRelatedTarget()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("screenX"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.screenX: Int
    get() = getScreenX()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("screenY"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.screenY: Int
    get() = getScreenY()

@Deprecated("Is replaced with automatic synthetic extension", ReplaceWith("shiftKey"), level = DeprecationLevel.HIDDEN)
public val MouseEvent.shiftKey: Boolean
    get() = getShiftKey()
