/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

import org.jetbrains.benchmarksLauncher.Random

class Empty : Workload {
    companion object : WorkloadProvider<Empty> {
        override fun name(): String = "Empty"
        override fun allocate(scale: Int) = Empty()
    }
}
