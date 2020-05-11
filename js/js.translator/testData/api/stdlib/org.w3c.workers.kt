package org.w3c.workers

public val org.w3c.workers.ServiceWorkerState.Companion.ACTIVATED: org.w3c.workers.ServiceWorkerState
    public inline fun org.w3c.workers.ServiceWorkerState.Companion.<get-ACTIVATED>(): org.w3c.workers.ServiceWorkerState
public val org.w3c.workers.ServiceWorkerState.Companion.ACTIVATING: org.w3c.workers.ServiceWorkerState
    public inline fun org.w3c.workers.ServiceWorkerState.Companion.<get-ACTIVATING>(): org.w3c.workers.ServiceWorkerState
public val org.w3c.workers.ClientType.Companion.ALL: org.w3c.workers.ClientType
    public inline fun org.w3c.workers.ClientType.Companion.<get-ALL>(): org.w3c.workers.ClientType
public val org.w3c.workers.FrameType.Companion.AUXILIARY: org.w3c.workers.FrameType
    public inline fun org.w3c.workers.FrameType.Companion.<get-AUXILIARY>(): org.w3c.workers.FrameType
public val org.w3c.workers.ServiceWorkerState.Companion.INSTALLED: org.w3c.workers.ServiceWorkerState
    public inline fun org.w3c.workers.ServiceWorkerState.Companion.<get-INSTALLED>(): org.w3c.workers.ServiceWorkerState
public val org.w3c.workers.ServiceWorkerState.Companion.INSTALLING: org.w3c.workers.ServiceWorkerState
    public inline fun org.w3c.workers.ServiceWorkerState.Companion.<get-INSTALLING>(): org.w3c.workers.ServiceWorkerState
public val org.w3c.workers.FrameType.Companion.NESTED: org.w3c.workers.FrameType
    public inline fun org.w3c.workers.FrameType.Companion.<get-NESTED>(): org.w3c.workers.FrameType
public val org.w3c.workers.FrameType.Companion.NONE: org.w3c.workers.FrameType
    public inline fun org.w3c.workers.FrameType.Companion.<get-NONE>(): org.w3c.workers.FrameType
public val org.w3c.workers.ServiceWorkerState.Companion.REDUNDANT: org.w3c.workers.ServiceWorkerState
    public inline fun org.w3c.workers.ServiceWorkerState.Companion.<get-REDUNDANT>(): org.w3c.workers.ServiceWorkerState
public val org.w3c.workers.ClientType.Companion.SHAREDWORKER: org.w3c.workers.ClientType
    public inline fun org.w3c.workers.ClientType.Companion.<get-SHAREDWORKER>(): org.w3c.workers.ClientType
public val org.w3c.workers.FrameType.Companion.TOP_LEVEL: org.w3c.workers.FrameType
    public inline fun org.w3c.workers.FrameType.Companion.<get-TOP_LEVEL>(): org.w3c.workers.FrameType
public val org.w3c.workers.ClientType.Companion.WINDOW: org.w3c.workers.ClientType
    public inline fun org.w3c.workers.ClientType.Companion.<get-WINDOW>(): org.w3c.workers.ClientType
public val org.w3c.workers.ClientType.Companion.WORKER: org.w3c.workers.ClientType
    public inline fun org.w3c.workers.ClientType.Companion.<get-WORKER>(): org.w3c.workers.ClientType
