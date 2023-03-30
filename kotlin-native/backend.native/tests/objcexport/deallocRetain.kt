/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package deallocretain

open class DeallocRetainBase

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
fun garbageCollect() = kotlin.native.runtime.GC.collect()

fun createWeakReference(value: Any) = kotlin.native.ref.WeakReference(value)

fun assertNull(value: Any?) {
    kotlin.test.assertNull(value)
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun isExperimentalMM() = kotlin.native.isExperimentalMM()
