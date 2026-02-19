/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop

import kotlin.native.internal.ref.*

internal fun createStablePointer(any: Any): COpaquePointer {
    return interpretCPointer<CPointed>(createRetainedExternalRCRef(any))!!
}

internal fun disposeStablePointer(pointer: COpaquePointer) {
    val ref: ExternalRCRef = pointer.getRawValue()
    releaseExternalRCRef(ref)
    disposeExternalRCRef(ref)
}

@PublishedApi
internal fun derefStablePointer(pointer: COpaquePointer): Any {
    val ref: ExternalRCRef = pointer.getRawValue()
    return dereferenceExternalRCRef(ref)!!
}
