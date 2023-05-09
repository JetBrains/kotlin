/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.runtime.GC
import kotlin.test.*


@Test
fun `nothing new collected`() {
    GC.collect()
    GC.collect()
    val stat = GC.lastGCInfo
    assertNotNull(stat)
    for (key in stat.sweepStatistics.keys) {
        assertEquals(stat.sweepStatistics[key]!!.sweptCount, 0L)
    }
}

object Global {
    val x = listOf(1, 2, 3)
}

@Test
fun `stable refs in root set`() {
    GC.collect()
    val stat0 = GC.lastGCInfo
    assertNotNull(stat0)
    val rootSet0 = stat0.rootSet
    assertNotNull(rootSet0)
    val x = listOf(1, 2, 3)
    val stable = kotlinx.cinterop.StableRef.create(x)
    GC.collect();
    val stat1 = GC.lastGCInfo
    assertNotNull(stat1)
    val rootSet1 = stat1.rootSet
    assertNotNull(rootSet1)
    assertEquals(rootSet0.stableReferences + 1, rootSet1.stableReferences)
    stable.dispose()
    Global.x // to initialize and register global object
    GC.collect();
    val stat2 = GC.lastGCInfo
    assertNotNull(stat2)
    val rootSet2 = stat2.rootSet
    assertNotNull(rootSet2)
    assertEquals(rootSet0.stableReferences, rootSet2.stableReferences)
    assertEquals(rootSet1.globalReferences + 1, rootSet2.globalReferences)
}

@Test
fun `check everything is filled at the end`() {
    GC.collect()
    val stat = GC.lastGCInfo
    assertNotNull(stat)
    // GC.collect is waiting for finalizers, so it should bet not null
    assertNotNull(stat.postGcCleanupTimeNs)
}

