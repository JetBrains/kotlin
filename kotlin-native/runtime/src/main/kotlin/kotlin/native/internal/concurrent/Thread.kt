/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.concurrent

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.ref.ExternalRCRef
import kotlin.native.internal.ref.createRetainedExternalRCRef

@ExperimentalForeignApi
internal fun startThread(routine: () -> Unit) = startThreadImpl(createRetainedExternalRCRef(routine))

@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_native_concurrent_currentThreadId")
internal external fun currentThreadId(): ULong

@ExperimentalForeignApi
@GCUnsafeCall("Kotlin_native_concurrent_startThreadImpl")
private external fun startThreadImpl(routine: ExternalRCRef)
