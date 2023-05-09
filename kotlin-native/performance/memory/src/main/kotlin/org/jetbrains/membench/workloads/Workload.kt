/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.membench

val DEFAULT_SCALE = 1024

interface Workload

interface WorkloadProvider<T : Workload> {
    fun name(): String
    fun allocate(scale: Int = DEFAULT_SCALE): T
}
