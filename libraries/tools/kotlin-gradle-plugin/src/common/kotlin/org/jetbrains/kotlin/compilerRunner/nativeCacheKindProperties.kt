/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider

internal fun Project.isKonanIncrementalCompilationEnabled(): Boolean {
    return PropertiesProvider(this).incrementalNative ?: false
}

internal fun Project.getKonanParallelThreads(): Int {
    return PropertiesProvider(this).nativeParallelThreads ?: 4
}
