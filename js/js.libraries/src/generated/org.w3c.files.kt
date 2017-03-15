/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.w3c.files

import kotlin.js.*
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

public external open class Blob(blobParts: Array<dynamic> = definedExternally, options: BlobPropertyBag = definedExternally) {
    open val size: Int
    open val type: String
    open val isClosed: Boolean
    fun slice(start: Int = definedExternally, end: Int = definedExternally, contentType: String = definedExternally): Blob
    fun close(): Unit
}

public external interface BlobPropertyBag {
    var type: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun BlobPropertyBag(type: String? = ""): BlobPropertyBag {
    val o = js("({})")

    o["type"] = type

    return o
}

public external open class File(fileBits: Array<dynamic>, fileName: String, options: FilePropertyBag = definedExternally) : Blob {
    open val name: String
    open val lastModified: Int
}

public external interface FilePropertyBag : BlobPropertyBag {
    var lastModified: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun FilePropertyBag(lastModified: Int? = null, type: String? = ""): FilePropertyBag {
    val o = js("({})")

    o["lastModified"] = lastModified
    o["type"] = type

    return o
}

public external abstract class FileList : ItemArrayLike<File> {
    override val length: Int
    override fun item(index: Int): File?
}
@kotlin.internal.InlineOnly inline operator fun FileList.get(index: Int): File? = asDynamic()[index]

public external open class FileReader : EventTarget {
    open val readyState: Short
    open val result: dynamic
    open val error: dynamic
    var onloadstart: ((Event) -> dynamic)?
    var onprogress: ((Event) -> dynamic)?
    var onload: ((Event) -> dynamic)?
    var onabort: ((Event) -> dynamic)?
    var onerror: ((Event) -> dynamic)?
    var onloadend: ((Event) -> dynamic)?
    fun readAsArrayBuffer(blob: Blob): Unit
    fun readAsBinaryString(blob: Blob): Unit
    fun readAsText(blob: Blob, label: String = definedExternally): Unit
    fun readAsDataURL(blob: Blob): Unit
    fun abort(): Unit

    companion object {
        val EMPTY: Short
        val LOADING: Short
        val DONE: Short
    }
}

public external open class FileReaderSync {
    fun readAsArrayBuffer(blob: Blob): ArrayBuffer
    fun readAsBinaryString(blob: Blob): String
    fun readAsText(blob: Blob, label: String = definedExternally): String
    fun readAsDataURL(blob: Blob): String
}

