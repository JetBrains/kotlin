/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestStructureExtractor
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCScheduler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.util.LAUNCHER_FILE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.util.generateBoxFunctionLauncher
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("partial-linkage")
@UsePartialLinkage(UsePartialLinkage.Mode.DEFAULT)
abstract class AbstractNativePartialLinkageTest : AbstractNativeCompilerInvocationTest() {

    // The entry point to generated test classes.
    protected fun runTest(@TestDataFile testDir: String) {
        // KT-70162: Partial Linkage tests take a lot of time when aggressive scheduler is enabled.
        // There is no major profit from running these tests with this scheduler. On the other hand,
        // we have to significantly increase timeouts to make such configurations pass.
        // So let's just disable them instead of wasting CI times.
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE)

        val configuration = NativeCompilerInvocationTestConfiguration(testRunSettings)

        KlibCompilerInvocationTestUtils.runTest(
            testStructure = NativePartialLinkageTestStructureExtractor(testRunSettings).extractTestStructure(getAbsoluteFile(testDir)),
            testConfiguration = configuration,
            artifactBuilder = NativeCompilerInvocationTestArtifactBuilder(configuration),
            binaryRunner = this,
            compilerEditionChange = KlibCompilerChangeScenario.NoChange,
        )
    }
}

private class NativePartialLinkageTestStructureExtractor(private val settings: Settings) : PartialLinkageTestStructureExtractor() {
    override val buildDir: File
        get() = settings.get<Binaries>().testBinariesDir

    override val testModeConstructorParameters = buildMap {
        this["isNative"] = "true"

        val cacheMode = settings.get<CacheMode>()
        when {
            cacheMode.useStaticCacheForUserLibraries -> this["staticCache"] = "TestMode.Scope.EVERYWHERE"
            cacheMode.useStaticCacheForDistributionLibraries -> this["staticCache"] = "TestMode.Scope.DISTRIBUTION"
        }
    }

    override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
        if (moduleName == KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME) {
            // Add a "box" function launcher to the main module.
            moduleSourceDir.resolve(LAUNCHER_FILE_NAME).writeText(generateBoxFunctionLauncher("box"))
        }
    }
}
