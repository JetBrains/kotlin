/*∆*/ public val org.w3c.workers.ServiceWorkerState.Companion.ACTIVATED: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ServiceWorkerState.Companion.ACTIVATING: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ClientType.Companion.ALL: org.w3c.workers.ClientType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.FrameType.Companion.AUXILIARY: org.w3c.workers.FrameType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ServiceWorkerState.Companion.INSTALLED: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ServiceWorkerState.Companion.INSTALLING: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.FrameType.Companion.NESTED: org.w3c.workers.FrameType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.FrameType.Companion.NONE: org.w3c.workers.FrameType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ServiceWorkerState.Companion.REDUNDANT: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ClientType.Companion.SHAREDWORKER: org.w3c.workers.ClientType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.FrameType.Companion.TOP_LEVEL: org.w3c.workers.FrameType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ClientType.Companion.WINDOW: org.w3c.workers.ClientType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.workers.ClientType.Companion.WORKER: org.w3c.workers.ClientType { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun CacheBatchOperation(type: kotlin.String? = ..., request: org.w3c.fetch.Request? = ..., response: org.w3c.fetch.Response? = ..., options: org.w3c.workers.CacheQueryOptions? = ...): org.w3c.workers.CacheBatchOperation
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun CacheQueryOptions(ignoreSearch: kotlin.Boolean? = ..., ignoreMethod: kotlin.Boolean? = ..., ignoreVary: kotlin.Boolean? = ..., cacheName: kotlin.String? = ...): org.w3c.workers.CacheQueryOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ClientQueryOptions(includeUncontrolled: kotlin.Boolean? = ..., type: org.w3c.workers.ClientType? = ...): org.w3c.workers.ClientQueryOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ExtendableEventInit(bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.workers.ExtendableEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ExtendableMessageEventInit(data: kotlin.Any? = ..., origin: kotlin.String? = ..., lastEventId: kotlin.String? = ..., source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker? = ..., ports: kotlin.Array<org.w3c.dom.MessagePort>? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.workers.ExtendableMessageEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun FetchEventInit(request: org.w3c.fetch.Request?, clientId: kotlin.String? = ..., isReload: kotlin.Boolean? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.workers.FetchEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ForeignFetchEventInit(request: org.w3c.fetch.Request?, origin: kotlin.String? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.workers.ForeignFetchEventInit
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ForeignFetchOptions(scopes: kotlin.Array<kotlin.String>?, origins: kotlin.Array<kotlin.String>?): org.w3c.workers.ForeignFetchOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ForeignFetchResponse(response: org.w3c.fetch.Response?, origin: kotlin.String? = ..., headers: kotlin.Array<kotlin.String>? = ...): org.w3c.workers.ForeignFetchResponse
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun RegistrationOptions(scope: kotlin.String? = ..., type: org.w3c.dom.WorkerType? = ...): org.w3c.workers.RegistrationOptions
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ServiceWorkerMessageEventInit(data: kotlin.Any? = ..., origin: kotlin.String? = ..., lastEventId: kotlin.String? = ..., source: org.w3c.workers.UnionMessagePortOrServiceWorker? = ..., ports: kotlin.Array<org.w3c.dom.MessagePort>? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.workers.ServiceWorkerMessageEventInit
/*∆*/ 
/*∆*/ public abstract external class Cache {
/*∆*/     public constructor Cache()
/*∆*/ 
/*∆*/     public final fun add(request: dynamic): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun addAll(requests: kotlin.Array<dynamic>): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun delete(request: dynamic, options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ 
/*∆*/     public final fun keys(request: dynamic = ..., options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.fetch.Request>>
/*∆*/ 
/*∆*/     public final fun match(request: dynamic, options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Any?>
/*∆*/ 
/*∆*/     public final fun matchAll(request: dynamic = ..., options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.fetch.Response>>
/*∆*/ 
/*∆*/     public final fun put(request: dynamic, response: org.w3c.fetch.Response): kotlin.js.Promise<kotlin.Unit>
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface CacheBatchOperation {
/*∆*/     public open var options: org.w3c.workers.CacheQueryOptions? { get; set; }
/*∆*/ 
/*∆*/     public open var request: org.w3c.fetch.Request? { get; set; }
/*∆*/ 
/*∆*/     public open var response: org.w3c.fetch.Response? { get; set; }
/*∆*/ 
/*∆*/     public open var type: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface CacheQueryOptions {
/*∆*/     public open var cacheName: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var ignoreMethod: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var ignoreSearch: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var ignoreVary: kotlin.Boolean? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class CacheStorage {
/*∆*/     public constructor CacheStorage()
/*∆*/ 
/*∆*/     public final fun delete(cacheName: kotlin.String): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ 
/*∆*/     public final fun has(cacheName: kotlin.String): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ 
/*∆*/     public final fun keys(): kotlin.js.Promise<kotlin.Array<kotlin.String>>
/*∆*/ 
/*∆*/     public final fun match(request: dynamic, options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Any?>
/*∆*/ 
/*∆*/     public final fun open(cacheName: kotlin.String): kotlin.js.Promise<org.w3c.workers.Cache>
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class Client : org.w3c.workers.UnionClientOrMessagePortOrServiceWorker {
/*∆*/     public constructor Client()
/*∆*/ 
/*∆*/     public open val frameType: org.w3c.workers.FrameType { get; }
/*∆*/ 
/*∆*/     public open val id: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val url: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun postMessage(message: kotlin.Any?, transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ClientQueryOptions {
/*∆*/     public open var includeUncontrolled: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var type: org.w3c.workers.ClientType? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface ClientType {
/*∆*/     public companion object of ClientType {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class Clients {
/*∆*/     public constructor Clients()
/*∆*/ 
/*∆*/     public final fun claim(): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun get(id: kotlin.String): kotlin.js.Promise<kotlin.Any?>
/*∆*/ 
/*∆*/     public final fun matchAll(options: org.w3c.workers.ClientQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.workers.Client>>
/*∆*/ 
/*∆*/     public final fun openWindow(url: kotlin.String): kotlin.js.Promise<org.w3c.workers.WindowClient?>
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ExtendableEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor ExtendableEvent(type: kotlin.String, eventInitDict: org.w3c.workers.ExtendableEventInit = ...)
/*∆*/ 
/*∆*/     public final fun waitUntil(f: kotlin.js.Promise<kotlin.Any?>): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of ExtendableEvent {
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
/*∆*/ public external interface ExtendableEventInit : org.w3c.dom.EventInit {
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ExtendableMessageEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor ExtendableMessageEvent(type: kotlin.String, eventInitDict: org.w3c.workers.ExtendableMessageEventInit = ...)
/*∆*/ 
/*∆*/     public open val data: kotlin.Any? { get; }
/*∆*/ 
/*∆*/     public open val lastEventId: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val origin: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val ports: kotlin.Array<out org.w3c.dom.MessagePort>? { get; }
/*∆*/ 
/*∆*/     public open val source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker? { get; }
/*∆*/ 
/*∆*/     public companion object of ExtendableMessageEvent {
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
/*∆*/ public external interface ExtendableMessageEventInit : org.w3c.workers.ExtendableEventInit {
/*∆*/     public open var data: kotlin.Any? { get; set; }
/*∆*/ 
/*∆*/     public open var lastEventId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var origin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var ports: kotlin.Array<org.w3c.dom.MessagePort>? { get; set; }
/*∆*/ 
/*∆*/     public open var source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class FetchEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor FetchEvent(type: kotlin.String, eventInitDict: org.w3c.workers.FetchEventInit)
/*∆*/ 
/*∆*/     public open val clientId: kotlin.String? { get; }
/*∆*/ 
/*∆*/     public open val isReload: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val request: org.w3c.fetch.Request { get; }
/*∆*/ 
/*∆*/     public final fun respondWith(r: kotlin.js.Promise<org.w3c.fetch.Response>): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of FetchEvent {
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
/*∆*/ public external interface FetchEventInit : org.w3c.workers.ExtendableEventInit {
/*∆*/     public open var clientId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var isReload: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public abstract var request: org.w3c.fetch.Request? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ForeignFetchEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor ForeignFetchEvent(type: kotlin.String, eventInitDict: org.w3c.workers.ForeignFetchEventInit)
/*∆*/ 
/*∆*/     public open val origin: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val request: org.w3c.fetch.Request { get; }
/*∆*/ 
/*∆*/     public final fun respondWith(r: kotlin.js.Promise<org.w3c.workers.ForeignFetchResponse>): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of ForeignFetchEvent {
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
/*∆*/ public external interface ForeignFetchEventInit : org.w3c.workers.ExtendableEventInit {
/*∆*/     public open var origin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public abstract var request: org.w3c.fetch.Request? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ForeignFetchOptions {
/*∆*/     public abstract var origins: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public abstract var scopes: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface ForeignFetchResponse {
/*∆*/     public open var headers: kotlin.Array<kotlin.String>? { get; set; }
/*∆*/ 
/*∆*/     public open var origin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public abstract var response: org.w3c.fetch.Response? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface FrameType {
/*∆*/     public companion object of FrameType {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class FunctionalEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor FunctionalEvent()
/*∆*/ 
/*∆*/     public companion object of FunctionalEvent {
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
/*∆*/ public open external class InstallEvent : org.w3c.workers.ExtendableEvent {
/*∆*/     public constructor InstallEvent(type: kotlin.String, eventInitDict: org.w3c.workers.ExtendableEventInit = ...)
/*∆*/ 
/*∆*/     public final fun registerForeignFetch(options: org.w3c.workers.ForeignFetchOptions): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of InstallEvent {
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
/*∆*/ public external interface RegistrationOptions {
/*∆*/     public open var scope: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var type: org.w3c.dom.WorkerType? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class ServiceWorker : org.w3c.dom.events.EventTarget, org.w3c.dom.AbstractWorker, org.w3c.workers.UnionMessagePortOrServiceWorker, org.w3c.workers.UnionClientOrMessagePortOrServiceWorker {
/*∆*/     public constructor ServiceWorker()
/*∆*/ 
/*∆*/     public open var onstatechange: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val scriptURL: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val state: org.w3c.workers.ServiceWorkerState { get; }
/*∆*/ 
/*∆*/     public final fun postMessage(message: kotlin.Any?, transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class ServiceWorkerContainer : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor ServiceWorkerContainer()
/*∆*/ 
/*∆*/     public open val controller: org.w3c.workers.ServiceWorker? { get; }
/*∆*/ 
/*∆*/     public open var oncontrollerchange: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val ready: kotlin.js.Promise<org.w3c.workers.ServiceWorkerRegistration> { get; }
/*∆*/ 
/*∆*/     public final fun getRegistration(clientURL: kotlin.String = ...): kotlin.js.Promise<kotlin.Any?>
/*∆*/ 
/*∆*/     public final fun getRegistrations(): kotlin.js.Promise<kotlin.Array<org.w3c.workers.ServiceWorkerRegistration>>
/*∆*/ 
/*∆*/     public final fun register(scriptURL: kotlin.String, options: org.w3c.workers.RegistrationOptions = ...): kotlin.js.Promise<org.w3c.workers.ServiceWorkerRegistration>
/*∆*/ 
/*∆*/     public final fun startMessages(): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class ServiceWorkerGlobalScope : org.w3c.dom.WorkerGlobalScope {
/*∆*/     public constructor ServiceWorkerGlobalScope()
/*∆*/ 
/*∆*/     public open val clients: org.w3c.workers.Clients { get; }
/*∆*/ 
/*∆*/     public open var onactivate: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onfetch: ((org.w3c.workers.FetchEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onforeignfetch: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onfunctionalevent: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var oninstall: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onnotificationclick: ((org.w3c.notifications.NotificationEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onnotificationclose: ((org.w3c.notifications.NotificationEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val registration: org.w3c.workers.ServiceWorkerRegistration { get; }
/*∆*/ 
/*∆*/     public final fun skipWaiting(): kotlin.js.Promise<kotlin.Unit>
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ServiceWorkerMessageEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor ServiceWorkerMessageEvent(type: kotlin.String, eventInitDict: org.w3c.workers.ServiceWorkerMessageEventInit = ...)
/*∆*/ 
/*∆*/     public open val data: kotlin.Any? { get; }
/*∆*/ 
/*∆*/     public open val lastEventId: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val origin: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val ports: kotlin.Array<out org.w3c.dom.MessagePort>? { get; }
/*∆*/ 
/*∆*/     public open val source: org.w3c.workers.UnionMessagePortOrServiceWorker? { get; }
/*∆*/ 
/*∆*/     public companion object of ServiceWorkerMessageEvent {
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
/*∆*/ public external interface ServiceWorkerMessageEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var data: kotlin.Any? { get; set; }
/*∆*/ 
/*∆*/     public open var lastEventId: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var origin: kotlin.String? { get; set; }
/*∆*/ 
/*∆*/     public open var ports: kotlin.Array<org.w3c.dom.MessagePort>? { get; set; }
/*∆*/ 
/*∆*/     public open var source: org.w3c.workers.UnionMessagePortOrServiceWorker? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class ServiceWorkerRegistration : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor ServiceWorkerRegistration()
/*∆*/ 
/*∆*/     public open val APISpace: dynamic { get; }
/*∆*/ 
/*∆*/     public open val active: org.w3c.workers.ServiceWorker? { get; }
/*∆*/ 
/*∆*/     public open val installing: org.w3c.workers.ServiceWorker? { get; }
/*∆*/ 
/*∆*/     public open var onupdatefound: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val scope: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val waiting: org.w3c.workers.ServiceWorker? { get; }
/*∆*/ 
/*∆*/     public final fun getNotifications(filter: org.w3c.notifications.GetNotificationOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.notifications.Notification>>
/*∆*/ 
/*∆*/     public final fun methodName(): kotlin.js.Promise<dynamic>
/*∆*/ 
/*∆*/     public final fun showNotification(title: kotlin.String, options: org.w3c.notifications.NotificationOptions = ...): kotlin.js.Promise<kotlin.Unit>
/*∆*/ 
/*∆*/     public final fun unregister(): kotlin.js.Promise<kotlin.Boolean>
/*∆*/ 
/*∆*/     public final fun update(): kotlin.js.Promise<kotlin.Unit>
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface ServiceWorkerState {
/*∆*/     public companion object of ServiceWorkerState {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface UnionClientOrMessagePortOrServiceWorker {
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface UnionMessagePortOrServiceWorker {
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class WindowClient : org.w3c.workers.Client {
/*∆*/     public constructor WindowClient()
/*∆*/ 
/*∆*/     public open val focused: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val visibilityState: dynamic { get; }
/*∆*/ 
/*∆*/     public final fun focus(): kotlin.js.Promise<org.w3c.workers.WindowClient>
/*∆*/ 
/*∆*/     public final fun navigate(url: kotlin.String): kotlin.js.Promise<org.w3c.workers.WindowClient>
/*∆*/ }