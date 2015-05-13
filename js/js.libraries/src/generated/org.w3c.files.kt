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
import org.w3c.fetch.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public open class Blob : ImageBitmapSource {
    var size: Long
        get() = noImpl
        set(value) = noImpl
    var type: String
        get() = noImpl
        set(value) = noImpl
    var isClosed: Boolean
        get() = noImpl
        set(value) = noImpl
    fun slice(start: Long = noImpl, end: Long = noImpl, contentType: String = noImpl): Blob = noImpl
    fun close(): Unit = noImpl
}

native public open class BlobPropertyBag {
    var type: String = ""
}

native public open class File(fileBits: Array<dynamic>, fileName: String, options: FilePropertyBag = noImpl) : Blob() {
    var name: String
        get() = noImpl
        set(value) = noImpl
    var lastModified: Long
        get() = noImpl
        set(value) = noImpl
}

native public open class FilePropertyBag {
    var type: String = ""
    var lastModified: Long
}

native public trait FileList {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    fun item(index: Int): File? = noImpl
    nativeGetter fun get(index: Int): File? = noImpl
}

native public open class FileReader : EventTarget {
    var readyState: Short
        get() = noImpl
        set(value) = noImpl
    var result: dynamic
        get() = noImpl
        set(value) = noImpl
    var error: dynamic
        get() = noImpl
        set(value) = noImpl
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

native public open class FileReaderSync {
    fun readAsArrayBuffer(blob: Blob): ArrayBuffer = noImpl
    fun readAsText(blob: Blob, label: String = noImpl): String = noImpl
    fun readAsDataURL(blob: Blob): String = noImpl
}

native public trait URL {
}

