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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FullValueClassStabilityTests : AbstractIrTransformTest() {
    override fun CompilerConfiguration.updateConfiguration() {
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersionSettings.languageVersion,
            apiVersion = languageVersionSettings.apiVersion,
            specificFeatures = mapOf(LanguageFeature.FullValueClasses to LanguageFeature.State.ENABLED),
        )
    }

    @Test
    fun testStableProperties() = assertStability("value class V(val a: Int, val b: String)", "Stable")

    @Test
    fun testUnstableProperty() = assertStability("value class V(val a: Int, val b: Unstable)", "Unstable")

    @Test
    fun testUncertainProperty() = assertStability("value class V(val a: Int, val b: List<Int>)", "Uncertain(List)")

    @Test
    fun testSingleUncertainProperty() = assertStability("value class V(val b: MutableList<Int>)", "Uncertain(MutableList)")

    @Test
    fun testTypeParameterProperty() = assertStability("value class V<T>(val a: Int, val t: T)", "Parameter(T)")

    @Test
    fun testStableTypeArgument() = assertStability(
        "value class V<T>(val t: T, val n: Int)\nvalue class W(val v: V<Int>)",
        "Stable",
    )

    @Test
    fun testUncertainTypeArgument() = assertStability(
        "value class V<T>(val t: T, val n: Int)\nvalue class W(val v: V<List<Int>>)",
        "Uncertain(List)",
    )

    @Test
    fun testInlineClassTypeArgument() = assertStability(
        "@JvmInline value class I<T>(val t: T)\nvalue class W(val i: I<Int>)",
        "Stable",
    )

    @Test
    fun testAbstractValueClass() = assertStability("abstract value class V", "Uncertain(V)")

    @Test
    fun testSealedValueClass() = assertStability("sealed value class V", "Uncertain(V)")

    @Test
    fun testConfiguredStableValueClass() = assertStability("value class V(val u: Unstable)", "Stable", externalTypes = setOf("V"))

    @Test
    fun testConfiguredStableInlineClass() =
        assertStability("@JvmInline value class V(val u: Unstable)\nclass W(val v: V)", "Stable", externalTypes = setOf("V"))

    @Test
    fun testRecursiveProperty() = assertStability("value class V(val a: Int, val next: V?)", "Unstable")

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
