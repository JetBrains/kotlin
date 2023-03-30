/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package runtime.memory.cycles1

import kotlin.test.*
import kotlin.native.ref.*

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
@Test fun runTest() {
    // TODO: make it work in relaxed model as well.
    if (Platform.memoryModel == MemoryModel.RELAXED) return
    val weakRefToTrashCycle = createLoop()
    kotlin.native.runtime.GC.collect()
    assertNull(weakRefToTrashCycle.get())
}

private fun createLoop(): WeakReference<Any> {
    val loop = Array<Any?>(1, { null })
    loop[0] = loop

    return WeakReference(loop)
}