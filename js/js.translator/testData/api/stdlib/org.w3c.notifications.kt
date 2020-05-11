package org.w3c.notifications

public val org.w3c.notifications.NotificationDirection.Companion.AUTO: org.w3c.notifications.NotificationDirection
    public inline fun org.w3c.notifications.NotificationDirection.Companion.<get-AUTO>(): org.w3c.notifications.NotificationDirection
public val org.w3c.notifications.NotificationPermission.Companion.DEFAULT: org.w3c.notifications.NotificationPermission
    public inline fun org.w3c.notifications.NotificationPermission.Companion.<get-DEFAULT>(): org.w3c.notifications.NotificationPermission
public val org.w3c.notifications.NotificationPermission.Companion.DENIED: org.w3c.notifications.NotificationPermission
    public inline fun org.w3c.notifications.NotificationPermission.Companion.<get-DENIED>(): org.w3c.notifications.NotificationPermission
public val org.w3c.notifications.NotificationPermission.Companion.GRANTED: org.w3c.notifications.NotificationPermission
    public inline fun org.w3c.notifications.NotificationPermission.Companion.<get-GRANTED>(): org.w3c.notifications.NotificationPermission
public val org.w3c.notifications.NotificationDirection.Companion.LTR: org.w3c.notifications.NotificationDirection
    public inline fun org.w3c.notifications.NotificationDirection.Companion.<get-LTR>(): org.w3c.notifications.NotificationDirection
public val org.w3c.notifications.NotificationDirection.Companion.RTL: org.w3c.notifications.NotificationDirection
    public inline fun org.w3c.notifications.NotificationDirection.Companion.<get-RTL>(): org.w3c.notifications.NotificationDirection
@kotlin.internal.InlineOnly public inline fun GetNotificationOptions(/*0*/ tag: kotlin.String? = ...): org.w3c.notifications.GetNotificationOptions
@kotlin.internal.InlineOnly public inline fun NotificationAction(/*0*/ action: kotlin.String?, /*1*/ title: kotlin.String?, /*2*/ icon: kotlin.String? = ...): org.w3c.notifications.NotificationAction
@kotlin.internal.InlineOnly public inline fun NotificationEventInit(/*0*/ notification: org.w3c.notifications.Notification?, /*1*/ action: kotlin.String? = ..., /*2*/ bubbles: kotlin.Boolean? = ..., /*3*/ cancelable: kotlin.Boolean? = ..., /*4*/ composed: kotlin.Boolean? = ...): org.w3c.notifications.NotificationEventInit
@kotlin.internal.InlineOnly public inline fun NotificationOptions(/*0*/ dir: org.w3c.notifications.NotificationDirection? = ..., /*1*/ lang: kotlin.String? = ..., /*2*/ body: kotlin.String? = ..., /*3*/ tag: kotlin.String? = ..., /*4*/ image: kotlin.String? = ..., /*5*/ icon: kotlin.String? = ..., /*6*/ badge: kotlin.String? = ..., /*7*/ sound: kotlin.String? = ..., /*8*/ vibrate: dynamic = ..., /*9*/ timestamp: kotlin.Number? = ..., /*10*/ renotify: kotlin.Boolean? = ..., /*11*/ silent: kotlin.Boolean? = ..., /*12*/ noscreen: kotlin.Boolean? = ..., /*13*/ requireInteraction: kotlin.Boolean? = ..., /*14*/ sticky: kotlin.Boolean? = ..., /*15*/ data: kotlin.Any? = ..., /*16*/ actions: kotlin.Array<org.w3c.notifications.NotificationAction>? = ...): org.w3c.notifications.NotificationOptions

public external interface GetNotificationOptions {
    public open var tag: kotlin.String?
        public open fun <get-tag>(): kotlin.String?
        public open fun <set-tag>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public open external class Notification : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor Notification(/*0*/ title: kotlin.String, /*1*/ options: org.w3c.notifications.NotificationOptions = ...)
    public open val actions: kotlin.Array<out org.w3c.notifications.NotificationAction>
        public open fun <get-actions>(): kotlin.Array<out org.w3c.notifications.NotificationAction>
    public open val badge: kotlin.String
        public open fun <get-badge>(): kotlin.String
    public open val body: kotlin.String
        public open fun <get-body>(): kotlin.String
    public open val data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
    public open val dir: org.w3c.notifications.NotificationDirection
        public open fun <get-dir>(): org.w3c.notifications.NotificationDirection
    public open val icon: kotlin.String
        public open fun <get-icon>(): kotlin.String
    public open val image: kotlin.String
        public open fun <get-image>(): kotlin.String
    public open val lang: kotlin.String
        public open fun <get-lang>(): kotlin.String
    public open val noscreen: kotlin.Boolean
        public open fun <get-noscreen>(): kotlin.Boolean
    public final var onclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public final fun <get-onclick>(): ((org.w3c.dom.events.MouseEvent) -> dynamic)?
        public final fun <set-onclick>(/*0*/ <set-?>: ((org.w3c.dom.events.MouseEvent) -> dynamic)?): kotlin.Unit
    public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val renotify: kotlin.Boolean
        public open fun <get-renotify>(): kotlin.Boolean
    public open val requireInteraction: kotlin.Boolean
        public open fun <get-requireInteraction>(): kotlin.Boolean
    public open val silent: kotlin.Boolean
        public open fun <get-silent>(): kotlin.Boolean
    public open val sound: kotlin.String
        public open fun <get-sound>(): kotlin.String
    public open val sticky: kotlin.Boolean
        public open fun <get-sticky>(): kotlin.Boolean
    public open val tag: kotlin.String
        public open fun <get-tag>(): kotlin.String
    public open val timestamp: kotlin.Number
        public open fun <get-timestamp>(): kotlin.Number
    public open val title: kotlin.String
        public open fun <get-title>(): kotlin.String
    public open val vibrate: kotlin.Array<out kotlin.Int>
        public open fun <get-vibrate>(): kotlin.Array<out kotlin.Int>
    public final fun close(): kotlin.Unit

    public companion object Companion {
        public final val maxActions: kotlin.Int
            public final fun <get-maxActions>(): kotlin.Int
        public final val permission: org.w3c.notifications.NotificationPermission
            public final fun <get-permission>(): org.w3c.notifications.NotificationPermission
        public final fun requestPermission(/*0*/ deprecatedCallback: (org.w3c.notifications.NotificationPermission) -> kotlin.Unit = ...): kotlin.js.Promise<org.w3c.notifications.NotificationPermission>
    }
}