@kotlin.internal.InlineOnly public inline fun CacheBatchOperation(/*0*/ type: kotlin.String? = ..., /*1*/ request: org.w3c.fetch.Request? = ..., /*2*/ response: org.w3c.fetch.Response? = ..., /*3*/ options: org.w3c.workers.CacheQueryOptions? = ...): org.w3c.workers.CacheBatchOperation
@kotlin.internal.InlineOnly public inline fun CacheQueryOptions(/*0*/ ignoreSearch: kotlin.Boolean? = ..., /*1*/ ignoreMethod: kotlin.Boolean? = ..., /*2*/ ignoreVary: kotlin.Boolean? = ..., /*3*/ cacheName: kotlin.String? = ...): org.w3c.workers.CacheQueryOptions
@kotlin.internal.InlineOnly public inline fun ClientQueryOptions(/*0*/ includeUncontrolled: kotlin.Boolean? = ..., /*1*/ type: org.w3c.workers.ClientType? = ...): org.w3c.workers.ClientQueryOptions
@kotlin.internal.InlineOnly public inline fun ExtendableEventInit(/*0*/ bubbles: kotlin.Boolean? = ..., /*1*/ cancelable: kotlin.Boolean? = ..., /*2*/ composed: kotlin.Boolean? = ...): org.w3c.workers.ExtendableEventInit
@kotlin.internal.InlineOnly public inline fun ExtendableMessageEventInit(/*0*/ data: kotlin.Any? = ..., /*1*/ origin: kotlin.String? = ..., /*2*/ lastEventId: kotlin.String? = ..., /*3*/ source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker? = ..., /*4*/ ports: kotlin.Array<org.w3c.dom.MessagePort>? = ..., /*5*/ bubbles: kotlin.Boolean? = ..., /*6*/ cancelable: kotlin.Boolean? = ..., /*7*/ composed: kotlin.Boolean? = ...): org.w3c.workers.ExtendableMessageEventInit
@kotlin.internal.InlineOnly public inline fun FetchEventInit(/*0*/ request: org.w3c.fetch.Request?, /*1*/ clientId: kotlin.String? = ..., /*2*/ isReload: kotlin.Boolean? = ..., /*3*/ bubbles: kotlin.Boolean? = ..., /*4*/ cancelable: kotlin.Boolean? = ..., /*5*/ composed: kotlin.Boolean? = ...): org.w3c.workers.FetchEventInit
@kotlin.internal.InlineOnly public inline fun ForeignFetchEventInit(/*0*/ request: org.w3c.fetch.Request?, /*1*/ origin: kotlin.String? = ..., /*2*/ bubbles: kotlin.Boolean? = ..., /*3*/ cancelable: kotlin.Boolean? = ..., /*4*/ composed: kotlin.Boolean? = ...): org.w3c.workers.ForeignFetchEventInit
@kotlin.internal.InlineOnly public inline fun ForeignFetchOptions(/*0*/ scopes: kotlin.Array<kotlin.String>?, /*1*/ origins: kotlin.Array<kotlin.String>?): org.w3c.workers.ForeignFetchOptions
@kotlin.internal.InlineOnly public inline fun ForeignFetchResponse(/*0*/ response: org.w3c.fetch.Response?, /*1*/ origin: kotlin.String? = ..., /*2*/ headers: kotlin.Array<kotlin.String>? = ...): org.w3c.workers.ForeignFetchResponse
@kotlin.internal.InlineOnly public inline fun RegistrationOptions(/*0*/ scope: kotlin.String? = ..., /*1*/ type: org.w3c.dom.WorkerType? = ...): org.w3c.workers.RegistrationOptions
@kotlin.internal.InlineOnly public inline fun ServiceWorkerMessageEventInit(/*0*/ data: kotlin.Any? = ..., /*1*/ origin: kotlin.String? = ..., /*2*/ lastEventId: kotlin.String? = ..., /*3*/ source: org.w3c.workers.UnionMessagePortOrServiceWorker? = ..., /*4*/ ports: kotlin.Array<org.w3c.dom.MessagePort>? = ..., /*5*/ bubbles: kotlin.Boolean? = ..., /*6*/ cancelable: kotlin.Boolean? = ..., /*7*/ composed: kotlin.Boolean? = ...): org.w3c.workers.ServiceWorkerMessageEventInit

