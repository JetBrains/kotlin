package org.w3c.xhr

public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.ARRAYBUFFER: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-ARRAYBUFFER>(): org.w3c.xhr.XMLHttpRequestResponseType
public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.BLOB: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-BLOB>(): org.w3c.xhr.XMLHttpRequestResponseType
public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.DOCUMENT: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-DOCUMENT>(): org.w3c.xhr.XMLHttpRequestResponseType
public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.EMPTY: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-EMPTY>(): org.w3c.xhr.XMLHttpRequestResponseType
public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.JSON: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-JSON>(): org.w3c.xhr.XMLHttpRequestResponseType
public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.TEXT: org.w3c.xhr.XMLHttpRequestResponseType
    public inline fun org.w3c.xhr.XMLHttpRequestResponseType.Companion.<get-TEXT>(): org.w3c.xhr.XMLHttpRequestResponseType
@kotlin.internal.InlineOnly public inline fun ProgressEventInit(/*0*/ lengthComputable: kotlin.Boolean? = ..., /*1*/ loaded: kotlin.Number? = ..., /*2*/ total: kotlin.Number? = ..., /*3*/ bubbles: kotlin.Boolean? = ..., /*4*/ cancelable: kotlin.Boolean? = ..., /*5*/ composed: kotlin.Boolean? = ...): org.w3c.xhr.ProgressEventInit

public open external class FormData {
    /*primary*/ public constructor FormData(/*0*/ form: org.w3c.dom.HTMLFormElement = ...)
    public final fun append(/*0*/ name: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
    public final fun append(/*0*/ name: kotlin.String, /*1*/ value: org.w3c.files.Blob, /*2*/ filename: kotlin.String = ...): kotlin.Unit
    public final fun delete(/*0*/ name: kotlin.String): kotlin.Unit
    public final fun get(/*0*/ name: kotlin.String): dynamic
    public final fun getAll(/*0*/ name: kotlin.String): kotlin.Array<dynamic>
    public final fun has(/*0*/ name: kotlin.String): kotlin.Boolean
    public final fun set(/*0*/ name: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
    public final fun set(/*0*/ name: kotlin.String, /*1*/ value: org.w3c.files.Blob, /*2*/ filename: kotlin.String = ...): kotlin.Unit
}

public open external class ProgressEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor ProgressEvent(/*0*/ type: kotlin.String, /*1*/ eventInitDict: org.w3c.xhr.ProgressEventInit = ...)
    public open val lengthComputable: kotlin.Boolean
        public open fun <get-lengthComputable>(): kotlin.Boolean
    public open val loaded: kotlin.Number
        public open fun <get-loaded>(): kotlin.Number
    public open val total: kotlin.Number
        public open fun <get-total>(): kotlin.Number

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

public external interface ProgressEventInit : org.w3c.dom.EventInit {
    public open var lengthComputable: kotlin.Boolean?
        public open fun <get-lengthComputable>(): kotlin.Boolean?
        public open fun <set-lengthComputable>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var loaded: kotlin.Number?
        public open fun <get-loaded>(): kotlin.Number?
        public open fun <set-loaded>(/*0*/ value: kotlin.Number?): kotlin.Unit
    public open var total: kotlin.Number?
        public open fun <get-total>(): kotlin.Number?
        public open fun <set-total>(/*0*/ value: kotlin.Number?): kotlin.Unit
}

public open external class XMLHttpRequest : org.w3c.xhr.XMLHttpRequestEventTarget {
    /*primary*/ public constructor XMLHttpRequest()
    public final var onreadystatechange: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onreadystatechange>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onreadystatechange>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open val readyState: kotlin.Short
        public open fun <get-readyState>(): kotlin.Short
    public open val response: kotlin.Any?
        public open fun <get-response>(): kotlin.Any?
    public open val responseText: kotlin.String
        public open fun <get-responseText>(): kotlin.String
    public final var responseType: org.w3c.xhr.XMLHttpRequestResponseType
        public final fun <get-responseType>(): org.w3c.xhr.XMLHttpRequestResponseType
        public final fun <set-responseType>(/*0*/ <set-?>: org.w3c.xhr.XMLHttpRequestResponseType): kotlin.Unit
    public open val responseURL: kotlin.String
        public open fun <get-responseURL>(): kotlin.String
    public open val responseXML: org.w3c.dom.Document?
        public open fun <get-responseXML>(): org.w3c.dom.Document?
    public open val status: kotlin.Short
        public open fun <get-status>(): kotlin.Short
    public open val statusText: kotlin.String
        public open fun <get-statusText>(): kotlin.String
    public final var timeout: kotlin.Int
        public final fun <get-timeout>(): kotlin.Int
        public final fun <set-timeout>(/*0*/ <set-?>: kotlin.Int): kotlin.Unit
    public open val upload: org.w3c.xhr.XMLHttpRequestUpload
        public open fun <get-upload>(): org.w3c.xhr.XMLHttpRequestUpload
    public final var withCredentials: kotlin.Boolean
        public final fun <get-withCredentials>(): kotlin.Boolean
        public final fun <set-withCredentials>(/*0*/ <set-?>: kotlin.Boolean): kotlin.Unit
    public final fun abort(): kotlin.Unit
    public final fun getAllResponseHeaders(): kotlin.String
    public final fun getResponseHeader(/*0*/ name: kotlin.String): kotlin.String?
    public final fun open(/*0*/ method: kotlin.String, /*1*/ url: kotlin.String): kotlin.Unit
    public final fun open(/*0*/ method: kotlin.String, /*1*/ url: kotlin.String, /*2*/ async: kotlin.Boolean, /*3*/ username: kotlin.String? = ..., /*4*/ password: kotlin.String? = ...): kotlin.Unit
    public final fun overrideMimeType(/*0*/ mime: kotlin.String): kotlin.Unit
    public final fun send(/*0*/ body: dynamic = ...): kotlin.Unit
    public final fun setRequestHeader(/*0*/ name: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final val DONE: kotlin.Short
            public final fun <get-DONE>(): kotlin.Short
        public final val HEADERS_RECEIVED: kotlin.Short
            public final fun <get-HEADERS_RECEIVED>(): kotlin.Short
        public final val LOADING: kotlin.Short
            public final fun <get-LOADING>(): kotlin.Short
        public final val OPENED: kotlin.Short
            public final fun <get-OPENED>(): kotlin.Short
        public final val UNSENT: kotlin.Short
            public final fun <get-UNSENT>(): kotlin.Short
    }
}

public abstract external class XMLHttpRequestEventTarget : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor XMLHttpRequestEventTarget()
    public open var onabort: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onabort>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onabort>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onload: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onload>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onload>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadend: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-onloadend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-onloadend>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public open var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <get-onloadstart>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <set-onloadstart>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <get-onprogress>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public open fun <set-onprogress>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open var ontimeout: ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <get-ontimeout>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public open fun <set-ontimeout>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
}

public external interface XMLHttpRequestResponseType {

    public companion object Companion {
    }
}

public abstract external class XMLHttpRequestUpload : org.w3c.xhr.XMLHttpRequestEventTarget {
    /*primary*/ public constructor XMLHttpRequestUpload()
}