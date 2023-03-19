/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun BlobPropertyBag(type: kotlin.String? = ...): org.w3c.files.BlobPropertyBag
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline fun FilePropertyBag(lastModified: kotlin.Int? = ..., type: kotlin.String? = ...): org.w3c.files.FilePropertyBag
/*∆*/ 
/*∆*/ @kotlin.internal.InlineOnly
/*∆*/ public inline operator fun org.w3c.files.FileList.get(index: kotlin.Int): org.w3c.files.File?
/*∆*/ 
/*∆*/ public open external class Blob : org.w3c.dom.MediaProvider, org.w3c.dom.ImageBitmapSource {
/*∆*/     public constructor Blob(blobParts: kotlin.Array<dynamic> = ..., options: org.w3c.files.BlobPropertyBag = ...)
/*∆*/ 
/*∆*/     public open val isClosed: kotlin.Boolean { get; }
/*∆*/ 
/*∆*/     public open val size: kotlin.Number { get; }
/*∆*/ 
/*∆*/     public open val type: kotlin.String { get; }
/*∆*/ 
/*∆*/     public final fun close(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun slice(start: kotlin.Int = ..., end: kotlin.Int = ..., contentType: kotlin.String = ...): org.w3c.files.Blob
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface BlobPropertyBag {
/*∆*/     public open var type: kotlin.String? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class File : org.w3c.files.Blob {
/*∆*/     public constructor File(fileBits: kotlin.Array<dynamic>, fileName: kotlin.String, options: org.w3c.files.FilePropertyBag = ...)
/*∆*/ 
/*∆*/     public open val lastModified: kotlin.Int { get; }
/*∆*/ 
/*∆*/     public open val name: kotlin.String { get; }
/*∆*/ }
/*∆*/ 
/*∆*/ public abstract external class FileList : org.w3c.dom.ItemArrayLike<org.w3c.files.File> {
/*∆*/     public constructor FileList()
/*∆*/ 
/*∆*/     public open override fun item(index: kotlin.Int): org.w3c.files.File?
/*∆*/ }
/*∆*/ 
/*∆*/ public external interface FilePropertyBag : org.w3c.files.BlobPropertyBag {
/*∆*/     public open var lastModified: kotlin.Int? { get; set; }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class FileReader : org.w3c.dom.events.EventTarget {
/*∆*/     public constructor FileReader()
/*∆*/ 
/*∆*/     public open val error: dynamic { get; }
/*∆*/ 
/*∆*/     public final var onabort: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onload: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onloadend: ((org.w3c.dom.events.Event) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public final var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)? { get; set; }
/*∆*/ 
/*∆*/     public open val readyState: kotlin.Short { get; }
/*∆*/ 
/*∆*/     public open val result: dynamic { get; }
/*∆*/ 
/*∆*/     public final fun abort(): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun readAsArrayBuffer(blob: org.w3c.files.Blob): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun readAsBinaryString(blob: org.w3c.files.Blob): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun readAsDataURL(blob: org.w3c.files.Blob): kotlin.Unit
/*∆*/ 
/*∆*/     public final fun readAsText(blob: org.w3c.files.Blob, label: kotlin.String = ...): kotlin.Unit
/*∆*/ 
/*∆*/     public companion object of FileReader {
/*∆*/         public final val DONE: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val EMPTY: kotlin.Short { get; }
/*∆*/ 
/*∆*/         public final val LOADING: kotlin.Short { get; }
/*∆*/     }
/*∆*/ }
/*∆*/ 
/*∆*/ public open external class FileReaderSync {
/*∆*/     public constructor FileReaderSync()
/*∆*/ 
/*∆*/     public final fun readAsArrayBuffer(blob: org.w3c.files.Blob): org.khronos.webgl.ArrayBuffer
/*∆*/ 
/*∆*/     public final fun readAsBinaryString(blob: org.w3c.files.Blob): kotlin.String
/*∆*/ 
/*∆*/     public final fun readAsDataURL(blob: org.w3c.files.Blob): kotlin.String
/*∆*/ 
/*∆*/     public final fun readAsText(blob: org.w3c.files.Blob, label: kotlin.String = ...): kotlin.String
/*∆*/ }