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

@native public open class Notification(title: String, options: NotificationOptions = noImpl) : EventTarget {
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
    open val icon: String
        get() = noImpl
    open val sound: String
        get() = noImpl
    open val renotify: Boolean
        get() = noImpl
    open val silent: Boolean
        get() = noImpl
    open val noscreen: Boolean
        get() = noImpl
    open val sticky: Boolean
        get() = noImpl
    open val data: Any?
        get() = noImpl
    fun close(): Unit = noImpl

    companion object {
        var permission: String
            get() = noImpl
            set(value) = noImpl
        fun requestPermission(callback: (String) -> Unit = noImpl): Unit = noImpl
    }
}

@native public interface NotificationOptions {
    var dir: String
    var lang: String
    var body: String
    var tag: String
    var icon: String
    var sound: String
    var vibrate: dynamic
    var renotify: Boolean
    var silent: Boolean
    var noscreen: Boolean
    var sticky: Boolean
    var data: Any?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationOptions(dir: String = "auto", lang: String = "", body: String = "", tag: String = "", icon: String, sound: String, vibrate: dynamic, renotify: Boolean = false, silent: Boolean = false, noscreen: Boolean = false, sticky: Boolean = false, data: Any? = null): NotificationOptions {
    val o = js("({})")

    o["dir"] = dir
    o["lang"] = lang
    o["body"] = body
    o["tag"] = tag
    o["icon"] = icon
    o["sound"] = sound
    o["vibrate"] = vibrate
    o["renotify"] = renotify
    o["silent"] = silent
    o["noscreen"] = noscreen
    o["sticky"] = sticky
    o["data"] = data

    return o
}

@native public interface GetNotificationOptions {
    var tag: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun GetNotificationOptions(tag: String = ""): GetNotificationOptions {
    val o = js("({})")

    o["tag"] = tag

    return o
}

@native public open class NotificationEvent(type: String, eventInitDict: NotificationEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    open val notification: Notification
        get() = noImpl
}

@native public interface NotificationEventInit : ExtendableEventInit {
    var notification: Notification
}

@Suppress("NOTHING_TO_INLINE")
public inline fun NotificationEventInit(notification: Notification, bubbles: Boolean = false, cancelable: Boolean = false): NotificationEventInit {
    val o = js("({})")

    o["notification"] = notification
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable

    return o
}

