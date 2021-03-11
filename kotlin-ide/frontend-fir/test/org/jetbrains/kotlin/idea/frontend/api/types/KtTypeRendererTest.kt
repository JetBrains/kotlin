/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty

class KtTypeRendererTest : KotlinLightCodeInsightFixtureTestCase() {
    private fun doTestByTypeText(
        type: String,
        expected: String,
        typeArguments: List<String> = emptyList(),
        rendererOptions: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT,
    ) {
        val typeArgumentsRendered = typeArguments
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = ">")
            .orEmpty()
        val fakeKtFile = myFixture.configureByText("file.kt", "fun ${typeArgumentsRendered}foo(): $type = 1") as KtFile
        val property = fakeKtFile.declarations.single() as KtFunction
        val renderedType = executeOnPooledThreadInReadAction {
            analyze(fakeKtFile) {
                val ktType = property.getReturnKtType()
                ktType.render(rendererOptions)
            }
        }
        assertEquals(expected, renderedType)
    }

    private fun doTestByExpression(
        expression: String,
        expected: String,
        rendererOptions: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT,
    ) {
        val fakeKtFile = myFixture.configureByText("file.kt", "val a = $expression") as KtFile
        val property = fakeKtFile.declarations.single() as KtProperty
        val renderedType = executeOnPooledThreadInReadAction {
            analyze(fakeKtFile) {
                val ktType = property.initializer?.getKtType()
                    ?: error("fake property should have initializer")
                ktType.render(rendererOptions)
            }
        }
        assertEquals(expected, renderedType)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testInt() {
        doTestByTypeText(type = "Int", expected = "kotlin.Int")
    }

    fun testList() {
        doTestByTypeText(type = "List<Int>", expected = "kotlin.collections.List<kotlin.Int>")
    }

    fun testTypeArguments() {
        doTestByTypeText(type = "Map<Int, V>", expected = "kotlin.collections.Map<kotlin.Int, V>", typeArguments = listOf("V"))
    }

    fun testNullable() {
        doTestByTypeText(
            type = "Map<List<Int?>, V?>?",
            expected = "kotlin.collections.Map<kotlin.collections.List<kotlin.Int?>, V?>?",
            typeArguments = listOf("V")
        )
    }

    fun testNoFqNames() {
        doTestByTypeText(
            type = "Map<List<Int?>, V?>?",
            expected = "Map<List<Int?>, V?>?",
            typeArguments = listOf("V"),
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFlexibleType() {
        doTestByExpression(
            expression = "java.lang.String.CASE_INSENSITIVE_ORDER",
            expected = "(Comparator<(String..String?)>..Comparator<(String..String?)>?)",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFunctionalType() {
        doTestByTypeText(
            type = "(String, Int?) -> Long",
            expected = "(String, Int?) -> Long",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testNullableFunctionalType() {
        doTestByTypeText(
            type = "((String, Int?) -> Long)?",
            expected = "((String, Int?) -> Long)?",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFunctionalTypeNoArguments() {
        doTestByTypeText(
            type = "() -> Long",
            expected = "() -> Long",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFunctionalTypeReceiver() {
        doTestByTypeText(
            type = "Int.(String) -> Long",
            expected = "Int.(String) -> Long",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFunctionalTypeUnitReturnType() {
        doTestByTypeText(
            type = "(String) -> Unit",
            expected = "(String) -> Unit",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }

    fun testFunctionalTypeRenderAsUsualClassType() {
        doTestByTypeText(
            type = "Int.(String, Long) -> Char",
            expected = "Function3<Int, String, Long, Char>",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES.copy(renderFunctionTypes = false)
        )
    }

    fun testIntersectionType() {
        doTestByExpression(
            expression = """
            run {
                val x: Any = 10
                if (x is String && x is Int) x else null
            }""".trimIndent(),
            expected = "(String?&Int?)",
            rendererOptions = KtTypeRendererOptions.SHORT_NAMES
        )
    }
}