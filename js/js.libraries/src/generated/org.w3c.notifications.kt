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

@native public open class Notification(title: String, options: NotificationOptions = noImpl) : EventTarget() {
    var onclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    open val title: String
        get() = noImpl
    open val dir: String
        get() = noImpl
    open val lang: String
        get() = noImpl
    open val body: String
        get() = noImpl
    open val tag: String
        get() = noImpl
    open val image: String
        get() = noImpl
    open val icon: String
        get() = noImpl
    open val badge: String
        get() = noImpl
    open val sound: String
        get() = noImpl
    open val vibrate: dynamic
        get() = noImpl
    open val timestamp: Number
        get() = noImpl
    open val renotify: Boolean
        get() = noImpl
    open val silent: Boolean
        get() = noImpl
    open val noscreen: Boolean
        get() = noImpl
    open val requireInteraction: Boolean
        get() = noImpl
    open val sticky: Boolean
        get() = noImpl
    open val data: Any?
        get() = noImpl
    open val actions: dynamic
        get() = noImpl
    fun close(): Unit = noImpl

    companion object {
        var permission: String
            get() = noImpl
            set(value) = noImpl
        var maxActions: Int
            get() = noImpl
            set(value) = noImpl
        fun requestPermission(deprecatedCallback: (String) -> Unit = noImpl): dynamic = noImpl
    }
}

@native public interface NotificationOptions {
    var dir: String? /* = "auto" */
    var lang: String? /* = "" */
    var body: String? /* = "" */
    var tag: String? /* = "" */
    var image: String?
    var icon: String?
    var badge: String?
    var sound: String?
    var vibrate: dynamic
    var timestamp: Number?
    var renotify: Boolean? /* = false */
    var silent: Boolean? /* = false */
    var noscreen: Boolean? /* = false */
    var requireInteraction: Boolean? /* = false */
    var sticky: Boolean? /* = false */
    var data: Any? /* = null */
    var actions: Array<NotificationAction>? /* = arrayOf() */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationOptions(dir: String? = "auto", lang: String? = "", body: String? = "", tag: String? = "", image: String?, icon: String?, badge: String?, sound: String?, vibrate: dynamic, timestamp: Number?, renotify: Boolean? = false, silent: Boolean? = false, noscreen: Boolean? = false, requireInteraction: Boolean? = false, sticky: Boolean? = false, data: Any? = null, actions: Array<NotificationAction>? = arrayOf()): NotificationOptions {
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

@native public interface NotificationAction {
    var action: String?
    var title: String?
    var icon: String?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationAction(action: String?, title: String?, icon: String?): NotificationAction {
    val o = js("({})")

    o["action"] = action
    o["title"] = title
    o["icon"] = icon

    return o
}

@native public interface GetNotificationOptions {
    var tag: String? /* = "" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun GetNotificationOptions(tag: String? = ""): GetNotificationOptions {
    val o = js("({})")

    o["tag"] = tag

    return o
}

@native public open class NotificationEvent(type: String, eventInitDict: NotificationEventInit) : ExtendableEvent(type, eventInitDict) {
    open val notification: Notification
        get() = noImpl
    open val action: String
        get() = noImpl
}

@native public interface NotificationEventInit : ExtendableEventInit {
    var notification: Notification?
    var action: String? /* = "" */
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

