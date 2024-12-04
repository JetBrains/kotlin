/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.junit.jupiter.api.DisplayName

@DisplayName("K2Kapt incremental tests with isolating apt")
open class Kapt4IncrementalWithIsolatingApt : Kapt4IncrementalIT() {
    override fun TestProject.customizeProject() {
        forceK2Kapt()
    }
}

@DisplayName("K2Kapt incremental tests with isolating apt with disabled precise compilation outputs backup")
class Kapt4IncrementalWithIsolatingAptAndWithoutPreciseBackup : Kapt4IncrementalWithIsolatingApt() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = false, keepIncrementalCompilationCachesInMemory = false)
}
