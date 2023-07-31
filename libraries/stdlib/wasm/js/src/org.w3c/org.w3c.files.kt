/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See github.com/kotlin/dukat for details

package org.w3c.files

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.xhr.*

/**
 * Exposes the JavaScript [Blob](https://developer.mozilla.org/en/docs/Web/API/Blob) to Kotlin
 */
public external open class Blob(blobParts: JsArray<JsAny? /* BufferSource|Blob|String */> = definedExternally, options: BlobPropertyBag = definedExternally) : MediaProvider, ImageBitmapSource, JsAny {
    open val size: JsNumber
    open val type: String
    open val isClosed: Boolean
    fun slice(start: Int = definedExternally, end: Int = definedExternally, contentType: String = definedExternally): Blob
    fun close()
}

public external interface BlobPropertyBag : JsAny {
    var type: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun BlobPropertyBag(type: String? = ""): BlobPropertyBag { js("return { type };") }

/**
 * Exposes the JavaScript [File](https://developer.mozilla.org/en/docs/Web/API/File) to Kotlin
 */
public external open class File(fileBits: JsArray<JsAny? /* BufferSource|Blob|String */>, fileName: String, options: FilePropertyBag = definedExternally) : Blob, JsAny {
    open val name: String
    open val lastModified: Int
}

public external interface FilePropertyBag : BlobPropertyBag, JsAny {
    var lastModified: Int?
        get() = definedExternally
        set(value) = definedExternally
}

@Suppress("UNUSED_PARAMETER")
public fun FilePropertyBag(lastModified: Int? = undefined, type: String? = ""): FilePropertyBag { js("return { lastModified, type };") }

/**
 * Exposes the JavaScript [FileList](https://developer.mozilla.org/en/docs/Web/API/FileList) to Kotlin
 */
public external abstract class FileList : ItemArrayLike<File>, JsAny {
    override fun item(index: Int): File?
}

@Suppress("UNUSED_PARAMETER")
internal fun getMethodImplForFileList(obj: FileList, index: Int): File? { js("return obj[index];") }

public operator fun FileList.get(index: Int): File? = getMethodImplForFileList(this, index)

/**
 * Exposes the JavaScript [FileReader](https://developer.mozilla.org/en/docs/Web/API/FileReader) to Kotlin
 */
public external open class FileReader : EventTarget, JsAny {
    open val readyState: Short
    open val result: JsAny? /* String|ArrayBuffer */
    open val error: JsAny?
    var onloadstart: ((ProgressEvent) -> Unit)?
    var onprogress: ((ProgressEvent) -> Unit)?
    var onload: ((Event) -> Unit)?
    var onabort: ((Event) -> Unit)?
    var onerror: ((Event) -> Unit)?
    var onloadend: ((Event) -> Unit)?
    fun readAsArrayBuffer(blob: Blob)
    fun readAsBinaryString(blob: Blob)
    fun readAsText(blob: Blob, label: String = definedExternally)
    fun readAsDataURL(blob: Blob)
    fun abort()

    companion object {
        val EMPTY: Short
        val LOADING: Short
        val DONE: Short
    }
}

/**
 * Exposes the JavaScript [FileReaderSync](https://developer.mozilla.org/en/docs/Web/API/FileReaderSync) to Kotlin
 */
public external open class FileReaderSync : JsAny {
    fun readAsArrayBuffer(blob: Blob): ArrayBuffer
    fun readAsBinaryString(blob: Blob): String
    fun readAsText(blob: Blob, label: String = definedExternally): String
    fun readAsDataURL(blob: Blob): String
}