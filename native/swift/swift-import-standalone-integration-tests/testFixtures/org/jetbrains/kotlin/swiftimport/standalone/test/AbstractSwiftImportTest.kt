/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.test

import org.jetbrains.kotlin.kir.printer.KirPrinter
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRoots
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestRunSettings
import org.jetbrains.kotlin.swiftimport.standalone.SwiftInputModule
import org.jetbrains.kotlin.swiftimport.standalone.runSwiftImport
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

/**
 * A test data directory is a single Swift module: the name of the directory is the name of the module,
 * and all `.swift` files directly inside the directory are its sources.
 */
@ExtendWith(SwiftImportTestSupport::class)
abstract class AbstractSwiftImportTest {
    lateinit var testRunSettings: TestRunSettings

    private val binariesDir get() = testRunSettings.get<Binaries>().testBinariesDir
    private val testDataBaseDir get() = testRunSettings.get<TestRoots>().baseDir
    protected fun buildDir(testPathFull: File) = binariesDir.resolve(testPathFull.relativeTo(testDataBaseDir))
    protected val targets: KotlinNativeTargets get() = testRunSettings.get()

    @BeforeEach
    fun checkHost() {
        // swiftextract is shipped only for macOS on Apple Silicon.
        Assumptions.assumeTrue(targets.hostTarget == KonanTarget.MACOS_ARM64)
    }

    protected fun importSwiftModule(testPathFull: File): SwiftImportOutput {
        val moduleName = testPathFull.name
        val kirModule = runSwiftImport(
            SwiftInputModule(name = moduleName, sources = testPathFull.swiftSources().map { it.toPath() })
        ).getOrThrow()

        val printer = KirPrinter()
        return SwiftImportOutput(
            moduleName = moduleName,
            kotlinSources = printer.print(kirModule),
            swiftBridges = printer.printSwiftBridges(kirModule),
            cHeaderBridges = printer.printCDeclarations(kirModule),
        )
    }

    protected fun File.swiftSources(): List<File> =
        listFiles { file -> file.isFile && file.extension == "swift" }!!.sortedBy { it.name }
}

class SwiftImportOutput(
    val moduleName: String,
    val kotlinSources: String,
    val swiftBridges: String,
    val cHeaderBridges: String,
)
