/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.generate

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getPossiblyQualifiedCallExpression
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.*
import org.jetbrains.uast.generate.UParameterInfo
import org.jetbrains.uast.generate.UastCodeGenerationPlugin
import org.jetbrains.uast.generate.UastElementFactory
import org.jetbrains.uast.kotlin.*

class KotlinUastCodeGenerationPlugin : UastCodeGenerationPlugin {
    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun getElementFactory(project: Project): UastElementFactory =
        KotlinUastElementFactory(project)

    override fun <T : UElement> replace(oldElement: UElement, newElement: T, elementType: Class<T>): T? {
        val oldPsi = oldElement.sourcePsi ?: return null
        val newPsi = newElement.sourcePsi?.let {
            when {
                it is KtCallExpression && it.parent is KtQualifiedExpression -> it.parent
                else -> it
            }
        } ?: return null

        val psiFactory = KtPsiFactory(oldPsi.project)
        val oldParentPsi = oldPsi.parent
        val (updOldPsi, updNewPsi) = when {
            oldParentPsi is KtStringTemplateExpression && oldParentPsi.entries.size == 1 -> oldParentPsi to newPsi
            oldPsi is KtStringTemplateEntry && newPsi !is KtStringTemplateEntry && newPsi is KtExpression -> oldPsi to psiFactory.createBlockStringTemplateEntry(newPsi)
            else -> oldPsi to newPsi
        }

        return when (val replaced = updOldPsi.replace(updNewPsi)?.safeAs<KtElement>()?.let { ShortenReferences.DEFAULT.process(it) }) {
            is KtCallExpression, is KtQualifiedExpression -> replaced.cast<KtExpression>().getPossiblyQualifiedCallExpression()
            else -> replaced
        }?.toUElementOfExpectedTypes(elementType)
    }
}

class KotlinUastElementFactory(project: Project) : UastElementFactory {
    private val psiFactory = KtPsiFactory(project)

    override fun createQualifiedReference(qualifiedName: String, context: UElement?): UQualifiedReferenceExpression? {
        return psiFactory.createExpression(qualifiedName).let {
            when (it) {
                is KtDotQualifiedExpression -> KotlinUQualifiedReferenceExpression(it, null)
                is KtSafeQualifiedExpression -> KotlinUSafeQualifiedExpression(it, null)
                else -> null
            }

        }
    }

    override fun createCallExpression(receiver: UExpression?, methodName: String, parameters: List<UExpression>, expectedReturnType: PsiType?, kind: UastCallKind, context: PsiElement?): UCallExpression? {
        if (kind != UastCallKind.METHOD_CALL) return null

        val name = methodName.quoteIfNeeded()
        val methodCall = psiFactory.createExpression(
            if (receiver != null) "a.$name()" else "$name()"
        ).getPossiblyQualifiedCallExpression() ?: return null

        if (receiver != null) {
            methodCall.parent.safeAs<KtDotQualifiedExpression>()?.receiverExpression?.replace(receiver.sourcePsi!!)
        }

        val valueArgumentList = methodCall.valueArgumentList
        for (parameter in parameters) {
            valueArgumentList?.addArgument(psiFactory.createArgument(parameter.sourcePsi as? KtExpression))
        }

        return KotlinUFunctionCallExpression(methodCall, null)

    }

    override fun createStringLiteralExpression(text: String, context: PsiElement?): ULiteralExpression? {
        return KotlinStringULiteralExpression(psiFactory.createExpression(StringUtil.wrapWithDoubleQuote(text)), null)
    }

    override fun createIfExpression(condition: UExpression, thenBranch: UExpression, elseBranch: UExpression?): UIfExpression? {
        val conditionPsi = condition.sourcePsi as? KtExpression ?: return null
        val thenBranchPsi = thenBranch.sourcePsi as? KtExpression ?: return null
        val elseBranchPsi = elseBranch?.sourcePsi as? KtExpression

        return KotlinUIfExpression(psiFactory.createIf(conditionPsi, thenBranchPsi, elseBranchPsi), null)
    }

    override fun createParenthesizedExpression(expression: UExpression): UParenthesizedExpression? {
        val source = expression.sourcePsi ?: return null
        val parenthesized = psiFactory.createExpression("(${source.text})") as? KtParenthesizedExpression ?: return null
        return KotlinUParenthesizedExpression(parenthesized, null)
    }

    override fun createSimpleReference(name: String): USimpleNameReferenceExpression? {
        return KotlinUSimpleReferenceExpression(psiFactory.createSimpleName(name), null)
    }

    override fun createSimpleReference(variable: UVariable): USimpleNameReferenceExpression? {
        return createSimpleReference(variable.name ?: return null)
    }

    override fun createReturnExpresion(expression: UExpression?, inLambda: Boolean): UReturnExpression? {
        val returnExpression = psiFactory.createExpression("return") as KtReturnExpression
        expression?.sourcePsi?.let { returnExpression.add(it) }
        return KotlinUReturnExpression(returnExpression, null)
    }

    override fun createBinaryExpression(leftOperand: UExpression, rightOperand: UExpression, operator: UastBinaryOperator): UBinaryExpression? {
        val leftPsi = leftOperand.sourcePsi ?: return null
        val rightPsi = rightOperand.sourcePsi ?: return null

        val binaryExpression = psiFactory.createExpression("a ${operator.text} b") as? KtBinaryExpression ?: return null
        binaryExpression.left?.replace(leftPsi)
        binaryExpression.right?.replace(rightPsi)
        return KotlinUBinaryExpression(binaryExpression, null)
    }

    override fun createFlatBinaryExpression(leftOperand: UExpression, rightOperand: UExpression, operator: UastBinaryOperator): UPolyadicExpression? {
        return createBinaryExpression(leftOperand, rightOperand, operator)
    }

    override fun createBlockExpression(expressions: List<UExpression>): UBlockExpression? {
        if (expressions.any { it.sourcePsi == null}) return null
        val block = psiFactory.createBlock(expressions.joinToString(separator = "\n") { it.sourcePsi?.text ?: "" })
        return KotlinUBlockExpression(block, null)
    }

    override fun createDeclarationExpression(declarations: List<UDeclaration>): UDeclarationsExpression? {
        return object : KotlinUDeclarationsExpression(null) {
            override var declarations: List<UDeclaration> = declarations
        }
    }

    override fun createLambdaExpression(parameters: List<UParameterInfo>, body: UExpression): ULambdaExpression? {
        TODO("Not yet implemented")
    }

    override fun createLocalVariable(suggestedName: String?, type: PsiType?, initializer: UExpression, immutable: Boolean): ULocalVariable? {
        TODO("Not yet implemented")
    }
}