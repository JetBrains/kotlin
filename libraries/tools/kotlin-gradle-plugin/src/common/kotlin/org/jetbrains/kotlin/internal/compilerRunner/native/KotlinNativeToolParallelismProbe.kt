/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

/**
 * Hook into Kotlin/Native tool workers. Only tests set it.
 */
internal interface KotlinNativeToolParallelismProbe {
    fun workerStarted()
    fun workerFinished()
}

internal interface UsesKotlinNativeToolParallelismProbe : Task {
    @get:Internal
    val kotlinNativeToolParallelismProbe: Property<KotlinNativeToolParallelismProbe>
}
