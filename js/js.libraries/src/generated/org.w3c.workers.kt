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

public external abstract class ServiceWorkerRegistration : EventTarget {
    open val installing: ServiceWorker?
    open val waiting: ServiceWorker?
    open val active: ServiceWorker?
    open val scope: String
    open var onupdatefound: ((Event) -> dynamic)?
    open val APISpace: dynamic
    fun update(): dynamic
    fun unregister(): dynamic
    fun methodName(): dynamic
    fun showNotification(title: String, options: NotificationOptions = noImpl): dynamic
    fun getNotifications(filter: GetNotificationOptions = noImpl): dynamic
}

public external abstract class ServiceWorkerGlobalScope : WorkerGlobalScope {
    open val clients: Clients
    open val registration: ServiceWorkerRegistration
    open var oninstall: ((Event) -> dynamic)?
    open var onactivate: ((Event) -> dynamic)?
    open var onfetch: ((Event) -> dynamic)?
    open var onforeignfetch: ((Event) -> dynamic)?
    open var onmessage: ((Event) -> dynamic)?
    open var onfunctionalevent: ((Event) -> dynamic)?
    open var onnotificationclick: ((Event) -> dynamic)?
    open var onnotificationclose: ((Event) -> dynamic)?
    fun skipWaiting(): dynamic
}

public external abstract class ServiceWorker : EventTarget, AbstractWorker, UnionMessagePortOrServiceWorker, UnionClientOrMessagePortOrServiceWorker {
    open val scriptURL: String
    open val state: String
    open var onstatechange: ((Event) -> dynamic)?
    fun postMessage(message: Any?, transfer: Array<dynamic> = noImpl): Unit
}

public external abstract class ServiceWorkerContainer : EventTarget {
    open val controller: ServiceWorker?
    open val ready: dynamic
    open var oncontrollerchange: ((Event) -> dynamic)?
    open var onmessage: ((Event) -> dynamic)?
    fun register(scriptURL: String, options: RegistrationOptions = noImpl): dynamic
    fun getRegistration(clientURL: String = noImpl): dynamic
    fun getRegistrations(): dynamic
    fun startMessages(): Unit
}

