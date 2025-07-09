/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.*
import kotlin.native.internal.ref.*

// Implementation details.

@PublishedApi
@ObsoleteWorkersApi
internal fun detachObjectGraphInternal(mode: Int, producer: () -> Any?): NativePtr {
    return createRetainedExternalRCRef(producer())
}

@PublishedApi
@ObsoleteWorkersApi
internal fun attachObjectGraphInternal(stable: NativePtr): Any? {
    val result = dereferenceExternalRCRef(stable)
    releaseExternalRCRef(stable)
    disposeExternalRCRef(stable)
    return result
}
