package org.w3c.dom.url

public open external class URL {
    /*primary*/ public constructor URL(/*0*/ url: kotlin.String, /*1*/ base: kotlin.String = ...)
    public final var hash: kotlin.String
        public final fun <get-hash>(): kotlin.String
        public final fun <set-hash>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var host: kotlin.String
        public final fun <get-host>(): kotlin.String
        public final fun <set-host>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var hostname: kotlin.String
        public final fun <get-hostname>(): kotlin.String
        public final fun <set-hostname>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var href: kotlin.String
        public final fun <get-href>(): kotlin.String
        public final fun <set-href>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val origin: kotlin.String
        public open fun <get-origin>(): kotlin.String
    public final var password: kotlin.String
        public final fun <get-password>(): kotlin.String
        public final fun <set-password>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var pathname: kotlin.String
        public final fun <get-pathname>(): kotlin.String
        public final fun <set-pathname>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var port: kotlin.String
        public final fun <get-port>(): kotlin.String
        public final fun <set-port>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var protocol: kotlin.String
        public final fun <get-protocol>(): kotlin.String
        public final fun <set-protocol>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public final var search: kotlin.String
        public final fun <get-search>(): kotlin.String
        public final fun <set-search>(/*0*/ <set-?>: kotlin.String): kotlin.Unit
    public open val searchParams: org.w3c.dom.url.URLSearchParams
        public open fun <get-searchParams>(): org.w3c.dom.url.URLSearchParams
    public final var username: kotlin.String
        public final fun <get-username>(): kotlin.String
        public final fun <set-username>(/*0*/ <set-?>: kotlin.String): kotlin.Unit

    public companion object Companion {
        public final fun createFor(/*0*/ blob: org.w3c.files.Blob): kotlin.String
        public final fun createObjectURL(/*0*/ mediaSource: org.w3c.dom.mediasource.MediaSource): kotlin.String
        public final fun createObjectURL(/*0*/ blob: org.w3c.files.Blob): kotlin.String
        public final fun domainToASCII(/*0*/ domain: kotlin.String): kotlin.String
        public final fun domainToUnicode(/*0*/ domain: kotlin.String): kotlin.String
        public final fun revokeObjectURL(/*0*/ url: kotlin.String): kotlin.Unit
    }
}

public open external class URLSearchParams {
    /*primary*/ public constructor URLSearchParams(/*0*/ init: dynamic = ...)
    public final fun append(/*0*/ name: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
    public final fun delete(/*0*/ name: kotlin.String): kotlin.Unit
    public final fun get(/*0*/ name: kotlin.String): kotlin.String?
    public final fun getAll(/*0*/ name: kotlin.String): kotlin.Array<kotlin.String>
    public final fun has(/*0*/ name: kotlin.String): kotlin.Boolean
    public final fun set(/*0*/ name: kotlin.String, /*1*/ value: kotlin.String): kotlin.Unit
}