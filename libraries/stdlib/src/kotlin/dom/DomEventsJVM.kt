package kotlin.dom

import org.w3c.dom.Node
import org.w3c.dom.events.*

// JavaScript style properties for JVM : TODO could auto-generate these
public val Event.bubbles: Boolean
    get() = getBubbles()

public val Event.cancelable: Boolean
    get() = getCancelable()

public val Event.getCurrentTarget: EventTarget?
    get() = getCurrentTarget()

public val Event.eventPhase: Short
    get() = getEventPhase()

public val Event.target: EventTarget?
    get() = getTarget()

public val Event.timeStamp: Long
    get() = getTimeStamp()

// TODO we can't use 'type' as the property name in Kotlin so we should fix it in JS
public val Event.eventType: String
    get() = getType()!!


public val MouseEvent.altKey: Boolean
    get() = getAltKey()

public val MouseEvent.button: Short
    get() = getButton()

public val MouseEvent.clientX: Int
    get() = getClientX()

public val MouseEvent.clientY: Int
    get() = getClientY()

public val MouseEvent.ctrlKey: Boolean
    get() = getCtrlKey()

public val MouseEvent.metaKey: Boolean
    get() = getMetaKey()

public val MouseEvent.relatedTarget: EventTarget?
    get() = getRelatedTarget()

public val MouseEvent.screenX: Int
    get() = getScreenX()

public val MouseEvent.screenY: Int
    get() = getScreenY()

public val MouseEvent.shiftKey: Boolean
    get() = getShiftKey()
