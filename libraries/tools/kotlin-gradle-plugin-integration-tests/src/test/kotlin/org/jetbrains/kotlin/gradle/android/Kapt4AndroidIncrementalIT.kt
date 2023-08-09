/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.forceKapt4
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.junit.jupiter.api.DisplayName

@DisplayName("android with kapt4 incremental build tests")
class Kapt4AndroidIncrementalIT : Kapt3AndroidIncrementalIT() {
    override val languageVersion: LanguageVersion
        get() = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.KOTLIN_2_0)

    override fun TestProject.customizeProject() {
        forceKapt4()
    }
}

class Kapt4AndroidIncrementalWithPreciseBackupIT : Kapt3AndroidIncrementalWithPreciseBackupIT() {
    override val languageVersion: LanguageVersion
        get() = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.KOTLIN_2_0)

    override fun TestProject.customizeProject() {
        forceKapt4()
    }
}