public external interface RegistrationOptions {
    var scope: String?
        get() = noImpl
        set(value) = noImpl
    var type: String? /* = "classic" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun RegistrationOptions(scope: String?, type: String? = "classic"): RegistrationOptions {
    val o = js("({})")

    o["scope"] = scope
    o["type"] = type

    return o
}

public external open class ServiceWorkerMessageEvent(type: String, eventInitDict: ServiceWorkerMessageEventInit = noImpl) : Event {
    open val data: Any?
    open val origin: String
    open val lastEventId: String
    open val source: UnionMessagePortOrServiceWorker?
    open val ports: dynamic
}

public external interface ServiceWorkerMessageEventInit : EventInit {
    var data: Any?
        get() = noImpl
        set(value) = noImpl
    var origin: String?
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String?
        get() = noImpl
        set(value) = noImpl
    var source: UnionMessagePortOrServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ports: Array<MessagePort>?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ServiceWorkerMessageEventInit(data: Any?, origin: String?, lastEventId: String?, source: UnionMessagePortOrServiceWorker?, ports: Array<MessagePort>?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ServiceWorkerMessageEventInit {
    val o = js("({})")

    o["data"] = data
    o["origin"] = origin
    o["lastEventId"] = lastEventId
    o["source"] = source
    o["ports"] = ports
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external abstract class Client : UnionClientOrMessagePortOrServiceWorker {
    open val url: String
    open val frameType: String
    open val id: String
    fun postMessage(message: Any?, transfer: Array<dynamic> = noImpl): Unit
}

public external abstract class WindowClient : Client {
    open val visibilityState: dynamic
    open val focused: Boolean
    fun focus(): dynamic
    fun navigate(url: String): dynamic
}

public external abstract class Clients {
    fun get(id: String): dynamic
    fun matchAll(options: ClientQueryOptions = noImpl): dynamic
    fun openWindow(url: String): dynamic
    fun claim(): dynamic
}

public external interface ClientQueryOptions {
    var includeUncontrolled: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var type: String? /* = "window" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ClientQueryOptions(includeUncontrolled: Boolean? = false, type: String? = "window"): ClientQueryOptions {
    val o = js("({})")

    o["includeUncontrolled"] = includeUncontrolled
    o["type"] = type

    return o
}

public external open class ExtendableEvent(type: String, eventInitDict: ExtendableEventInit = noImpl) : Event {
    fun waitUntil(f: dynamic): Unit
}

public external interface ExtendableEventInit : EventInit {
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ExtendableEventInit(bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ExtendableEventInit {
    val o = js("({})")

    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class InstallEvent(type: String, eventInitDict: ExtendableEventInit = noImpl) : ExtendableEvent {
    fun registerForeignFetch(options: ForeignFetchOptions): Unit
}

public external interface ForeignFetchOptions {
    var scopes: Array<String>?
        get() = noImpl
        set(value) = noImpl
    var origins: Array<String>?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ForeignFetchOptions(scopes: Array<String>?, origins: Array<String>?): ForeignFetchOptions {
    val o = js("({})")

    o["scopes"] = scopes
    o["origins"] = origins

    return o
}

public external open class FetchEvent(type: String, eventInitDict: FetchEventInit) : ExtendableEvent {
    open val request: Request
    open val clientId: String?
    open val isReload: Boolean
    fun respondWith(r: dynamic): Unit
}

public external interface FetchEventInit : ExtendableEventInit {
    var request: Request?
        get() = noImpl
        set(value) = noImpl
    var clientId: String? /* = null */
        get() = noImpl
        set(value) = noImpl
    var isReload: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun FetchEventInit(request: Request?, clientId: String? = null, isReload: Boolean? = false, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): FetchEventInit {
    val o = js("({})")

    o["request"] = request
    o["clientId"] = clientId
    o["isReload"] = isReload
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external open class ForeignFetchEvent(type: String, eventInitDict: ForeignFetchEventInit) : ExtendableEvent {
    open val request: Request
    open val origin: String
    fun respondWith(r: dynamic): Unit
}

public external interface ForeignFetchEventInit : ExtendableEventInit {
    var request: Request?
        get() = noImpl
        set(value) = noImpl
    var origin: String? /* = "null" */
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ForeignFetchEventInit(request: Request?, origin: String? = "null", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ForeignFetchEventInit {
    val o = js("({})")

    o["request"] = request
    o["origin"] = origin
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external interface ForeignFetchResponse {
    var response: Response?
        get() = noImpl
        set(value) = noImpl
    var origin: String?
        get() = noImpl
        set(value) = noImpl
    var headers: Array<String>?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ForeignFetchResponse(response: Response?, origin: String?, headers: Array<String>?): ForeignFetchResponse {
    val o = js("({})")

    o["response"] = response
    o["origin"] = origin
    o["headers"] = headers

    return o
}

public external open class ExtendableMessageEvent(type: String, eventInitDict: ExtendableMessageEventInit = noImpl) : ExtendableEvent {
    open val data: Any?
    open val origin: String
    open val lastEventId: String
    open val source: UnionClientOrMessagePortOrServiceWorker?
    open val ports: dynamic
}

public external interface ExtendableMessageEventInit : ExtendableEventInit {
    var data: Any?
        get() = noImpl
        set(value) = noImpl
    var origin: String?
        get() = noImpl
        set(value) = noImpl
    var lastEventId: String?
        get() = noImpl
        set(value) = noImpl
    var source: UnionClientOrMessagePortOrServiceWorker?
        get() = noImpl
        set(value) = noImpl
    var ports: Array<MessagePort>?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun ExtendableMessageEventInit(data: Any?, origin: String?, lastEventId: String?, source: UnionClientOrMessagePortOrServiceWorker?, ports: Array<MessagePort>?, bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): ExtendableMessageEventInit {
    val o = js("({})")

    o["data"] = data
    o["origin"] = origin
    o["lastEventId"] = lastEventId
    o["source"] = source
    o["ports"] = ports
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

public external abstract class Cache {
    fun match(request: dynamic, options: CacheQueryOptions = noImpl): dynamic
    fun matchAll(request: dynamic = noImpl, options: CacheQueryOptions = noImpl): dynamic
    fun add(request: dynamic): dynamic
    fun addAll(requests: Array<dynamic>): dynamic
    fun put(request: dynamic, response: Response): dynamic
    fun delete(request: dynamic, options: CacheQueryOptions = noImpl): dynamic
    fun keys(request: dynamic = noImpl, options: CacheQueryOptions = noImpl): dynamic
}

public external interface CacheQueryOptions {
    var ignoreSearch: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var ignoreMethod: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var ignoreVary: Boolean? /* = false */
        get() = noImpl
        set(value) = noImpl
    var cacheName: String?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CacheQueryOptions(ignoreSearch: Boolean? = false, ignoreMethod: Boolean? = false, ignoreVary: Boolean? = false, cacheName: String?): CacheQueryOptions {
    val o = js("({})")

    o["ignoreSearch"] = ignoreSearch
    o["ignoreMethod"] = ignoreMethod
    o["ignoreVary"] = ignoreVary
    o["cacheName"] = cacheName

    return o
}

public external interface CacheBatchOperation {
    var type: String?
        get() = noImpl
        set(value) = noImpl
    var request: Request?
        get() = noImpl
        set(value) = noImpl
    var response: Response?
        get() = noImpl
        set(value) = noImpl
    var options: CacheQueryOptions?
        get() = noImpl
        set(value) = noImpl
}

@Suppress("NOTHING_TO_INLINE")
public inline fun CacheBatchOperation(type: String?, request: Request?, response: Response?, options: CacheQueryOptions?): CacheBatchOperation {
    val o = js("({})")

    o["type"] = type
    o["request"] = request
    o["response"] = response
    o["options"] = options

    return o
}

public external abstract class CacheStorage {
    fun match(request: dynamic, options: CacheQueryOptions = noImpl): dynamic
    fun has(cacheName: String): dynamic
    fun open(cacheName: String): dynamic
    fun delete(cacheName: String): dynamic
    fun keys(): dynamic
}

public external open class FunctionalEvent : ExtendableEvent {
}

public external @marker interface UnionClientOrMessagePortOrServiceWorker {
}

