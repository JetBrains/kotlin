/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import org.jetbrains.benchmarksLauncher.Random

class LinkedList(scale: Int) : Workload {
    private class Elem(val x: Int, var next: Elem?)
    private var head: Elem? = null
    init {
        val size = 2 * 1024 * scale
        for (i in 0 until size) {
            head = Elem(Random.nextInt(size), head)
        }
    }

    companion object : WorkloadProvider<LinkedList> {
        override fun name(): String = "LinkedList"
        override fun allocate(scale: Int) = LinkedList(scale)
    }
}
