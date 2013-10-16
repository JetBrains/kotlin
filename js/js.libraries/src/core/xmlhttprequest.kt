package com.jetbrains.upsource.frontend.jsrpc

import org.w3c.dom.events.EventTarget
import org.w3c.dom.Document

public class HttpMethods() {
    class object {
        public val DELETE: String = "DELETE"
        public val GET: String = "GET"
        public val HEAD: String = "HEAD"
        public val POST: String = "POST"
        public val PUT: String = "PUT"
    }
}

public class HttpResponseStatus() {
    class object {
        public val CONTINUE: Int = 100
        public val SWITCHING_PROTOCOLS: Int = 101
        public val PROCESSING: Int = 102
        public val OK: Int = 200
        public val CREATED: Int = 201
        public val ACCEPTED: Int = 202
        public val NON_AUTHORITATIVE_INFORMATION: Int = 203
        public val NO_CONTENT: Int = 204
        public val RESET_CONTENT: Int = 205
        public val PARTIAL_CONTENT: Int = 206
        public val MULTI_STATUS: Int = 207
        public val MULTIPLE_CHOICES: Int = 300
        public val MOVED_PERMANENTLY: Int = 301
        public val FOUND: Int = 302
        public val SEE_OTHER: Int = 303
        public val NOT_MODIFIED: Int = 304
        public val USE_PROXY: Int = 305
        public val TEMPORARY_REDIRECT: Int = 307
        public val BAD_REQUEST: Int = 400
        public val UNAUTHORIZED: Int = 401
        public val PAYMENT_REQUIRED: Int = 402
        public val FORBIDDEN: Int = 403
        public val NOT_FOUND: Int = 404
        public val METHOD_NOT_ALLOWED: Int = 405
        public val NOT_ACCEPTABLE: Int = 406
        public val PROXY_AUTHENTICATION_REQUIRED: Int = 407
        public val REQUEST_TIMEOUT: Int = 408
        public val CONFLICT: Int = 409
        public val GONE: Int = 410
        public val LENGTH_REQUIRED: Int = 411
        public val PRECONDITION_FAILED: Int = 412
        public val REQUEST_ENTITY_TOO_LARGE: Int = 413
        public val REQUEST_URI_TOO_LONG: Int = 414
        public val UNSUPPORTED_MEDIA_TYPE: Int = 415
        public val REQUESTED_RANGE_NOT_SATISFIABLE: Int = 416
        public val EXPECTATION_FAILED: Int = 417
        public val UNPROCESSABLE_ENTITY: Int = 422
        public val LOCKED: Int = 423
        public val FAILED_DEPENDENCY: Int = 424
        public val UNORDERED_COLLECTION: Int = 425
        public val UPGRADE_REQUIRED: Int = 426
        public val PRECONDITION_REQUIRED: Int = 428
        public val TOO_MANY_REQUESTS: Int = 429
        public val REQUEST_HEADER_FIELDS_TOO_LARGE: Int = 431
        public val INTERNAL_SERVER_ERROR: Int = 500
        public val NOT_IMPLEMENTED: Int = 501
        public val BAD_GATEWAY: Int = 502
        public val SERVICE_UNAVAILABLE: Int = 503
        public val GATEWAY_TIMEOUT: Int = 504
        public val HTTP_VERSION_NOT_SUPPORTED: Int = 505
        public val VARIANT_ALSO_NEGOTIATES: Int = 506
        public val INSUFFICIENT_STORAGE: Int = 507
        public val NOT_EXTENDED: Int = 510
        public val NETWORK_AUTHENTICATION_REQUIRED: Int = 511
    }
}

native
public class XMLHttpRequest() : EventTarget {
    public var response: String? = noImpl;
    public var status: Int = noImpl;
    public var readyState: Int = noImpl;
    public var responseText: String = noImpl;
    public var responseXML: Document = noImpl;
    public var ontimeout: (ev: Event) -> Unit = noImpl;
    public var statusText: String = noImpl;
    public var onreadystatechange: (ev: Event) -> Unit = noImpl;
    public var timeout: Int = noImpl;
    public var onload: (ev: Event) -> Unit = noImpl;
    public var responseType: String = noImpl;
    public fun open(method: String, url: String, async: Boolean = true, user: String? = null, password: String? = null): Unit = noImpl;
    public fun create(): XMLHttpRequest = noImpl;
    public fun send(data: Any? = null): Unit = noImpl;
    public fun abort(): Unit = noImpl;
    public fun getAllResponseHeaders(): String? = noImpl;
    public fun setRequestHeader(header: String, value: String): Unit = noImpl;
    public fun getResponseHeader(header: String): String = noImpl;

    public val LOADING: Int = noImpl;
    public val DONE: Int = noImpl;
    public val UNSENT: Int = noImpl;
    public val OPENED: Int = noImpl;
    public val HEADERS_RECEIVED: Int = noImpl;

    class object {
        public val LOADING: Int = noImpl;
        public val DONE: Int = noImpl;
        public val UNSENT: Int = noImpl;
        public val OPENED: Int = noImpl;
        public val HEADERS_RECEIVED: Int = noImpl;
    }
}
