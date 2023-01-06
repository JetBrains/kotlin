/*∆*/ public val org.w3c.notifications.NotificationDirection.Companion.AUTO: org.w3c.notifications.NotificationDirection { get; }
/*∆*/ 
/*∆*/ public val org.w3c.notifications.NotificationPermission.Companion.DEFAULT: org.w3c.notifications.NotificationPermission { get; }
/*∆*/ 
/*∆*/ public val org.w3c.notifications.NotificationPermission.Companion.DENIED: org.w3c.notifications.NotificationPermission { get; }
/*∆*/ 
/*∆*/ public val org.w3c.notifications.NotificationPermission.Companion.GRANTED: org.w3c.notifications.NotificationPermission { get; }
/*∆*/ 
/*∆*/ public val org.w3c.notifications.NotificationDirection.Companion.LTR: org.w3c.notifications.NotificationDirection { get; }
/*∆*/ 
/*∆*/ public val org.w3c.notifications.NotificationDirection.Companion.RTL: org.w3c.notifications.NotificationDirection { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun GetNotificationOptions(tag: kotlin.String? = ...): org.w3c.notifications.GetNotificationOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun NotificationAction(action: kotlin.String?, title: kotlin.String?, icon: kotlin.String? = ...): org.w3c.notifications.NotificationAction
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun NotificationEventInit(notification: org.w3c.notifications.Notification?, action: kotlin.String? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.notifications.NotificationEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun NotificationOptions(dir: org.w3c.notifications.NotificationDirection? = ..., lang: kotlin.String? = ..., body: kotlin.String? = ..., tag: kotlin.String? = ..., image: kotlin.String? = ..., icon: kotlin.String? = ..., badge: kotlin.String? = ..., sound: kotlin.String? = ..., vibrate: dynamic = ..., timestamp: kotlin.Number? = ..., renotify: kotlin.Boolean? = ..., silent: kotlin.Boolean? = ..., noscreen: kotlin.Boolean? = ..., requireInteraction: kotlin.Boolean? = ..., sticky: kotlin.Boolean? = ..., data: kotlin.Any? = ..., actions: kotlin.Array<org.w3c.notifications.NotificationAction>? = ...): org.w3c.notifications.NotificationOptions
/*∆*/ 
/*∆*/ public external interface GetNotificationOptions {
/*∆*/     public open var tag: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class Notification : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor Notification(title: kotlin.String, options: org.w3c.notifications.NotificationOptions = ...)
/*∆*/ 
/*∆*/     public open val actions: kotlin.Array<out org.w3c.notifications.NotificationAction> { get; }
/*∆*/ 
/*∆*/     public open val badge: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val body: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val data: kotlin.Any? { get; }
/*∆*/ 
/*∆*/     public open val dir: org.w3c.notifications.NotificationDirection { get; }
/*∆*/ 
/*∆*/     public open val icon: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val image: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val lang: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val noscreen: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public final var onclick: ((org.w3c.dom.events.MouseEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val renotify: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val requireInteraction: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val silent: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val sound: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val sticky: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val tag: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val timestamp: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val title: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val vibrate: kotlin.Array<out kotlin.Int> { get; }
/*∆*/ 
/*∆*/     public final fun close(): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of Notification {
/*∆*/         public final val maxActions: kotlin.Int { get; }
/*∆*/ 
/*∆*/         public final val permission: org.w3c.notifications.NotificationPermission { get; }
/*∆*/ 
/*∆*/         public final fun requestPermission(deprecatedCallback: (org.w3c.notifications.NotificationPermission) -> kotlin.Unit = ...): kotlin.js.Promise<org.w3c.notifications.NotificationPermission>
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface NotificationAction {
/*∆*/     public abstract var action: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var icon: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public abstract var title: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface NotificationDirection {
/*∆*/     public companion object of NotificationDirection {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class NotificationEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor NotificationEvent(type: kotlin.String, eventInitDict: org.w3c.notifications.NotificationEventInit)
/*∆*/ 
/*∆*/     public open val action: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val notification: org.w3c.notifications.Notification { get; }
/*∆*/ 
/*∆*/     public companion object of NotificationEvent {
/*∆*/         public final val AT_TARGET: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val BUBBLING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val CAPTURING_PHASE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val NONE: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface NotificationEventInit : org.w3c.workers.ExtendableEventInit {
/*∆*/     public open var action: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public abstract var notification: org.w3c.notifications.Notification? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface NotificationOptions {
/*∆*/     public open var actions: kotlin.Array<org.w3c.notifications.NotificationAction>? { get; set; }
/*∆*/ 
/*∆*/     public open var badge: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var body: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var data: kotlin.Any? { get; set; }
/*∆*/ 
/*∆*/     public open var dir: org.w3c.notifications.NotificationDirection? { get; set; }
/*∆*/ 
/*∆*/     public open var icon: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var image: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var lang: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var noscreen: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var renotify: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var requireInteraction: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var silent: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var sound: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var sticky: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var tag: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var timestamp: kotlin.Number? { get; set; }
/*∆*/ 
/*∆*/     public open var vibrate: dynamic { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface NotificationPermission {
/*∆*/     public companion object of NotificationPermission {
/*∆*/     }
/*∆*/ }