/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator

class NativeFirstStageWithBackendSettingsEnvironmentConfigurator(testServices: TestServices) :
    NativeFirstStageEnvironmentConfigurator(testServices) {

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        super.configureCompilerConfiguration(configuration, module)
        // Proper setting of konanTarget is required by KlibPlatformChecker::Native.check()
        // during `createFirstStageCompilationConfig()` -> `loadNativeKlibs()` -> ... -> `KlibLoaderImpl.loadSingleLibrary()`
        // Note: for 2nd compilation stage, konanTarget is set in SetupConfigurationKt.setupFromArguments()
        configuration.konanTarget = testServices.testRunSettings.get<KotlinNativeTargets>().testTarget.name
    }
}
