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

public external open class Blob(blobParts: Array<dynamic> = noImpl, options: BlobPropertyBag = noImpl) {
    open val size: Int
        get() = noImpl
    open val type: String
        get() = noImpl
    open val isClosed: Boolean
        get() = noImpl
    fun slice(start: Int = noImpl, end: Int = noImpl, contentType: String = noImpl): Blob = noImpl
    fun close(): Unit = noImpl
}

public external interface BlobPropertyBag {
    var type: String? /* = "" */
}

@Suppress("NOTHING_TO_INLINE")
public inline fun BlobPropertyBag(type: String? = ""): BlobPropertyBag {
    val o = js("({})")

    o["type"] = type

    return o
}

public external open class File(fileBits: Array<dynamic>, fileName: String, options: FilePropertyBag = noImpl) : Blob(noImpl, options) {
    open val name: String
        get() = noImpl
    open val lastModified: Int
        get() = noImpl
}

public external interface FilePropertyBag : BlobPropertyBag {
    var lastModified: Int?
}

@Suppress("NOTHING_TO_INLINE")
public inline fun FilePropertyBag(lastModified: Int?, type: String? = ""): FilePropertyBag {
    val o = js("({})")

    o["lastModified"] = lastModified
    o["type"] = type

    return o
}

public external abstract class FileList {
    open val length: Int
        get() = noImpl
    fun item(index: Int): File? = noImpl
    @nativeGetter
    operator fun get(index: Int): File? = noImpl
}

public external open class FileReader : EventTarget() {
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
    fun readAsBinaryString(blob: Blob): Unit = noImpl
    fun readAsText(blob: Blob, label: String = noImpl): Unit = noImpl
    fun readAsDataURL(blob: Blob): Unit = noImpl
    fun abort(): Unit = noImpl

    companion object {
        val EMPTY: Short = 0
        val LOADING: Short = 1
        val DONE: Short = 2
    }
}

public external open class FileReaderSync {
    fun readAsArrayBuffer(blob: Blob): ArrayBuffer = noImpl
    fun readAsBinaryString(blob: Blob): String = noImpl
    fun readAsText(blob: Blob, label: String = noImpl): String = noImpl
    fun readAsDataURL(blob: Blob): String = noImpl
}

