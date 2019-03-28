/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiStatement
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.prettyDebugPrintTree
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class NewJavaToKotlinConverter(
    val project: Project,
    val settings: ConverterSettings,
    val oldConverterServices: JavaToKotlinConverterServices
) : JavaToKotlinConverter(project) {
    val converterServices = object : NewJavaToKotlinServices {
        override val oldServices = oldConverterServices
    }

    override fun createDummyKtFile(text: String, project: Project, context: PsiElement): KtFile =
        KtPsiFactory(project).createFileWithLightClassSupport("dummy.kt", text, context)

    private fun List<JKTreeElement>.prettyPrintTrees() = buildString {
        for (tree in this@prettyPrintTrees) {
            appendln()
            appendln(tree.prettyDebugPrintTree())
            appendln()
        }
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor): Result {
        val symbolProvider = JKSymbolProvider()
        symbolProvider.preBuildTree(inputElements)
        val importStorage = ImportStorage()
        val treeBuilder = JavaToJKTreeBuilder(symbolProvider, converterServices, importStorage)
        val asts = inputElements.map { element ->
            element to treeBuilder.buildTree(element)
        }

        val context = ConversionContext(
            symbolProvider,
            this,
            { it.containingFile in inputElements },
            importStorage
        )

        ConversionsRunner.doApply(asts.mapNotNull { it.second }, context)
        val results = asts.map { (element, ast) ->
            if (ast == null) return@map null
            val code = NewCodeBuilder().run { printCodeOut(ast) }
            val parseContext = when (element) {
                is PsiStatement, is PsiExpression -> ParseContext.CODE_BLOCK
                else -> ParseContext.TOP_LEVEL
            }
            ElementResult(
                code,
                importsToAdd = importStorage.getImports(),
                parseContext = parseContext
            )
        }

        return Result(results, null)
    }
}