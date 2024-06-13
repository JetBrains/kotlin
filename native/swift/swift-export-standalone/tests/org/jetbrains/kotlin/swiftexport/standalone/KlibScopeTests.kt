/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompilerWithoutOutputInterceptor
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.analysis.api.getAllLibraryModules
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.swiftexport.standalone.klib.KlibScope
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.writeText
import kotlin.streams.asSequence
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KlibScopeTests : AbstractNativeSimpleTest() {

    @Test
    fun `smoke single function`() {
        withKlibScope(
            source = """
                fun foo() {}
            """.trimIndent()
        ) {
            val symbol = getAllSymbols().single()
            assertTrue(symbol is KaFunctionSymbol)
            assertEquals("foo", symbol.name.asString())
        }
    }

    @Test
    fun `smoke empty file`() {
        withKlibScope(source = "") {
            val symbols = getAllSymbols()
            val classifiersNames = getPossibleClassifierNames()
            val callableNames = getPossibleCallableNames()
            assertTrue(symbols.toList().isEmpty())
            assertTrue(classifiersNames.toList().isEmpty())
            assertTrue(callableNames.toList().isEmpty())
        }
    }

    private val simpleContentWithCollisions: String = """
        fun foo(): Int = 42
        
        fun bar() {}

        interface foo {}

        interface bar {} 
    """.trimIndent()

    @Test
    fun `callable name filter`() {
        withKlibScope(source = simpleContentWithCollisions) {
            val symbol = getCallableSymbols { it.asString() == "foo" }.single()
            assertTrue(symbol is KaFunctionSymbol)
            assertEquals("foo", symbol.name.asString())
        }
    }

    @Test
    fun `classifier name filter`() {
        withKlibScope(source = simpleContentWithCollisions) {
            val symbol = getClassifierSymbols { it.asString() == "foo" }.single()
            assertTrue(symbol is KaNamedSymbol)
            assertEquals("foo", symbol.name.asString())
        }
    }

    @Test
    fun `possible classifier names`() {
        withKlibScope(source = simpleContentWithCollisions) {
            val classifierNames = getPossibleClassifierNames()
            assertContains(classifierNames, Name.identifier("foo"))
            assertContains(classifierNames, Name.identifier("bar"))
        }
    }

    @Test
    fun `possible callable names`() {
        withKlibScope(source = simpleContentWithCollisions) {
            val callableNames = getPossibleCallableNames()
            assertContains(callableNames, Name.identifier("foo"))
            assertContains(callableNames, Name.identifier("bar"))
        }
    }

    private fun <T> withKlibScope(@Language("kotlin") source: String, block: KlibScope.() -> T): T {
        val srcFile = kotlin.io.path.createTempFile(suffix = ".kt").also { it.writeText(source) }
        return withKlibScope(srcFile, block)
    }

    private fun <T> withKlibScope(sources: Path, block: KlibScope.() -> T): T {
        val klib = compileToNativeKLib(sources)
        lateinit var module: KtLibraryModule
        val session = buildStandaloneAnalysisAPISession {
            val nativePlatform = NativePlatforms.unspecifiedNativePlatform
            buildKtModuleProvider {
                platform = nativePlatform
                module = addModule(buildKtLibraryModule {
                    addBinaryRoot(klib)
                    platform = nativePlatform
                    libraryName = "testLibrary"
                })
            }
        }

        return analyze(session.getAllLibraryModules().single()) {
            KlibScope(module, this.analysisSession).block()
        }
    }
}

internal fun AbstractNativeSimpleTest.compileToNativeKLib(kLibSourcesRoot: Path): Path {
    val ktFiles = Files.walk(kLibSourcesRoot).asSequence().filter { it.extension == "kt" }.toList()
    val testKlib = KtTestUtil.tmpDir("testLibrary").resolve("library.klib").toPath()

    val arguments = buildList {
        ktFiles.mapTo(this) { it.absolutePathString() }
        addAll(listOf("-produce", "library"))
        addAll(listOf("-output", testKlib.absolutePathString()))
    }

    // Avoid creating excessive number of classloaders
    val classLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader
    val compileResult = callCompilerWithoutOutputInterceptor(arguments.toTypedArray(), classLoader)

    check(compileResult.exitCode == ExitCode.OK) {
        "Compilation error: $compileResult"
    }

    return testKlib
}
