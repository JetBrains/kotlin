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
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.xhr.*

native public interface ServiceWorkerRegistration : EventTarget {
    val installing: ServiceWorker?
        get() = noImpl
    val waiting: ServiceWorker?
        get() = noImpl
    val active: ServiceWorker?
        get() = noImpl
    val scope: String
        get() = noImpl
    var onupdatefound: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    val APISpace: dynamic
        get() = noImpl
    fun update(): Unit = noImpl
    fun unregister(): dynamic = noImpl
    fun methodName(of: dynamic): dynamic = noImpl
    fun showNotification(title: String, options: NotificationOptions = noImpl): dynamic = noImpl
    fun getNotifications(filter: GetNotificationOptions = noImpl): dynamic = noImpl
}

native public interface ServiceWorkerGlobalScope : WorkerGlobalScope {
    val clients: Clients
        get() = noImpl
    val registration: ServiceWorkerRegistration
        get() = noImpl
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

native public interface ServiceWorker : EventTarget, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker {
    val scriptURL: String
        get() = noImpl
    val state: String
        get() = noImpl
    val id: String
        get() = noImpl
    var onstatechange: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public interface ServiceWorkerContainer : EventTarget {
    val controller: ServiceWorker?
        get() = noImpl
    val ready: dynamic
        get() = noImpl
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
    open val data: Any?
        get() = noImpl
    open val origin: String
        get() = noImpl
    open val lastEventId: String
        get() = noImpl
    open val source: UnionMessagePortOrServiceWorker?
        get() = noImpl
    open val ports: Array<MessagePort>?
        get() = noImpl
    fun initServiceWorkerMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionMessagePortOrServiceWorker, portsArg: Array<MessagePort>?): Unit = noImpl
}

native public open class ServiceWorkerMessageEventInit : EventInit() {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionMessagePortOrServiceWorker?
    var ports: Array<MessagePort>
}

native public interface Client : UnionClientOrMessagePortOrServiceWorker {
    val url: String
        get() = noImpl
    val frameType: String
        get() = noImpl
    val id: String
        get() = noImpl
    fun postMessage(message: Any?, transfer: Array<Transferable> = noImpl): Unit = noImpl
}

native public interface WindowClient : Client {
    val visibilityState: dynamic
        get() = noImpl
    val focused: Boolean
        get() = noImpl
    fun focus(): dynamic = noImpl
}

native public interface Clients {
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
    open val request: Request
        get() = noImpl
    open val client: Client
        get() = noImpl
    open val isReload: Boolean
        get() = noImpl
    fun respondWith(r: dynamic): Unit = noImpl
}

native public open class FetchEventInit : ExtendableEventInit() {
    var request: Request
    var client: Client
    var isReload: Boolean = false
}

native public open class ExtendableMessageEvent(type: String, eventInitDict: ExtendableMessageEventInit = noImpl) : ExtendableEvent(type, eventInitDict) {
    open val data: Any?
        get() = noImpl
    open val origin: String
        get() = noImpl
    open val lastEventId: String
        get() = noImpl
    open val source: UnionClientOrMessagePortOrServiceWorker?
        get() = noImpl
    open val ports: Array<MessagePort>?
        get() = noImpl
    fun initExtendableMessageEvent(typeArg: String, canBubbleArg: Boolean, cancelableArg: Boolean, dataArg: Any?, originArg: String, lastEventIdArg: String, sourceArg: UnionClientOrMessagePortOrServiceWorker, portsArg: Array<MessagePort>?): Unit = noImpl
}

native public open class ExtendableMessageEventInit : ExtendableEventInit() {
    var data: Any?
    var origin: String
    var lastEventId: String
    var source: UnionClientOrMessagePortOrServiceWorker?
    var ports: Array<MessagePort>
}

native public interface Cache {
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

native public interface CacheStorage {
    fun match(request: dynamic, options: CacheQueryOptions = noImpl): dynamic = noImpl
    fun has(cacheName: String): dynamic = noImpl
    fun open(cacheName: String): dynamic = noImpl
    fun delete(cacheName: String): dynamic = noImpl
    fun keys(): dynamic = noImpl
}

native public open class FunctionalEvent : ExtendableEvent(noImpl, noImpl) {
}

native public marker interface UnionClientOrMessagePortOrServiceWorker {
}

