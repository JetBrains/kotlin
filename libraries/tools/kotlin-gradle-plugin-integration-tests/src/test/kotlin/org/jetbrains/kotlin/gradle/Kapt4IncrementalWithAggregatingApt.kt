/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.junit.jupiter.api.DisplayName

@DisplayName("K2Kapt incremental tests with aggregating apt")
open class Kapt4IncrementalWithAggregatingApt : Kapt4IncrementalIT() {
    override fun TestProject.customizeProject() {
        forceKapt4()
    }
}


@DisplayName("K2Kapt incremental tests with aggregating apt with disabled precise compilation outputs backup")
class Kapt4IncrementalWithAggregatingAptAndWithoutPreciseBackup : Kapt4IncrementalWithAggregatingApt() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}