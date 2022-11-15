/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.ARRAYBUFFER: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.BLOB: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.DOCUMENT: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.EMPTY: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.JSON: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ public val org.w3c.xhr.XMLHttpRequestResponseType.Companion.TEXT: org.w3c.xhr.XMLHttpRequestResponseType { get; }
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun ProgressEventInit(lengthComputable: kotlin.Boolean? = ..., loaded: kotlin.Number? = ..., total: kotlin.Number? = ..., bubbles: kotlin.Boolean? = ..., cancelable: kotlin.Boolean? = ..., composed: kotlin.Boolean? = ...): org.w3c.xhr.ProgressEventInit
/*∆*/ 
/*∆*/ public open external class FormData {
/*∆*/     public constructor FormData(form: org.w3c.dom.HTMLFormElement = ...)
/*∆*/ 
/*∆*/     public final fun append(name: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun append(name: kotlin.String, value: org.w3c.files.Blob, filename: kotlin.String = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun delete(name: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun get(name: kotlin.String): dynamic
/*∆*/ 
/*∆*/     public final fun getAll(name: kotlin.String): kotlin.Array<dynamic>
/*∆*/ 
/*∆*/     public final fun has(name: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun set(name: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun set(name: kotlin.String, value: org.w3c.files.Blob, filename: kotlin.String = ...): kotlin.Unit
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class ProgressEvent : org.w3c.dom.events.Event {
/*∆*/     public constructor ProgressEvent(type: kotlin.String, eventInitDict: org.w3c.xhr.ProgressEventInit = ...)
/*∆*/ 
/*∆*/     public open val lengthComputable: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val loaded: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val total: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public companion object of ProgressEvent {
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
/*∆*/ public external interface ProgressEventInit : org.w3c.dom.EventInit {
/*∆*/     public open var lengthComputable: kotlin.Boolean? { get; set; }
/*∆*/ 
/*∆*/     public open var loaded: kotlin.Number? { get; set; }
/*∆*/ 
/*∆*/     public open var total: kotlin.Number? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class XMLHttpRequest : org.w3c.xhr.XMLHttpRequestEventTarget {
/*∆*/     public constructor XMLHttpRequest()
/*∆*/ 
/*∆*/     public final var onreadystatechange: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val readyState: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val response: kotlin.Any? { get; }
/*∆*/ 
/*∆*/     public open val responseText: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final var responseType: org.w3c.xhr.XMLHttpRequestResponseType { get; set; }
/*∆*/ 
/*∆*/     public open val responseURL: kotlin.String { get; }
/*∆*/ 
/*∆*/     public open val responseXML: org.w3c.dom.Document? { get; }
/*∆*/ 
/*∆*/     public open val status: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val statusText: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final var timeout: kotlin.Int { get; set; }
/*∆*/ 
/*∆*/     public open val upload: org.w3c.xhr.XMLHttpRequestUpload { get; }
/*∆*/ 
/*∆*/     public final var withCredentials: kotlin.Boolean { get; set; }
/*∆*/ 
/*∆*/     public final fun abort(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun getAllResponseHeaders(): kotlin.String
/*∆*/ 
/*∆*/     public final fun getResponseHeader(name: kotlin.String): kotlin.String?
/*∆*/ 
/*∆*/     public final fun open(method: kotlin.String, url: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun open(method: kotlin.String, url: kotlin.String, async: kotlin.Boolean, username: kotlin.String? = ..., password: kotlin.String? = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun overrideMimeType(mime: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun send(body: dynamic = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun setRequestHeader(name: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of XMLHttpRequest {
/*∆*/         public final val DONE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val HEADERS_RECEIVED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LOADING: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val OPENED: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val UNSENT: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class XMLHttpRequestEventTarget : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor XMLHttpRequestEventTarget()
/*∆*/ 
/*∆*/     public open var onabort: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onerror: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onload: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onloadend: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open var ontimeout: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ @kotlin.js.JsName(name = "null")
/*∆*/ public external interface XMLHttpRequestResponseType {
/*∆*/     public companion object of XMLHttpRequestResponseType {
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class XMLHttpRequestUpload : org.w3c.xhr.XMLHttpRequestEventTarget {
/*∆*/     public constructor XMLHttpRequestUpload()
/*∆*/ }