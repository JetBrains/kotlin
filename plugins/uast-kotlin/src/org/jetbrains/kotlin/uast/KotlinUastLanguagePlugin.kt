/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.uast.lint.PropertyAsCallAndroidUastAdditionalChecker
import org.jetbrains.uast.*

object KotlinUastLanguagePlugin : UastLanguagePlugin {
    override val converter: UastConverter = KotlinConverter
    override val additionalCheckers = listOf(PropertyAsCallAndroidUastAdditionalChecker())
}

internal object KotlinConverter : UastConverter {
    override fun isFileSupported(path: String) = path.endsWith(".kt", false) || path.endsWith(".kts", false)

    override fun convert(element: Any?, parent: UElement): UElement? {
        if (element !is KtElement) return null
        return convertKtElement(element, parent)
    }

    override fun convertWithParent(element: Any?): UElement? {
        if (element !is KtElement) return null
        if (element is KtFile) return KotlinUFile(element)

        val parent = element.parent ?: return null
        val parentUElement = convertWithParent(parent) ?: return null
        return convertKtElement(element, parentUElement)
    }

    private fun convertKtElement(element: KtElement?, parent: UElement): UElement? = when (element) {
        is KtFile -> KotlinUFile(element)
        is KtAnnotationEntry -> KotlinUAnnotation(element, parent)
        is KtAnnotation -> KotlinUAnnotationList(element, parent).apply {
            annotations = element.entries.map { KotlinUAnnotation(it, this) }
        }
        is KtDeclaration -> convert(element, parent)
        is KtImportDirective -> KotlinUImportStatement(element, parent)
        is KtCatchClause -> KotlinUCatchClause(element, parent)
        is KtExpression -> KotlinConverter.convert(element, parent)
        else -> {
            if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
                asSimpleReference(element, parent)
            } else {
                null
            }
        }
    }

    internal fun convert(element: KtDeclaration, parent: UElement): UDeclaration? = when (element) {
        is KtClassOrObject -> convert(element, parent)
        is KtAnonymousInitializer -> KotlinAnonymousInitializerUFunction(element, parent)
        is KtConstructor<*> -> KotlinConstructorUFunction(element, parent)
        is KtFunction -> KotlinUFunction(element, parent)
        is KtVariableDeclaration -> KotlinUVariable(element, parent)
        is KtParameter -> convert(element, parent)
        else -> null
    }

    private fun convertStringTemplateExpression(
            expression: KtStringTemplateExpression,
            parent: UElement,
            i: Int
    ): UExpression {
        return if (i == 1) KotlinStringTemplateUBinaryExpression(expression, parent).apply {
            leftOperand = convert(expression.entries[0], this)
            rightOperand = convert(expression.entries[1], this)
        } else KotlinStringTemplateUBinaryExpression(expression, parent).apply {
            leftOperand = convertStringTemplateExpression(expression, parent, i - 1)
            rightOperand = convert(expression.entries[i], this)
        }
    }

    internal fun convert(entry: KtStringTemplateEntry, parent: UElement): UExpression = when (entry) {
        is KtStringTemplateEntryWithExpression -> convertOrEmpty(entry.expression, parent)
        else -> KotlinStringULiteralExpression(entry, parent)
    }

    internal fun convert(expression: KtExpression, parent: UElement): UExpression = when (expression) {
        is KtFunction -> convertDeclaration(expression, parent)
        is KtVariableDeclaration -> convertDeclaration(expression, parent)
        is KtClass -> convertDeclaration(expression, parent)

        is KtStringTemplateExpression -> {
            if (expression.entries.size == 1)
                convert(expression.entries[0], parent)
            else
                convertStringTemplateExpression(expression, parent, expression.entries.size - 1)
        }
        is KtDestructuringDeclaration -> KotlinUDeclarationsExpression(parent).apply {
            val tempAssignment = KotlinDestructuringUVariable(expression, this)
            val destructuringAssignments = expression.entries.mapIndexed { i, entry ->
                KotlinDestructuredUVariable(entry, this).apply {
                    initializer = KotlinUComponentQualifiedExpression(entry, this).apply {
                        receiver = KotlinStringUSimpleReferenceExpression(tempAssignment.name, this)
                        selector = KotlinUComponentFunctionCallExpression(entry, i + 1, this)
                    }
                }
            }
            declarations = listOf(tempAssignment) + destructuringAssignments
        }
        is KtStringTemplateEntry -> convertOrEmpty(expression.expression, parent)
        is KtDotQualifiedExpression -> KotlinUQualifiedExpression(expression, parent)
        is KtSafeQualifiedExpression -> KotlinUSafeQualifiedExpression(expression, parent)
        is KtSimpleNameExpression -> KotlinUSimpleReferenceExpression(expression, expression.getReferencedName(), parent)
        is KtCallExpression -> KotlinUFunctionCallExpression(expression, parent)
        is KtBinaryExpression -> KotlinUBinaryExpression(expression, parent)
        is KtParenthesizedExpression -> KotlinUParenthesizedExpression(expression, parent)
        is KtPrefixExpression -> KotlinUPrefixExpression(expression, parent)
        is KtPostfixExpression -> KotlinUPostfixExpression(expression, parent)
        is KtThisExpression -> KotlinUThisExpression(expression, parent)
        is KtSuperExpression -> KotlinUSuperExpression(expression, parent)
        is KtIsExpression -> KotlinUTypeCheckExpression(expression, parent)
        is KtIfExpression -> KotlinUIfExpression(expression, parent)
        is KtWhileExpression -> KotlinUWhileExpression(expression, parent)
        is KtDoWhileExpression -> KotlinUDoWhileExpression(expression, parent)
        is KtForExpression -> KotlinUForEachExpression(expression, parent)
        is KtWhenExpression -> KotlinUSwitchExpression(expression, parent)
        is KtBreakExpression -> KotlinUSpecialExpressionList.Empty(expression, UastSpecialExpressionKind.BREAK, parent)
        is KtContinueExpression -> KotlinUSpecialExpressionList.Empty(expression, UastSpecialExpressionKind.CONTINUE, parent)
        is KtReturnExpression -> KotlinUSpecialExpressionList(expression, UastSpecialExpressionKind.RETURN, parent).apply {
            expressions = listOf(convertOrEmpty(expression.returnedExpression, this))
        }
        is KtThrowExpression -> KotlinUSpecialExpressionList(expression, UastSpecialExpressionKind.THROW, parent).apply {
            expressions = singletonListOrEmpty(convertOrNull(expression.thrownExpression, this))
        }
        is KtBlockExpression -> KotlinUBlockExpression(expression, parent)
        is KtConstantExpression -> KotlinULiteralExpression(expression, parent)
        is KtTryExpression -> KotlinUTryExpression(expression, parent)
        is KtArrayAccessExpression -> KotlinUArrayAccessExpression(expression, parent)
        is KtLambdaExpression -> KotlinULambdaExpression(expression, parent)
        is KtBinaryExpressionWithTypeRHS -> KotlinUBinaryExpressionWithType(expression, parent)

        else -> UnknownKotlinExpression(expression, parent)
    }

    internal fun convert(element: KtParameter, parent: UElement) : UVariable {
        return KotlinParameterUVariable(element, parent)
    }

    internal fun convert(element: KtClassOrObject, parent: UElement) : UClass {
        return KotlinUClass(element, parent)
    }

    internal fun convert(element: KotlinType, project: Project, parent: UElement?): UType {
        return KotlinUType(element, project, parent)
    }

    internal fun convert(typeReference: KtTypeReference?, parent: UElement): UType {
        if (typeReference == null) return UastErrorType
        val bindingContext = typeReference.analyze(BodyResolveMode.PARTIAL)
        val type = bindingContext[BindingContext.TYPE, typeReference] ?: return UastErrorType
        return convert(type, typeReference.project, parent)
    }

    internal fun asSimpleReference(element: PsiElement?, parent: UElement): USimpleReferenceExpression? {
        if (element == null) return null
        return KotlinNameUSimpleReferenceExpression(element, KtPsiUtil.unquoteIdentifier(element.text), parent)
    }

    internal fun convertOrEmpty(expression: KtExpression?, parent: UElement): UExpression {
        return if (expression != null) convert(expression, parent) else EmptyExpression(parent)
    }

    internal fun convertOrNull(expression: KtExpression?, parent: UElement): UExpression? {
        return if (expression != null) convert(expression, parent) else null
    }

    private fun convertDeclaration(declaration: KtDeclaration, parent: UElement): UExpression {
        val udeclarations = mutableListOf<UElement>()
        return SimpleUDeclarationsExpression(parent, udeclarations).apply {
            convert(declaration, this)?.let { udeclarations += it }
        }
    }
}