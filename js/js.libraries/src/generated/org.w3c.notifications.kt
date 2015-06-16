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

native public open class Notification(title: String, options: NotificationOptions = noImpl) : EventTarget {
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

native public open class NotificationOptions {
    var dir: String = "auto"
    var lang: String = ""
    var body: String = ""
    var tag: String = ""
    var icon: String
    var sound: String
    var vibrate: dynamic
    var renotify: Boolean = false
    var silent: Boolean = false
    var noscreen: Boolean = false
    var sticky: Boolean = false
    var data: Any? = null
}

native public open class GetNotificationOptions {
    var tag: String = ""
}

native public open class NotificationEvent(type: String, eventInitDict: NotificationEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    open val notification: Notification
        get() = noImpl
}

native public open class NotificationEventInit : ExtendableEventInit() {
    var notification: Notification
}

