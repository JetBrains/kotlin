/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

internal fun Project.isKonanIncrementalCompilationEnabled(): Boolean {
    return PropertiesProvider(this).incrementalNative ?: true
}

// Disabled by default for Linux targets because of clashing declarations, see KT-81760.
internal fun Project.isKonanIncrementalCompilationEnabled(konanTarget: KonanTarget): Boolean {
    return PropertiesProvider(this).incrementalNative ?: (konanTarget.family != Family.LINUX)
}

internal fun Project.getKonanParallelThreads(): Int {
    return PropertiesProvider(this).nativeParallelThreads ?: 4
}
