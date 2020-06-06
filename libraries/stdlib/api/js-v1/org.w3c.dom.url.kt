/*∆*/ public open external class URL {
/*∆*/     public constructor URL(url: kotlin.String, base: kotlin.String = ...)
/*∆*/ 
/*∆*/     public final var hash: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var host: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var hostname: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var href: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val origin: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final var password: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var pathname: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var port: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var protocol: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public final var search: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public open val searchParams: org.w3c.dom.url.URLSearchParams { get; }
/*∆*/ 
/*∆*/     public final var username: kotlin.String { get; set; }
/*∆*/ 
/*∆*/     public companion object of URL {
/*∆*/         public final fun createFor(blob: org.w3c.files.Blob): kotlin.String
/*∆*/ 
/*∆*/         public final fun createObjectURL(mediaSource: org.w3c.dom.mediasource.MediaSource): kotlin.String
/*∆*/ 
/*∆*/         public final fun createObjectURL(blob: org.w3c.files.Blob): kotlin.String
/*∆*/ 
/*∆*/         public final fun domainToASCII(domain: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/         public final fun domainToUnicode(domain: kotlin.String): kotlin.String
/*∆*/ 
/*∆*/         public final fun revokeObjectURL(url: kotlin.String): kotlin.Unit
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class URLSearchParams {
/*∆*/     public constructor URLSearchParams(init: dynamic = ...)
/*∆*/ 
/*∆*/     public final fun append(name: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun delete(name: kotlin.String): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun get(name: kotlin.String): kotlin.String?
/*∆*/ 
/*∆*/     public final fun getAll(name: kotlin.String): kotlin.Array<kotlin.String>
/*∆*/ 
/*∆*/     public final fun has(name: kotlin.String): kotlin.Boolean
/*∆*/ 
/*∆*/     public final fun set(name: kotlin.String, value: kotlin.String): kotlin.Unit
/*∆*/ }