public abstract external class Cache {
    /*primary*/ public constructor Cache()
    public final fun add(/*0*/ request: dynamic): kotlin.js.Promise<kotlin.Unit>
    public final fun addAll(/*0*/ requests: kotlin.Array<dynamic>): kotlin.js.Promise<kotlin.Unit>
    public final fun delete(/*0*/ request: dynamic, /*1*/ options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Boolean>
    public final fun keys(/*0*/ request: dynamic = ..., /*1*/ options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.fetch.Request>>
    public final fun match(/*0*/ request: dynamic, /*1*/ options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Any?>
    public final fun matchAll(/*0*/ request: dynamic = ..., /*1*/ options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.fetch.Response>>
    public final fun put(/*0*/ request: dynamic, /*1*/ response: org.w3c.fetch.Response): kotlin.js.Promise<kotlin.Unit>
}

public external interface CacheBatchOperation {
    public open var options: org.w3c.workers.CacheQueryOptions?
        public open fun <get-options>(): org.w3c.workers.CacheQueryOptions?
        public open fun <set-options>(/*0*/ value: org.w3c.workers.CacheQueryOptions?): kotlin.Unit
    public open var request: org.w3c.fetch.Request?
        public open fun <get-request>(): org.w3c.fetch.Request?
        public open fun <set-request>(/*0*/ value: org.w3c.fetch.Request?): kotlin.Unit
    public open var response: org.w3c.fetch.Response?
        public open fun <get-response>(): org.w3c.fetch.Response?
        public open fun <set-response>(/*0*/ value: org.w3c.fetch.Response?): kotlin.Unit
    public open var type: kotlin.String?
        public open fun <get-type>(): kotlin.String?
        public open fun <set-type>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public external interface CacheQueryOptions {
    public open var cacheName: kotlin.String?
        public open fun <get-cacheName>(): kotlin.String?
        public open fun <set-cacheName>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var ignoreMethod: kotlin.Boolean?
        public open fun <get-ignoreMethod>(): kotlin.Boolean?
        public open fun <set-ignoreMethod>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var ignoreSearch: kotlin.Boolean?
        public open fun <get-ignoreSearch>(): kotlin.Boolean?
        public open fun <set-ignoreSearch>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var ignoreVary: kotlin.Boolean?
        public open fun <get-ignoreVary>(): kotlin.Boolean?
        public open fun <set-ignoreVary>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public abstract external class CacheStorage {
    /*primary*/ public constructor CacheStorage()
    public final fun delete(/*0*/ cacheName: kotlin.String): kotlin.js.Promise<kotlin.Boolean>
    public final fun has(/*0*/ cacheName: kotlin.String): kotlin.js.Promise<kotlin.Boolean>
    public final fun keys(): kotlin.js.Promise<kotlin.Array<kotlin.String>>
    public final fun match(/*0*/ request: dynamic, /*1*/ options: org.w3c.workers.CacheQueryOptions = ...): kotlin.js.Promise<kotlin.Any?>
    public final fun open(/*0*/ cacheName: kotlin.String): kotlin.js.Promise<org.w3c.workers.Cache>
}

public abstract external class Client : org.w3c.workers.UnionClientOrMessagePortOrServiceWorker {
    /*primary*/ public constructor Client()
    public open val frameType: org.w3c.workers.FrameType
        public open fun <get-frameType>(): org.w3c.workers.FrameType
    public open val id: kotlin.String
        public open fun <get-id>(): kotlin.String
    public open val url: kotlin.String
        public open fun <get-url>(): kotlin.String
    public final fun postMessage(/*0*/ message: kotlin.Any?, /*1*/ transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
}

public external interface ClientQueryOptions {
    public open var includeUncontrolled: kotlin.Boolean?
        public open fun <get-includeUncontrolled>(): kotlin.Boolean?
        public open fun <set-includeUncontrolled>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var type: org.w3c.workers.ClientType?
        public open fun <get-type>(): org.w3c.workers.ClientType?
        public open fun <set-type>(/*0*/ value: org.w3c.workers.ClientType?): kotlin.Unit
}

public external interface ClientType {

    public companion object Companion {
    }
}

public abstract external class Clients {
    /*primary*/ public constructor Clients()
    public final fun claim(): kotlin.js.Promise<kotlin.Unit>
    public final fun get(/*0*/ id: kotlin.String): kotlin.js.Promise<kotlin.Any?>
    public final fun matchAll(/*0*/ options: org.w3c.workers.ClientQueryOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.workers.Client>>
    public final fun openWindow(/*0*/ url: kotlin.String): kotlin.js.Promise<org.w3c.workers.WindowClient?>
}

public open external class ExtendableEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor ExtendableEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.ExtendableEventInit = ...)
    public final fun waitUntil(/*0*/ f: kotlin.js.Promise<kotlin.Any?>): kotlin.Unit

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

public external interface ExtendableEventInit : org.w3c.dom.EventInit {
}

public open external class ExtendableMessageEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor ExtendableMessageEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.ExtendableMessageEventInit = ...)
    public open val data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
    public open val lastEventId: kotlin.String
        public open fun <get-lastEventId>(): kotlin.String
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public open val ports: kotlin.Array<out org.w3c.dom.MessagePort>?
        public open fun <get-ports>(): kotlin.Array<out org.w3c.dom.MessagePort>?
    public open val source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker?
        public open fun <get-source>(): org.w3c.workers.UnionClientOrMessagePortOrServiceWorker?

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

public external interface ExtendableMessageEventInit : org.w3c.workers.ExtendableEventInit {
    public open var data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
        public open fun <set-data>(/*0*/ value: kotlin.Any?): kotlin.Unit
    public open var lastEventId: kotlin.String?
        public open fun <get-lastEventId>(): kotlin.String?
        public open fun <set-lastEventId>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var origin: kotlin.String?
        public open fun <get-origin>(): kotlin.String?
        public open fun <set-origin>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var ports: kotlin.Array<org.w3c.dom.MessagePort>?
        public open fun <get-ports>(): kotlin.Array<org.w3c.dom.MessagePort>?
        public open fun <set-ports>(/*0*/ value: kotlin.Array<org.w3c.dom.MessagePort>?): kotlin.Unit
    public open var source: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker?
        public open fun <get-source>(): org.w3c.workers.UnionClientOrMessagePortOrServiceWorker?
        public open fun <set-source>(/*0*/ value: org.w3c.workers.UnionClientOrMessagePortOrServiceWorker?): kotlin.Unit
}

public open external class FetchEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor FetchEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.FetchEventInit)
    public open val clientId: kotlin.String?
        public open fun <get-clientId>(): kotlin.String?
    public open val isReload: kotlin.Boolean
        public open fun <get-isReload>(): kotlin.Boolean
    public open val request: org.w3c.fetch.Request
        public open fun <get-request>(): org.w3c.fetch.Request
    public final fun respondWith(/*0*/ r: kotlin.js.Promise<org.w3c.fetch.Response>): kotlin.Unit

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

public external interface FetchEventInit : org.w3c.workers.ExtendableEventInit {
    public open var clientId: kotlin.String?
        public open fun <get-clientId>(): kotlin.String?
        public open fun <set-clientId>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var isReload: kotlin.Boolean?
        public open fun <get-isReload>(): kotlin.Boolean?
        public open fun <set-isReload>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var request: org.w3c.fetch.Request?
        public open fun <get-request>(): org.w3c.fetch.Request?
        public open fun <set-request>(/*0*/ value: org.w3c.fetch.Request?): kotlin.Unit
}

public open external class ForeignFetchEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor ForeignFetchEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.ForeignFetchEventInit)
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public open val request: org.w3c.fetch.Request
        public open fun <get-request>(): org.w3c.fetch.Request
    public final fun respondWith(/*0*/ r: kotlin.js.Promise<org.w3c.workers.ForeignFetchResponse>): kotlin.Unit

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

public external interface ForeignFetchEventInit : org.w3c.workers.ExtendableEventInit {
    public open var origin: kotlin.String?
        public open fun <get-origin>(): kotlin.String?
        public open fun <set-origin>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var request: org.w3c.fetch.Request?
        public open fun <get-request>(): org.w3c.fetch.Request?
        public open fun <set-request>(/*0*/ value: org.w3c.fetch.Request?): kotlin.Unit
}

public external interface ForeignFetchOptions {
    public open var origins: kotlin.Array<kotlin.String>?
        public open fun <get-origins>(): kotlin.Array<kotlin.String>?
        public open fun <set-origins>(/*0*/ value: kotlin.Array<kotlin.String>?): kotlin.Unit
    public open var scopes: kotlin.Array<kotlin.String>?
        public open fun <get-scopes>(): kotlin.Array<kotlin.String>?
        public open fun <set-scopes>(/*0*/ value: kotlin.Array<kotlin.String>?): kotlin.Unit
}

public external interface ForeignFetchResponse {
    public open var headers: kotlin.Array<kotlin.String>?
        public open fun <get-headers>(): kotlin.Array<kotlin.String>?
        public open fun <set-headers>(/*0*/ value: kotlin.Array<kotlin.String>?): kotlin.Unit
    public open var origin: kotlin.String?
        public open fun <get-origin>(): kotlin.String?
        public open fun <set-origin>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var response: org.w3c.fetch.Response?
        public open fun <get-response>(): org.w3c.fetch.Response?
        public open fun <set-response>(/*0*/ value: org.w3c.fetch.Response?): kotlin.Unit
}

public external interface FrameType {

    public companion object Companion {
    }
}

public open external class FunctionalEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor FunctionalEvent()

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

public open external class InstallEvent : org.w3c.workers.ExtendableEvent {
    /*primary*/ public constructor InstallEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.ExtendableEventInit = ...)
    public final fun registerForeignFetch(/*0*/ options: org.w3c.workers.ForeignFetchOptions): kotlin.Unit

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

public external interface RegistrationOptions {
    public open var scope: kotlin.String?
        public open fun <get-scope>(): kotlin.String?
        public open fun <set-scope>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var type: org.w3c.dom.WorkerType?
        public open fun <get-type>(): org.w3c.dom.WorkerType?
        public open fun <set-type>(/*0*/ value: org.w3c.dom.WorkerType?): kotlin.Unit
}

public abstract external class ServiceWorker : org.w3c.dom.events.EventTarget, org.w3c.dom.AbstractWorker, org.w3c.workers.UnionMessagePortOrServiceWorker, org.w3c.workers.UnionClientOrMessagePortOrServiceWorker {
    /*primary*/ public constructor ServiceWorker()
    public open var onstatechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onstatechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onstatechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val scriptURL: kotlin.String
        public open fun <get-scriptURL>(): kotlin.String
    public open val state: org.w3c.workers.ServiceWorkerState
        public open fun <get-state>(): org.w3c.workers.ServiceWorkerState
    public final fun postMessage(/*0*/ message: kotlin.Any?, /*1*/ transfer: kotlin.Array<dynamic> = ...): kotlin.Unit
}

public abstract external class ServiceWorkerContainer : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor ServiceWorkerContainer()
    public open val controller: org.w3c.workers.ServiceWorker?
        public open fun <get-controller>(): org.w3c.workers.ServiceWorker?
    public open var oncontrollerchange: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oncontrollerchange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oncontrollerchange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public open val ready: kotlin.js.Promise<org.w3c.workers.ServiceWorkerRegistration>
        public open fun <get-ready>(): kotlin.js.Promise<org.w3c.workers.ServiceWorkerRegistration>
    public final fun getRegistration(/*0*/ clientURL: kotlin.String = ...): kotlin.js.Promise<kotlin.Any?>
    public final fun getRegistrations(): kotlin.js.Promise<kotlin.Array<org.w3c.workers.ServiceWorkerRegistration>>
    public final fun register(/*0*/ scriptURL: kotlin.String, /*1*/ options: org.w3c.workers.RegistrationOptions = ...): kotlin.js.Promise<org.w3c.workers.ServiceWorkerRegistration>
    public final fun startMessages(): kotlin.Unit
}

public abstract external class ServiceWorkerGlobalScope : org.w3c.dom.WorkerGlobalScope {
    /*primary*/ public constructor ServiceWorkerGlobalScope()
    public open val clients: org.w3c.workers.Clients
        public open fun <get-clients>(): org.w3c.workers.Clients
    public open var onactivate: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onactivate>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onactivate>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onfetch: ((org.w3c.workers.FetchEvent) -> dynamic)?
        public open fun <get-onfetch>(): ((org.w3c.workers.FetchEvent) -> dynamic)?
        public open fun <set-onfetch>(/*0*/ <set-?>: ((org.w3c.workers.FetchEvent) -> dynamic)?): kotlin.Unit
    public open var onforeignfetch: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onforeignfetch>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onforeignfetch>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onfunctionalevent: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onfunctionalevent>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onfunctionalevent>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var oninstall: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-oninstall>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-oninstall>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onmessage: ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <get-onmessage>(): ((org.w3c.dom.MessageEvent) -> dynamic)?
        public open fun <set-onmessage>(/*0*/ <set-?>: ((org.w3c.dom.MessageEvent) -> dynamic)?): kotlin.Unit
    public open var onnotificationclick: ((org.w3c.notifications.NotificationEvent) -> dynamic)?
        public open fun <get-onnotificationclick>(): ((org.w3c.notifications.NotificationEvent) -> dynamic)?
        public open fun <set-onnotificationclick>(/*0*/ <set-?>: ((org.w3c.notifications.NotificationEvent) -> dynamic)?): kotlin.Unit
    public open var onnotificationclose: ((org.w3c.notifications.NotificationEvent) -> dynamic)?
        public open fun <get-onnotificationclose>(): ((org.w3c.notifications.NotificationEvent) -> dynamic)?
        public open fun <set-onnotificationclose>(/*0*/ <set-?>: ((org.w3c.notifications.NotificationEvent) -> dynamic)?): kotlin.Unit
    public open val registration: org.w3c.workers.ServiceWorkerRegistration
        public open fun <get-registration>(): org.w3c.workers.ServiceWorkerRegistration
    public final fun skipWaiting(): kotlin.js.Promise<kotlin.Unit>
}

public open external class ServiceWorkerMessageEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor ServiceWorkerMessageEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.workers.ServiceWorkerMessageEventInit = ...)
    public open val data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
    public open val lastEventId: kotlin.String
        public open fun <get-lastEventId>(): kotlin.String
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public open val ports: kotlin.Array<out org.w3c.dom.MessagePort>?
        public open fun <get-ports>(): kotlin.Array<out org.w3c.dom.MessagePort>?
    public open val source: org.w3c.workers.UnionMessagePortOrServiceWorker?
        public open fun <get-source>(): org.w3c.workers.UnionMessagePortOrServiceWorker?

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

public external interface ServiceWorkerMessageEventInit : org.w3c.dom.EventInit {
    public open var data: kotlin.Any?
        public open fun <get-data>(): kotlin.Any?
        public open fun <set-data>(/*0*/ value: kotlin.Any?): kotlin.Unit
    public open var lastEventId: kotlin.String?
        public open fun <get-lastEventId>(): kotlin.String?
        public open fun <set-lastEventId>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var origin: kotlin.String?
        public open fun <get-origin>(): kotlin.String?
        public open fun <set-origin>(/*0*/ value: kotlin.String?): kotlin.Unit
    public open var ports: kotlin.Array<org.w3c.dom.MessagePort>?
        public open fun <get-ports>(): kotlin.Array<org.w3c.dom.MessagePort>?
        public open fun <set-ports>(/*0*/ value: kotlin.Array<org.w3c.dom.MessagePort>?): kotlin.Unit
    public open var source: org.w3c.workers.UnionMessagePortOrServiceWorker?
        public open fun <get-source>(): org.w3c.workers.UnionMessagePortOrServiceWorker?
        public open fun <set-source>(/*0*/ value: org.w3c.workers.UnionMessagePortOrServiceWorker?): kotlin.Unit
}

public abstract external class ServiceWorkerRegistration : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor ServiceWorkerRegistration()
    public open val APISpace: dynamic
        public open fun <get-APISpace>(): dynamic
    public open val active: org.w3c.workers.ServiceWorker?
        public open fun <get-active>(): org.w3c.workers.ServiceWorker?
    public open val installing: org.w3c.workers.ServiceWorker?
        public open fun <get-installing>(): org.w3c.workers.ServiceWorker?
    public open var onupdatefound: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onupdatefound>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onupdatefound>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val scope: kotlin.String
        public open fun <get-scope>(): kotlin.String
    public open val waiting: org.w3c.workers.ServiceWorker?
        public open fun <get-waiting>(): org.w3c.workers.ServiceWorker?
    public final fun getNotifications(/*0*/ filter: org.w3c.notifications.GetNotificationOptions = ...): kotlin.js.Promise<kotlin.Array<org.w3c.notifications.Notification>>
    public final fun methodName(): kotlin.js.Promise<dynamic>
    public final fun showNotification(/*0*/ title: kotlin.String, /*1*/ options: org.w3c.notifications.NotificationOptions = ...): kotlin.js.Promise<kotlin.Unit>
    public final fun unregister(): kotlin.js.Promise<kotlin.Boolean>
    public final fun update(): kotlin.js.Promise<kotlin.Unit>
}

public external interface ServiceWorkerState {

    public companion object Companion {
    }
}

public external interface UnionClientOrMessagePortOrServiceWorker {
}

public external interface UnionMessagePortOrServiceWorker {
}

public abstract external class WindowClient : org.w3c.workers.Client {
    /*primary*/ public constructor WindowClient()
    public open val focused: kotlin.Boolean
        public open fun <get-focused>(): kotlin.Boolean
    public open val visibilityState: dynamic
        public open fun <get-visibilityState>(): dynamic
    public final fun focus(): kotlin.js.Promise<org.w3c.workers.WindowClient>
    public final fun navigate(/*0*/ url: kotlin.String): kotlin.js.Promise<org.w3c.workers.WindowClient>
}