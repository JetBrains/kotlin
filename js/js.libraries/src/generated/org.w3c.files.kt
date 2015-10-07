/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.w3c.files

import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

@native public open class Blob() : ImageBitmapSource {
    constructor(blobParts: Array<dynamic>, options: BlobPropertyBag = noImpl) : this()
    open val size: Int
        get() = noImpl
    open val type: String
        get() = noImpl
    open val isClosed: Boolean
        get() = noImpl
    fun slice(start: Int = noImpl, end: Int = noImpl, contentType: String = noImpl): Blob = noImpl
    fun close(): Unit = noImpl
}

@native public interface BlobPropertyBag {
    var type: String
}

@Suppress("NOTHING_TO_INLINE")
public inline fun BlobPropertyBag(type: String = ""): BlobPropertyBag {
    val o = js("({})")

    o["type"] = type

    return o
}

@native public open class File(fileBits: Array<dynamic>, fileName: String, options: FilePropertyBag = noImpl) : Blob() {
    open val name: String
        get() = noImpl
    open val lastModified: Int
        get() = noImpl
}

@native public interface FilePropertyBag {
    var type: String
    var lastModified: Int
}

@Suppress("NOTHING_TO_INLINE")
public inline fun FilePropertyBag(type: String = "", lastModified: Int): FilePropertyBag {
    val o = js("({})")

    o["type"] = type
    o["lastModified"] = lastModified

    return o
}

@native public interface FileList {
    val length: Int
        get() = noImpl
    fun item(index: Int): File? = noImpl
    operator @nativeGetter fun get(index: Int): File? = noImpl
}

@native public open class FileReader : EventTarget {
    open val readyState: Short
        get() = noImpl
    open val result: dynamic
        get() = noImpl
    open val error: dynamic
        get() = noImpl
    var onloadstart: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onprogress: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onload: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onabort: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onerror: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    var onloadend: ((Event) -> dynamic)?
        get() = noImpl
        set(value) = noImpl
    fun readAsArrayBuffer(blob: Blob): Unit = noImpl
    fun readAsText(blob: Blob, label: String = noImpl): Unit = noImpl
    fun readAsDataURL(blob: Blob): Unit = noImpl
    fun abort(): Unit = noImpl

    companion object {
        val EMPTY: Short = 0
        val LOADING: Short = 1
        val DONE: Short = 2
    }
}

@native public open class FileReaderSync {
    fun readAsArrayBuffer(blob: Blob): ArrayBuffer = noImpl
    fun readAsText(blob: Blob, label: String = noImpl): String = noImpl
    fun readAsDataURL(blob: Blob): String = noImpl
}

