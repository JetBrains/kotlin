/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.syntax.smoke

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AnalysisApiSyntaxSmokeTest {
    @Test
    fun testSimple() {
        val sourceText = $$"""
            package smoke

            class Greeter {
                /**
                 * Greets [name].
                 */
                fun greet(name: String): String = "$greeting, $name"
            }

            val greeting = "Hello"

            fun main() {
                println(Greeter().greet("world"))
            }
        """.trimIndent()

        test(sourceText) {
            assertEquals("smoke", ktFile.packageFqName.asString())
            assertEquals(listOf("Greeter", "greeting", "main"), ktFile.declarations.map { it.name })

            val greeter = ktFile.declarations.filterIsInstance<KtClass>().single()
            val greet = greeter.declarations.filterIsInstance<KtNamedFunction>().single()
            assertEquals("greet", greet.name)
            assertEquals(listOf("name"), greet.valueParameters.map { it.name })
            assertEquals("String", greet.typeReference?.text)
            assertEquals("Greets [name].", greet.docComment?.getDefaultSection()?.getContent())

            val greeting = ktFile.declarations.filterIsInstance<KtProperty>().single()
            assertEquals("\"Hello\"", greeting.initializer?.text)

            val callees = mutableListOf<String>()
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    callees += expression.calleeExpression?.text.orEmpty()
                    super.visitCallExpression(expression)
                }
            })
            assertEquals(listOf("println", "Greeter", "greet"), callees)

            val property = KtPsiFactory(project).createProperty("val answer = 42")
            assertEquals("answer", property.name)
            assertNotNull(property.initializer)
        }
    }
}

private class TestContext(val project: Project, val ktFile: KtFile)

@Suppress("UnstableApiUsage")
private fun test(@Suppress("SameParameterValue") sourceText: String, block: TestContext.() -> Unit) {
    val disposable = Disposer.newDisposable("SyntaxSmokeTest")
    try {
        val applicationEnvironment = CoreApplicationEnvironment(disposable)
        applicationEnvironment.registerFileType(KotlinFileType.INSTANCE, KotlinFileType.EXTENSION)
        applicationEnvironment.registerParserDefinition(KotlinParserDefinition())

        val project = CoreProjectEnvironment(disposable, applicationEnvironment).project

        val ktFile = PsiFileFactory.getInstance(project)
            .createFileFromText("main.kt", KotlinFileType.INSTANCE, sourceText) as KtFile

        val context = TestContext(project, ktFile)
        block(context)
    } finally {
        Disposer.dispose(disposable)
    }
}
