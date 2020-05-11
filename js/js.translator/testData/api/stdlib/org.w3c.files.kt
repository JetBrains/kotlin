package org.w3c.files

@kotlin.internal.InlineOnly public inline fun BlobPropertyBag(/*0*/ type: kotlin.String? = ...): org.w3c.files.BlobPropertyBag
@kotlin.internal.InlineOnly public inline fun FilePropertyBag(/*0*/ lastModified: kotlin.Int? = ..., /*1*/ type: kotlin.String? = ...): org.w3c.files.FilePropertyBag
@kotlin.internal.InlineOnly public inline operator fun org.w3c.files.FileList.get(/*0*/ index: kotlin.Int): org.w3c.files.File?

public open external class Blob : org.w3c.dom.MediaProvider, org.w3c.dom.ImageBitmapSource {
    /*primary*/ public constructor Blob(/*0*/ blobParts: kotlin.Array<dynamic> = ..., /*1*/ options: org.w3c.files.BlobPropertyBag = ...)
    public open val isClosed: kotlin.Boolean
        public open fun <get-isClosed>(): kotlin.Boolean
    public open val size: kotlin.Number
        public open fun <get-size>(): kotlin.Number
    public open val type: kotlin.String
        public open fun <get-type>(): kotlin.String
    public final fun close(): kotlin.Unit
    public final fun slice(/*0*/ start: kotlin.Int = ..., /*1*/ end: kotlin.Int = ..., /*2*/ contentType: kotlin.String = ...): org.w3c.files.Blob
}

public external interface BlobPropertyBag {
    public open var type: kotlin.String?
        public open fun <get-type>(): kotlin.String?
        public open fun <set-type>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public open external class File : org.w3c.files.Blob {
    /*primary*/ public constructor File(/*0*/ fileBits: kotlin.Array<dynamic>, /*1*/ fileName: kotlin.String, /*2*/ options: org.w3c.files.FilePropertyBag = ...)
    public open val lastModified: kotlin.Int
        public open fun <get-lastModified>(): kotlin.Int
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
}

public abstract external class FileList : org.w3c.dom.ItemArrayLike<org.w3c.files.File> {
    /*primary*/ public constructor FileList()
    public open override /*1*/ fun item(/*0*/ index: kotlin.Int): org.w3c.files.File?
}

public external interface FilePropertyBag : org.w3c.files.BlobPropertyBag {
    public open var lastModified: kotlin.Int?
        public open fun <get-lastModified>(): kotlin.Int?
        public open fun <set-lastModified>(/*0*/ value: kotlin.Int?): kotlin.Unit
}

public open external class FileReader : org.w3c.dom.events.EventTarget {
    /*primary*/ public constructor FileReader()
    public open val error: dynamic
        public open fun <get-error>(): dynamic
    public final var onabort: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onabort>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onabort>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onerror: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onerror>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onerror>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onload: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onload>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onload>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onloadend: ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <get-onloadend>(): ((org.w3c.dom.events.Event) -> dynamic)?
        public final fun <set-onloadend>(/*0*/ <set-?>: ((org.w3c.dom.events.Event) -> dynamic)?): kotlin.Unit
    public final var onloadstart: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public final fun <get-onloadstart>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public final fun <set-onloadstart>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public final var onprogress: ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public final fun <get-onprogress>(): ((org.w3c.xhr.ProgressEvent) -> dynamic)?
        public final fun <set-onprogress>(/*0*/ <set-?>: ((org.w3c.xhr.ProgressEvent) -> dynamic)?): kotlin.Unit
    public open val readyState: kotlin.Short
        public open fun <get-readyState>(): kotlin.Short
    public open val result: dynamic
        public open fun <get-result>(): dynamic
    public final fun abort(): kotlin.Unit
    public final fun readAsArrayBuffer(/*0*/ blob: org.w3c.files.Blob): kotlin.Unit
    public final fun readAsBinaryString(/*0*/ blob: org.w3c.files.Blob): kotlin.Unit
    public final fun readAsDataURL(/*0*/ blob: org.w3c.files.Blob): kotlin.Unit
    public final fun readAsText(/*0*/ blob: org.w3c.files.Blob, /*1*/ label: kotlin.String = ...): kotlin.Unit

    public companion object Companion {
        public final val DONE: kotlin.Short
            public final fun <get-DONE>(): kotlin.Short
        public final val EMPTY: kotlin.Short
            public final fun <get-EMPTY>(): kotlin.Short
        public final val LOADING: kotlin.Short
            public final fun <get-LOADING>(): kotlin.Short
    }
}

public open external class FileReaderSync {
    /*primary*/ public constructor FileReaderSync()
    public final fun readAsArrayBuffer(/*0*/ blob: org.w3c.files.Blob): org.khronos.webgl.ArrayBuffer
    public final fun readAsBinaryString(/*0*/ blob: org.w3c.files.Blob): kotlin.String
    public final fun readAsDataURL(/*0*/ blob: org.w3c.files.Blob): kotlin.String
    public final fun readAsText(/*0*/ blob: org.w3c.files.Blob, /*1*/ label: kotlin.String = ...): kotlin.String
}