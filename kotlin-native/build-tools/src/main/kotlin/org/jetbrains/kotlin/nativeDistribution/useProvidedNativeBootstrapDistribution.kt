/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.nativeDistribution

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.extra

internal const val PROVIDED_NATIVE_BOOTSTRAP_DISTRIBUTION_KEY = "providedNativeBootstrapDistribution"

/**
 * Provides a Kotlin/Native bootstrap distribution for the task.
 * This distribution is located within the build directory of the project and therefore can be used to run tests again without
 * modifying the global .konan directory.
 */
fun Task.useProvidedNativeBootstrapDistribution(configure: (Provider<NativeDistribution>) -> Unit) {
    @Suppress("UNCHECKED_CAST") val distribution = project.extra.get(PROVIDED_NATIVE_BOOTSTRAP_DISTRIBUTION_KEY) as Provider<NativeDistribution>
    inputs.dir(distribution.map { it.root }).withPathSensitivity(PathSensitivity.RELATIVE)
    configure(distribution)
}