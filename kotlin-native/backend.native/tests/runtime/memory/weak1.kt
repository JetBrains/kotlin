/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package runtime.memory.weak1

import kotlin.test.*
import kotlin.native.ref.*

class Node(var next: Node?)

@Test fun runTest1() {
    val node1 = Node(null)
    val node2 = Node(node1)
    node1.next = node2

    kotlin.native.ref.WeakReference(node1)
    println("OK")
}
