/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.notifications

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.events.*
import org.w3c.workers.*

/**
 * Exposes the JavaScript [Notification](https://developer.mozilla.org/en/docs/Web/API/Notification) to Kotlin
 */
public external open class Notification(title: String, options: NotificationOptions = definedExternally) : EventTarget, JsAny {
    var onclick: ((MouseEvent) -> JsAny?)?
    var onerror: ((Event) -> JsAny?)?
    open val title: String
    open val dir: NotificationDirection
    open val lang: String
    open val body: String
    open val tag: String
    open val image: String
    open val icon: String
    open val badge: String
    open val sound: String
    open val vibrate: JsArray<out JsNumber>
    open val timestamp: JsNumber
    open val renotify: Boolean
    open val silent: Boolean
    open val noscreen: Boolean
    open val requireInteraction: Boolean
    open val sticky: Boolean
    open val data: JsAny?
    open val actions: JsArray<out NotificationAction>
    fun close()

    companion object {
        val permission: NotificationPermission
        val maxActions: Int
        fun requestPermission(deprecatedCallback: (NotificationPermission) -> Unit = definedExternally): Promise<NotificationPermission>
    }
}

public external interface NotificationOptions : JsAny {
    var dir: NotificationDirection? /* = NotificationDirection.AUTO */
        get() = definedExternally
        set(value) = definedExternally
    var lang: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var body: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var tag: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
    var image: String?
        get() = definedExternally
        set(value) = definedExternally
    var icon: String?
        get() = definedExternally
        set(value) = definedExternally
    var badge: String?
        get() = definedExternally
        set(value) = definedExternally
    var sound: String?
        get() = definedExternally
        set(value) = definedExternally
    var vibrate: JsAny?
        get() = definedExternally
        set(value) = definedExternally
    var timestamp: JsNumber?
        get() = definedExternally
        set(value) = definedExternally
    var renotify: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var silent: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var noscreen: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var requireInteraction: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var sticky: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var data: JsAny? /* = null */
        get() = definedExternally
        set(value) = definedExternally
    var actions: JsArray<NotificationAction>? /* = arrayOf() */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun NotificationOptions(dir: NotificationDirection? = NotificationDirection.AUTO, lang: String? = "", body: String? = "", tag: String? = "", image: String? = undefined, icon: String? = undefined, badge: String? = undefined, sound: String? = undefined, vibrate: JsAny? = undefined, timestamp: JsNumber? = undefined, renotify: Boolean? = false, silent: Boolean? = false, noscreen: Boolean? = false, requireInteraction: Boolean? = false, sticky: Boolean? = false, data: JsAny? = null, actions: JsArray<NotificationAction>? = JsArray()): NotificationOptions { js("return { dir, lang, body, tag, image, icon, badge, sound, vibrate, timestamp, renotify, silent, noscreen, requireInteraction, sticky, data, actions };") }

public external interface NotificationAction : JsAny {
    var action: String?
    var title: String?
    var icon: String?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun NotificationAction(action: String?, title: String?, icon: String? = undefined): NotificationAction { js("return { action, title, icon };") }

public external interface GetNotificationOptions : JsAny {
    var tag: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun GetNotificationOptions(tag: String? = ""): GetNotificationOptions { js("return { tag };") }

/**
 * Exposes the JavaScript [NotificationEvent](https://developer.mozilla.org/en/docs/Web/API/NotificationEvent) to Kotlin
 */
public external open class NotificationEvent(type: String, eventInitDict: NotificationEventInit) : ExtendableEvent, JsAny {
    open val notification: Notification
    open val action: String

    companion object {
        val NONE: Short
        val CAPTURING_PHASE: Short
        val AT_TARGET: Short
        val BUBBLING_PHASE: Short
    }
}

public external interface NotificationEventInit : ExtendableEventInit, JsAny {
    var notification: Notification?
    var action: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun NotificationEventInit(notification: Notification?, action: String? = "", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): NotificationEventInit { js("return { notification, action, bubbles, cancelable, composed };") }

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface NotificationPermission : JsAny {
    companion object
}

public inline val NotificationPermission.Companion.DEFAULT: NotificationPermission get() = "default".toJsString().unsafeCast<NotificationPermission>()

public inline val NotificationPermission.Companion.DENIED: NotificationPermission get() = "denied".toJsString().unsafeCast<NotificationPermission>()

public inline val NotificationPermission.Companion.GRANTED: NotificationPermission get() = "granted".toJsString().unsafeCast<NotificationPermission>()

/* please, don't implement this interface! */
@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface NotificationDirection : JsAny {
    companion object
}

public inline val NotificationDirection.Companion.AUTO: NotificationDirection get() = "auto".toJsString().unsafeCast<NotificationDirection>()

public inline val NotificationDirection.Companion.LTR: NotificationDirection get() = "ltr".toJsString().unsafeCast<NotificationDirection>()

public inline val NotificationDirection.Companion.RTL: NotificationDirection get() = "rtl".toJsString().unsafeCast<NotificationDirection>()