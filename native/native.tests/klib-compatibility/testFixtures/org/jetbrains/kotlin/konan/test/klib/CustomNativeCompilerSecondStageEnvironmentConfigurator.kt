/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator

class CustomNativeCompilerSecondStageEnvironmentConfigurator(
    testServices: TestServices,
    private val nativeCompilerSettings: NativeCompilerSettings,
) : NativeEnvironmentConfigurator(testServices) {
    override val nativeHome: String by lazy {
        if (nativeCompilerSettings.defaultLanguageVersion >= LanguageVersion.LATEST_STABLE)
            super.nativeHome
        else
            nativeCompilerSettings.nativeHome.absolutePath
    }
}