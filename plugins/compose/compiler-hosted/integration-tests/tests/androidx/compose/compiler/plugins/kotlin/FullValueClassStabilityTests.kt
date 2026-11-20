/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.FqNameMatcher
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.analysis.normalize
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtensionsForTest
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.jvm.abi.JvmAbiComponentRegistrar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FullValueClassStabilityTests : AbstractIrTransformTest() {
    override fun CompilerConfiguration.updateConfiguration() {
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersionSettings.languageVersion,
            apiVersion = languageVersionSettings.apiVersion,
            analysisFlags = mapOf(AnalysisFlags.skipPrereleaseCheck to true),
            specificFeatures = mapOf(LanguageFeature.FullValueClasses to LanguageFeature.State.ENABLED),
        )
    }

    @TempDir
    lateinit var libraryDirectory: File

    // Regression test for KT-89957
    @Test
    fun testStableProperties() = assertStability("value class V(val a: Int, val b: String)", "Stable")

    @Test
    fun testUnstableProperty() = assertStability("value class V(val a: Int, val b: Unstable)", "Unstable")

    // Regression test for KT-89957
    @Test
    fun testUncertainProperty() = assertStability("value class V(val a: Int, val b: List<Int>)", "Uncertain(List)")

    // Regression test for KT-89957
    @Test
    fun testSingleUncertainProperty() = assertStability("value class V(val b: MutableList<Int>)", "Uncertain(MutableList)")

    @Test
    fun testTypeParameterProperty() = assertStability("value class V<T>(val a: Int, val t: T)", "Parameter(T)")

    // Regression test for KT-89958
    @Test
    fun testStableTypeArgument() = assertStability(
        """
            value class V<T>(val t: T, val n: Int)
            value class W(val v: V<Int>)
        """,
        "Stable",
    )

    // Regression test for KT-89958
    @Test
    fun testUncertainTypeArgument() = assertStability(
        """
            value class V<T>(val t: T, val n: Int)
            value class W(val v: V<List<Int>>)
        """,
        "Uncertain(List)",
    )

    // Regression test for KT-89958
    @Test
    fun testInlineClassTypeArgument() = assertStability(
        """
            @JvmInline value class I<T>(val t: T)
            value class W(val i: I<Int>)
        """,
        "Stable",
    )

    // Regression test for KT-89968
    @Test
    fun testAbstractValueClass() = assertStability("abstract value class V", "Uncertain(V)")

    // Regression test for KT-89968
    @Test
    fun testSealedValueClass() = assertStability("sealed value class V", "Uncertain(V)")

    // Regression test for KT-89969
    @Test
    fun testConfiguredStableValueClass() = assertStability("value class V(val u: Unstable)", "Stable", externalTypes = setOf("V"))

    // Regression test for KT-89969
    @Test
    fun testConfiguredStableInlineClass() = assertStability(
        """
            @JvmInline value class V(val u: Unstable)
            class W(val v: V)
        """,
        "Stable",
        externalTypes = setOf("V"),
    )

    @Test
    fun testInheritedStableMarker() = assertStability(
        """
            @androidx.compose.runtime.Stable sealed value class Base
            value class V(val u: Unstable) : Base()
        """,
        "Stable",
    )

    @Test
    fun testInheritedStableMarkerInlineClass() = assertStability(
        """
            @androidx.compose.runtime.Stable interface Base
            @JvmInline value class V(val u: Unstable) : Base
        """,
        "Stable",
    )

    // Regression test for KT-89961
    @Test
    fun testRecursiveProperty() = assertStability("value class V(val a: Int, val next: V?)", "Unstable")

    @Test
    fun testFromOtherFile() {
        val point = SourceFile("Point.kt", "value class Point(val x: Int, val y: Int)")
        val irModule = compileToIr(listOf(point, SourceFile("Holder.kt", "class Holder(val point: Point)")))
        assertStabilityOfHolder(irModule, "Runtime(Point)")
    }

    // Regression test for KT-89995
    @Test
    fun testPrivateConstructorFromAbiJar() {
        compileLibrary(
            """
                value class Point private constructor(val x: Int, private val buffer: java.lang.StringBuilder) {
                    companion object {
                        fun of(x: Int) = Point(x, java.lang.StringBuilder())
                    }
                }
            """,
            withCompose = true,
            toAbiJar = true,
        )
        assertStabilityOfLibraryHolder("Runtime(Point)")
    }

    @Test
    fun testFromLibraryCompiledWithCompose() {
        compileLibrary("value class Point(val x: Int, val y: Int)", withCompose = true, toAbiJar = false)
        assertStabilityOfLibraryHolder("Runtime(Point)")
    }

    @Test
    fun testFromLibraryCompiledWithoutCompose() {
        compileLibrary("value class Point(val x: Int, val y: Int)", withCompose = false, toAbiJar = false)
        assertStabilityOfLibraryHolder("Unstable")
    }

    @OptIn(ExperimentalCompilerApi::class)
    private fun compileLibrary(@Language("kotlin") source: String, withCompose: Boolean, toAbiJar: Boolean) {
        val library = SourceFile("Point.kt", "package lib\n\n" + source.trimIndent())
        val outputFiles = createClassLoader(listOf(library), registerExtensions = { configuration ->
            registerExtensionsForTest(this, configuration) {
                if (withCompose) {
                    with(ComposePluginRegistrar.Companion) {
                        registerCommonExtensions()
                    }
                    IrGenerationExtension.registerExtension(ComposePluginRegistrar.createComposeIrExtension(configuration))
                }
                if (toAbiJar) {
                    with(JvmAbiComponentRegistrar { it.writeAllTo(libraryDirectory) }) {
                        registerExtensions(configuration)
                    }
                }
            }
        }).allGeneratedFiles
        if (!toAbiJar) outputFiles.writeToDir(libraryDirectory)
    }

    private fun assertStabilityOfLibraryHolder(stability: String) {
        val irModule = compileToIr(listOf(SourceFile("Holder.kt", "class Holder(val point: lib.Point)")), listOf(libraryDirectory))
        assertStabilityOfHolder(irModule, stability)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun assertStabilityOfHolder(irModule: IrModuleFragment, stability: String) {
        val holder = irModule.files.last().declarations.last() as IrClass
        val holderStability = StabilityInferencer(isTargetJvm = true, emptySet()).stabilityOf(holder.defaultType, holder.file)
        assertEquals(stability, holderStability.normalize().toString())
    }

    /**
     * Asserts that the stability of the last type declared in [classDefSrc], normalized as for the code generation, is [stability].
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun assertStability(@Language("kotlin") classDefSrc: String, stability: String, externalTypes: Set<String> = emptySet()) {
        val source = """
            class Unstable { var value: Int = 0 }

            $classDefSrc
        """.trimIndent()
        val irModule = compileToIr(listOf(SourceFile("Test.kt", source)), registerExtensions = {
            it.put(ComposeConfiguration.TEST_STABILITY_CONFIG_KEY, externalTypes)
        })
        val irClass = irModule.files.last().declarations.last() as IrClass
        val stabilityInferencer = StabilityInferencer(isTargetJvm = true, externalTypes.map { FqNameMatcher(it) }.toSet())
        val classStability = stabilityInferencer.stabilityOf(irClass.defaultType as IrType, irClass.file)
        assertEquals(stability, classStability.normalize().toString())
    }
}
