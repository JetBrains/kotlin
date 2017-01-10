/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.notifications

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external open class Notification(title: String, options: NotificationOptions = noImpl) : EventTarget {
    var onclick: ((Event) -> dynamic)?
    var onerror: ((Event) -> dynamic)?
    open val title: String
    open val dir: String
    open val lang: String
    open val body: String
    open val tag: String
    open val image: String
    open val icon: String
    open val badge: String
    open val sound: String
    open val vibrate: dynamic
    open val timestamp: Number
    open val renotify: Boolean
    open val silent: Boolean
    open val noscreen: Boolean
    open val requireInteraction: Boolean
    open val sticky: Boolean
    open val data: Any?
    open val actions: dynamic
    fun close(): Unit

    companion object {
        var permission: String
        var maxActions: Int
        fun requestPermission(deprecatedCallback: (String) -> Unit = noImpl): dynamic
    }
}

public external interface NotificationOptions {
    var dir: String? /* = "auto" */
        get() = noImpl
        set(value) = noImpl
    var lang: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var body: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var tag: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
    var image: String?
        get() = noImpl
        set(value) = noImpl
    var icon: String?
        get() = noImpl
        set(value) = noImpl
    var badge: String?
        get() = noImpl
        set(value) = noImpl
    var sound: String?
        get() = noImpl
        set(value) = noImpl
    var vibrate: dynamic
        get() = noImpl
        set(value) = noImpl
    var timestamp: Number?
        get() = noImpl
        set(value) = noImpl
    var renotify: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var silent: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var noscreen: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var requireInteraction: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var sticky: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var data: Any? /* = null */
        get() = noImpl
        set(value) = noImpl
    var actions: Array<NotificationAction>? /* = arrayOf() */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationOptions(dir: String? = "auto", lang: String? = "", body: String? = "", tag: String? = "", image: String? = null, icon: String? = null, badge: String? = null, sound: String? = null, vibrate: dynamic = null, timestamp: Number? = null, renotify: Boolean? = false, silent: Boolean? = false, noscreen: Boolean? = false, requireInteraction: Boolean? = false, sticky: Boolean? = false, data: Any? = null, actions: Array<NotificationAction>? = arrayOf()): NotificationOptions {
    val o = js("({})")

    o["dir"] = dir
    o["lang"] = lang
    o["body"] = body
    o["tag"] = tag
    o["image"] = image
    o["icon"] = icon
    o["badge"] = badge
    o["sound"] = sound
    o["vibrate"] = vibrate
    o["timestamp"] = timestamp
    o["renotify"] = renotify
    o["silent"] = silent
    o["noscreen"] = noscreen
    o["requireInteraction"] = requireInteraction
    o["sticky"] = sticky
    o["data"] = data
    o["actions"] = actions

    return o
}

public external interface NotificationAction {
    var action: String?
        get() = noImpl
        set(value) = noImpl
    var title: String?
        get() = noImpl
        set(value) = noImpl
    var icon: String?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationAction(action: String?, title: String?, icon: String? = null): NotificationAction {
    val o = js("({})")

    o["action"] = action
    o["title"] = title
    o["icon"] = icon

    return o
}

public external interface GetNotificationOptions {
    var tag: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun GetNotificationOptions(tag: String? = ""): GetNotificationOptions {
    val o = js("({})")

    o["tag"] = tag

    return o
}

public external open class NotificationEvent(type: String, eventInitDict: NotificationEventInit) : ExtendableEvent {
    open val notification: Notification
    open val action: String
}

public external interface NotificationEventInit : ExtendableEventInit {
    var notification: Notification?
        get() = noImpl
        set(value) = noImpl
    var action: String? /* = "" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationEventInit(notification: Notification?, action: String? = "", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): NotificationEventInit {
    val o = js("({})")

    o["notification"] = notification
    o["action"] = action
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

