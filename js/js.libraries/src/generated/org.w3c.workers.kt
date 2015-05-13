/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.workers

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.xhr.*

native public trait ServiceWorkerRegistration : EventTarget {
    var installing: ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var waiting: ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var active: ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var scope: String
        get() = noImpl
        set(value) = noImpl
    var onupdatefound: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var APISpace: dynamic
        get() = noImpl
        set(value) = noImpl
    fun update(): Unit = noImpl
    fun unregister(): dynamic = noImpl
    fun methodName(of: dynamic): dynamic = noImpl
    fun showNotification(title: String, options: NotificationOptions = noImpl): dynamic = noImpl
    fun getNotifications(filter: GetNotificationOptions = noImpl): dynamic = noImpl
}

native public trait ServiceWorkerGlobalScope : WorkerGlobalScope {
    var clients: Clients
        get() = noImpl
        set(value) = noImpl
    var registration: ServiceWorkerRegistration
        get() = noImpl
        set(value) = noImpl
    var oninstall: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onactivate: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfetch: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onfunctionalevent: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onnotificationclick: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun skipWaiting(): dynamic = noImpl
}

native public trait ServiceWorker : EventTarget, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker {
    var scriptURL: String
        get() = noImpl
        set(value) = noImpl
    var state: String
        get() = noImpl
        set(value) = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    var onstatechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public trait ServiceWorkerContainer : EventTarget {
    var controller: ServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ready: dynamic
        get() = noImpl
        set(value) = noImpl
    var oncontrollerchange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onmessage: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun register(scriptURL: String, options: RegistrationOptions = noImpl): dynamic = noImpl
    fun getRegistration(clientURL: String = ""): dynamic = noImpl
    fun getRegistrations(): dynamic = noImpl
}

native public open class RegistrationOptions {
    var scope: String
}

native public open class ServiceWorkerMessageEvent(type: String, eventInitDict: ServiceWorkerMessageEventInit = noImpl) : Event(type, eventInitDict) {
    var data: Any?
        get() = noImpl
        set(value) = noImpl
    var origin: String
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String
        get() = noImpl
        set(value) = noImpl
    var source: UnionMessagePortOrServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ports: Array<dynamic>
        get() = noImpl
        set(value) = noImpl
    fun initServiceWorkerMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionMessagePortOrServiceWorker, portsArg: Array<dynamic>): Unit = noImpl
}

native public open class ServiceWorkerMessageEventInit : EventInit() {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionMessagePortOrServiceWorker?
    var ports: Array<MessagePort>
}

native public trait Client : UnionClientOrMessagePortOrServiceWorker {
    var url: String
        get() = noImpl
        set(value) = noImpl
    var frameType: String
        get() = noImpl
        set(value) = noImpl
    var id: String
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public trait WindowClient : Client {
    var visibilityState: dynamic
        get() = noImpl
        set(value) = noImpl
    var focused: Boolean
        get() = noImpl
        set(value) = noImpl
    fun focus(): dynamic = noImpl
}

native public trait Clients {
    fun matchAll(options: ClientQueryOptions = noImpl): dynamic = noImpl
    fun openWindow(url: String): dynamic = noImpl
    fun claim(): dynamic = noImpl
}

native public open class ClientQueryOptions {
    var includeUncontrolled: Boolean = false
    var type: String = "window"
}

native public open class ExtendableEvent(type: String, eventInitDict: ExtendableEventInit = noImpl) : Event(type, eventInitDict) {
    fun waitUntil(f: dynamic): Unit = noImpl
}

native public open class ExtendableEventInit : EventInit() {
}

native public open class FetchEvent(type: String, eventInitDict: FetchEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    var request: Request
        get() = noImpl
        set(value) = noImpl
    var client: Client
        get() = noImpl
        set(value) = noImpl
    var isReload: Boolean
        get() = noImpl
        set(value) = noImpl
    fun respondWith(r: dynamic): Unit = noImpl
}

native public open class FetchEventInit : ExtendableEventInit() {
    var request: Request
    var client: Client
    var isReload: Boolean = false
}

native public open class ExtendableMessageEvent(type: String, eventInitDict: ExtendableMessageEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    var data: Any?
        get() = noImpl
        set(value) = noImpl
    var origin: String
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String
        get() = noImpl
        set(value) = noImpl
    var source: UnionClientOrMessagePortOrServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ports: Array<dynamic>
        get() = noImpl
        set(value) = noImpl
    fun initExtendableMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionClientOrMessagePortOrServiceWorker, portsArg: Array<dynamic>): Unit = noImpl
}

native public open class ExtendableMessageEventInit : ExtendableEventInit() {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionClientOrMessagePortOrServiceWorker?
    var ports: Array<MessagePort>
}

native public trait Cache {
    fun match(request: dynamic, options: CacheQueryOptions = noImpl): dynamic = noImpl
    fun matchAll(request: dynamic = noImpl, options: CacheQueryOptions = noImpl): dynamic = noImpl
    fun add(request: dynamic): dynamic = noImpl
    fun addAll(requests: Array<dynamic>): dynamic = noImpl
    fun put(request: dynamic, response: Response): dynamic = noImpl
    fun delete(request: dynamic, options: CacheQueryOptions = noImpl): dynamic = noImpl
    fun keys(request: dynamic = noImpl, options: CacheQueryOptions = noImpl): dynamic = noImpl
}

native public open class CacheQueryOptions {
    var ignoreSearch: Boolean = false
    var ignoreMethod: Boolean = false
    var ignoreVary: Boolean = false
    var cacheName: String
}

native public open class CacheBatchOperation {
    var type: String
    var request: Request
    var response: Response
    var options: CacheQueryOptions
}

native public trait CacheStorage {
    fun match(request: dynamic, options: CacheQueryOptions = noImpl): dynamic = noImpl
    fun has(cacheName: String): dynamic = noImpl
    fun open(cacheName: String): dynamic = noImpl
    fun delete(cacheName: String): dynamic = noImpl
    fun keys(): dynamic = noImpl
}

native public open class FunctionalEvent : ExtendableEvent(noImpl, noImpl) {
}

native public marker trait UnionMessagePortOrServiceWorker {
}

native public marker trait UnionClientOrMessagePortOrServiceWorker {
}

