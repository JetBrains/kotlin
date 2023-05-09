/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import org.jetbrains.benchmarksLauncher.Random

class ArrayWorkload(scale: Int) : Workload {
    private data class Elem(val x: Int)
    private val size = 2 * 1024 * scale
    private val data = Array(size) { Elem(Random.nextInt(size)) }

    companion object : WorkloadProvider<ArrayWorkload> {
        override fun name(): String = "Array"
        override fun allocate(scale: Int) = ArrayWorkload(scale)
    }
}
