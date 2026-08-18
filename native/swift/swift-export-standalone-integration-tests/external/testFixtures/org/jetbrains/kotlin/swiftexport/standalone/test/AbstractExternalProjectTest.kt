/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.util.mapToSet
import org.jetbrains.kotlin.konan.test.testLibraryAKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFuCinteropInteropKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryAtomicFuKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxCoroutinesKlibFile
import org.jetbrains.kotlin.konan.test.testLibraryKotlinxSerializationCoreKlibFile
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleConfig
import org.jetbrains.kotlin.swiftexport.standalone.config.SwiftModuleExportMode
import org.jetbrains.kotlin.swiftexport.standalone.runSwiftExport
import org.junit.jupiter.api.Test
import java.io.File

// Uses external project klibs prebuilt for macos_arm64 only on the CI agents.
@EnabledOnNativeTargets(targets = ["macos_arm64"])
abstract class AbstractExternalProjectTest : AbstractSwiftExportTest() {

    @Test
    fun `full export of testLibraryA`() {
        val klibSettings = KlibExportSettings(
            testLibraryAKlibFile,
            targets.testTarget,
            "testLibraryA",
        )
        runTest(klibSettings, "testLibraryA_full_dump")
    }

    @Test
    fun `kotlinx-serialization-core`() {
        val klibSettings = KlibExportSettings(
            testLibraryKotlinxSerializationCoreKlibFile,
            targets.testTarget,
            "KotlinSerialization",
            "kotlinx.serialization",
        )
        runTest(klibSettings, "kotlinx-serialization-core")
    }

    @Test
    fun `kotlinx-coroutines-core`() {
        minOSVersion = "15.0"
        val atomicFuCinterop = KlibExportSettings(
            testLibraryAtomicFuCinteropInteropKlibFile,
            targets.testTarget,
            "KotlinxAtomicFuCinterop",
            "kotlinx.atomicfu",
        )
        val atomicFu = KlibExportSettings(
            testLibraryAtomicFuKlibFile,
            targets.testTarget,
            "KotlinxAtomicFu",
            "kotlinx.atomicfu",
            setOf(atomicFuCinterop),
        )
        val coroutines = KlibExportSettings(
            testLibraryKotlinxCoroutinesKlibFile,
            targets.testTarget,
            "KotlinxCoroutinesCore",
            "kotlinx.coroutines",
            setOf(atomicFu, atomicFuCinterop)
        )
        runTest(coroutines, "kotlinx-coroutines-core")
    }

    private val tmpdir = FileUtil.createTempDirectory("SwiftExportIntegrationTests", null, false)

    private fun runTest(
        klib: KlibExportSettings,
        goldenData: String,
    ) {
        val klibs = mutableSetOf<KlibExportSettings>()
        fun collectKlibs(klib: KlibExportSettings) {
            klibs.add(klib)
            klib.dependencies.forEach(::collectKlibs)
        }
        collectKlibs(klib)

        val testPathFull = testDataDir.resolve(goldenData)
        val config = klib.createConfig(outputPath = tmpdir.toPath().resolve(klib.swiftModuleName))
        val modules = klibs.mapToSet {
            val exportMode = if (it == klib) SwiftModuleExportMode.Full else SwiftModuleExportMode.Transitive
            val config = SwiftModuleConfig(rootPackage = it.rootPackage, exportMode = exportMode)
            it.createInputModule(config)
        }

        val swiftExportOutputs = runSwiftExport(modules, config).getOrThrow()

        val testModules = mutableMapOf<KlibExportSettings, TestModule.Given>()
        fun createTestModule(klib: KlibExportSettings): TestModule.Given = testModules.getOrPut(klib) {
            val dependencies = klib.dependencies.mapToSet(::createTestModule)
            TestModule.Given(klib.path.toFile(), dependencies)
        }
        createTestModule(klib)

        runTest(testModules.values.toSet(), testPathFull, swiftExportOutputs)
    }

    protected abstract fun runTest(
        modules: Set<TestModule.Given>,
        testPathFull: File,
        swiftExportOutputs: Set<SwiftExportModule>,
    )
}

private val testDataDir = ForTestCompileRuntime.transformTestDataPath("native/swift/swift-export-standalone-integration-tests/external/testData/generation")