public external interface NotificationAction {
    public open var action: kotlin.String?
        public open fun <get-action>(): kotlin.String?
        public open fun <set-action>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var icon: kotlin.String?
        public open fun <get-icon>(): kotlin.String?
        public open fun <set-icon>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var title: kotlin.String?
        public open fun <get-title>(): kotlin.String?
        public open fun <set-title>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public external interface NotificationDirection {

    public companion object Companion {
    }
}

public open external class NotificationEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor NotificationEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.notifications.NotificationEventInit)
    public open val action: kotlin.String
        public open fun <get-action>(): kotlin.String
    public open val notification: org.w3c.notifications.Notification
        public open fun <get-notification>(): org.w3c.notifications.Notification

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface NotificationEventInit : org.w3c.workers.ExtendableEventInit {
    public open var action: kotlin.String?
        public open fun <get-action>(): kotlin.String?
        public open fun <set-action>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var notification: org.w3c.notifications.Notification?
        public open fun <get-notification>(): org.w3c.notifications.Notification?
        public open fun <set-notification>(/*0*/ value: org.w3c.notifications.Notification?): kotlin.Unit
}

public external interface NotificationOptions {
    public open var actions: kotlin.Array<org.w3c.notifications.NotificationAction>?
        public open fun <get-actions>(): kotlin.Array<org.w3c.notifications.NotificationAction>?
        public open fun <set-actions>(/*0*/ value: kotlin.Array<org.w3c.notifications.NotificationAction>?): kotlin.Unit
    public open var badge: kotlin.String?
        public open fun <get-badge>(): kotlin.String?
        public open fun <set-badge>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var body: kotlin.String?
        public open fun <get-body>(): kotlin.String?
        public open fun <set-body>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
        public open fun <set-data>(/*0*/ value: kotlin.Any?): kotlin.Unit
    public open var dir: org.w3c.notifications.NotificationDirection?
        public open fun <get-dir>(): org.w3c.notifications.NotificationDirection?
        public open fun <set-dir>(/*0*/ value: org.w3c.notifications.NotificationDirection?): kotlin.Unit
    public open var icon: kotlin.String?
        public open fun <get-icon>(): kotlin.String?
        public open fun <set-icon>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var image: kotlin.String?
        public open fun <get-image>(): kotlin.String?
        public open fun <set-image>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var lang: kotlin.String?
        public open fun <get-lang>(): kotlin.String?
        public open fun <set-lang>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var noscreen: kotlin.Boolean?
        public open fun <get-noscreen>(): kotlin.Boolean?
        public open fun <set-noscreen>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var renotify: kotlin.Boolean?
        public open fun <get-renotify>(): kotlin.Boolean?
        public open fun <set-renotify>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var requireInteraction: kotlin.Boolean?
        public open fun <get-requireInteraction>(): kotlin.Boolean?
        public open fun <set-requireInteraction>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var silent: kotlin.Boolean?
        public open fun <get-silent>(): kotlin.Boolean?
        public open fun <set-silent>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var sound: kotlin.String?
        public open fun <get-sound>(): kotlin.String?
        public open fun <set-sound>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var sticky: kotlin.Boolean?
        public open fun <get-sticky>(): kotlin.Boolean?
        public open fun <set-sticky>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var tag: kotlin.String?
        public open fun <get-tag>(): kotlin.String?
        public open fun <set-tag>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var timestamp: kotlin.Number?
        public open fun <get-timestamp>(): kotlin.Number?
        public open fun <set-timestamp>(/*0*/ value: kotlin.Number?): kotlin.Unit
    public open var vibrate: dynamic
        public open fun <get-vibrate>(): dynamic
        public open fun <set-vibrate>(/*0*/ value: dynamic): kotlin.Unit
}

public external interface NotificationPermission {

    public companion object Companion {
    }
}