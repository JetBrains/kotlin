/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.generate

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.resolveToKotlinType
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.*
import org.jetbrains.uast.generate.UParameterInfo
import org.jetbrains.uast.generate.UastCodeGenerationPlugin
import org.jetbrains.uast.generate.UastElementFactory
import org.jetbrains.uast.kotlin.*
import org.jetbrains.uast.kotlin.internal.KotlinFakeUElement
import org.jetbrains.uast.kotlin.internal.toSourcePsiFakeAware

class KotlinUastCodeGenerationPlugin : UastCodeGenerationPlugin {
    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun getElementFactory(project: Project): UastElementFactory =
        KotlinUastElementFactory(project)

    override fun <T : UElement> replace(oldElement: UElement, newElement: T, elementType: Class<T>): T? {
        val oldPsi = oldElement.toSourcePsiFakeAware().singleOrNull() ?: return null
        val newPsi = newElement.sourcePsi?.let {
            when {
                it is KtCallExpression && it.parent is KtQualifiedExpression -> it.parent
                else -> it
            }
        } ?: return null

        val psiFactory = KtPsiFactory(oldPsi.project)
        val oldParentPsi = oldPsi.parent
        val (updOldPsi, updNewPsi) = when {
            oldParentPsi is KtStringTemplateExpression && oldParentPsi.entries.size == 1 ->
                oldParentPsi to newPsi

            oldPsi is KtStringTemplateEntry && newPsi !is KtStringTemplateEntry && newPsi is KtExpression ->
                oldPsi to psiFactory.createBlockStringTemplateEntry(newPsi)

            oldPsi is KtBlockExpression && newPsi is KtBlockExpression -> {
                if (!hasBraces(oldPsi) && hasBraces(newPsi)) {
                    oldPsi to psiFactory.createLambdaExpression("none", newPsi.statements.joinToString("\n") { "println()" }).bodyExpression!!.also {
                        it.statements.zip(newPsi.statements).forEach { it.first.replace(it.second) }
                    }
                } else
                    oldPsi to newPsi
            }

            else ->
                oldPsi to newPsi
        }

        return when (val replaced = updOldPsi.replace(updNewPsi)?.safeAs<KtElement>()?.let { ShortenReferences.DEFAULT.process(it) }) {
            is KtCallExpression, is KtQualifiedExpression -> replaced.cast<KtExpression>().getPossiblyQualifiedCallExpression()
            else -> replaced
        }?.toUElementOfExpectedTypes(elementType)
    }
}

private fun hasBraces(oldPsi: KtBlockExpression): Boolean = oldPsi.lBrace != null && oldPsi.rBrace != null

class KotlinUastElementFactory(project: Project) : UastElementFactory {
    private val psiFactory = KtPsiFactory(project)

    @Deprecated("use version with context parameter")
    override fun createQualifiedReference(qualifiedName: String, context: UElement?): UQualifiedReferenceExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createQualifiedReference(qualifiedName, context?.sourcePsi)
    }

    /*override*/ fun createQualifiedReference(qualifiedName: String, context: PsiElement?): UQualifiedReferenceExpression? {
        return psiFactory.createExpression(qualifiedName).let {
            when (it) {
                is KtDotQualifiedExpression -> KotlinUQualifiedReferenceExpression(it, null)
                is KtSafeQualifiedExpression -> KotlinUSafeQualifiedExpression(it, null)
                else -> null
            }

        }
    }

    override fun createCallExpression(
        receiver: UExpression?,
        methodName: String,
        parameters: List<UExpression>,
        expectedReturnType: PsiType?,
        kind: UastCallKind,
        context: PsiElement?
    ): UCallExpression? {
        if (kind != UastCallKind.METHOD_CALL) return null

        val typeParams = (context as? KtElement)?.let { kontext ->
            val resolutionFacade = kontext.getResolutionFacade()
            (expectedReturnType as? PsiClassType)?.parameters?.map { it.resolveToKotlinType(resolutionFacade) }
        }

        val name = methodName.quoteIfNeeded()
        val methodCall = psiFactory.createExpression(
            buildString {
                if (receiver != null)
                    append("a.")
                append(name)
                if (typeParams != null) {
                    append(typeParams.joinToString(", ", "<", ">") { type ->
                        type.fqName?.asString() ?: ""
                    })
                }
                append("()")
            }
        ).getPossiblyQualifiedCallExpression() ?: return null

        if (receiver != null) {
            methodCall.parent.safeAs<KtDotQualifiedExpression>()?.receiverExpression?.replace(wrapULiteral(receiver).sourcePsi!!)
        }

        val valueArgumentList = methodCall.valueArgumentList
        for (parameter in parameters) {
            valueArgumentList?.addArgument(psiFactory.createArgument(wrapULiteral(parameter).sourcePsi as KtExpression))
        }

        return KotlinUFunctionCallExpression(methodCall, null)

    }

    override fun createStringLiteralExpression(text: String, context: PsiElement?): ULiteralExpression? {
        return KotlinStringULiteralExpression(psiFactory.createExpression(StringUtil.wrapWithDoubleQuote(text)), null)
    }

    /*override*/ fun createNullLiteral(context: PsiElement?): ULiteralExpression {
        return psiFactory.createExpression("null").toUElementOfType<ULiteralExpression>()!!
    }

    /*override*/ fun createIntLiteral(value: Int, context: PsiElement?): ULiteralExpression {
        return psiFactory.createExpression(value.toString()).toUElementOfType<ULiteralExpression>()!!
    }

    private fun KtExpression.ensureBlockExpressionBraces(): KtExpression {
        if (this !is KtBlockExpression || hasBraces(this)) return this
        val blockExpression = psiFactory.createBlock(this.statements.joinToString("\n") { "println()" })
        for ((placeholder, statement) in blockExpression.statements.zip(this.statements)) {
            placeholder.replace(statement)
        }
        return blockExpression
    }

    @Deprecated("use version with context parameter")
    override fun createIfExpression(condition: UExpression, thenBranch: UExpression, elseBranch: UExpression?): UIfExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createIfExpression(condition, thenBranch, elseBranch, null)
    }

    /*override*/ fun createIfExpression(
        condition: UExpression,
        thenBranch: UExpression,
        elseBranch: UExpression?,
        context: PsiElement?
    ): UIfExpression? {
        val conditionPsi = condition.sourcePsi as? KtExpression ?: return null
        val thenBranchPsi = thenBranch.sourcePsi as? KtExpression ?: return null
        val elseBranchPsi = elseBranch?.sourcePsi as? KtExpression

        return KotlinUIfExpression(psiFactory.createIf(conditionPsi, thenBranchPsi.ensureBlockExpressionBraces(), elseBranchPsi?.ensureBlockExpressionBraces()), null)
    }

    @Deprecated("use version with context parameter")
    override fun createParenthesizedExpression(expression: UExpression): UParenthesizedExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createParenthesizedExpression(expression, null)
    }

    /*override*/ fun createParenthesizedExpression(expression: UExpression, context: PsiElement?): UParenthesizedExpression? {
        val source = expression.sourcePsi ?: return null
        val parenthesized = psiFactory.createExpression("(${source.text})") as? KtParenthesizedExpression ?: return null
        return KotlinUParenthesizedExpression(parenthesized, null)
    }

    @Deprecated("use version with context parameter")
    override fun createSimpleReference(name: String): USimpleNameReferenceExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createSimpleReference(name, null)
    }

    /*override*/ fun createSimpleReference(name: String, context: PsiElement?): USimpleNameReferenceExpression? {
        return KotlinUSimpleReferenceExpression(psiFactory.createSimpleName(name), null)
    }

    @Deprecated("use version with context parameter")
    override fun createSimpleReference(variable: UVariable): USimpleNameReferenceExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createSimpleReference(variable, null)
    }

    /*override*/ fun createSimpleReference(variable: UVariable, context: PsiElement?): USimpleNameReferenceExpression? {
        return createSimpleReference(variable.name ?: return null, context)
    }

    @Deprecated("use version with context parameter")
    override fun createReturnExpresion(expression: UExpression?, inLambda: Boolean): UReturnExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createReturnExpresion(expression, inLambda, null)
    }

    /*override*/ fun createReturnExpresion(expression: UExpression?, inLambda: Boolean, context: PsiElement?): UReturnExpression? {
        val label = if (inLambda && context != null) getParentLambdaLabelName(context)?.let { "@$it" } ?: "" else ""
        val returnExpression = psiFactory.createExpression("return$label 1") as KtReturnExpression
        val sourcePsi = expression?.sourcePsi
        if (sourcePsi != null) {
            returnExpression.returnedExpression!!.replace(sourcePsi)
        } else {
            returnExpression.returnedExpression?.delete()
        }
        return KotlinUReturnExpression(returnExpression, null)
    }

    private fun getParentLambdaLabelName(context: PsiElement): String? {
        val lambdaExpression = context.getParentOfType<KtLambdaExpression>(false) ?: return null
        lambdaExpression.parent.safeAs<KtLabeledExpression>()?.let { return it.getLabelName() }
        val callExpression = lambdaExpression.getStrictParentOfType<KtCallExpression>() ?: return null
        callExpression.valueArguments.find {
            it.getArgumentExpression()?.unpackFunctionLiteral(allowParentheses = false) === lambdaExpression
        } ?: return null
        return callExpression.getCallNameExpression()?.text
    }

    @Deprecated("use version with context parameter")
    override fun createBinaryExpression(
        leftOperand: UExpression,
        rightOperand: UExpression,
        operator: UastBinaryOperator
    ): UBinaryExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createBinaryExpression(leftOperand, rightOperand, operator, null)
    }

    /*override*/ fun createBinaryExpression(
        leftOperand: UExpression,
        rightOperand: UExpression,
        operator: UastBinaryOperator,
        context: PsiElement?
    ): UBinaryExpression? {
        val binaryExpression = joinBinaryExpression(leftOperand, rightOperand, operator) ?: return null
        return KotlinUBinaryExpression(binaryExpression, null)
    }

    private fun joinBinaryExpression(
        leftOperand: UExpression,
        rightOperand: UExpression,
        operator: UastBinaryOperator
    ): KtBinaryExpression? {
        val leftPsi = leftOperand.sourcePsi ?: return null
        val rightPsi = rightOperand.sourcePsi ?: return null

        val binaryExpression = psiFactory.createExpression("a ${operator.text} b") as? KtBinaryExpression ?: return null
        binaryExpression.left?.replace(leftPsi)
        binaryExpression.right?.replace(rightPsi)
        return binaryExpression
    }

    @Deprecated("use version with context parameter")
    override fun createFlatBinaryExpression(
        leftOperand: UExpression,
        rightOperand: UExpression,
        operator: UastBinaryOperator
    ): UPolyadicExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createFlatBinaryExpression(leftOperand, rightOperand, operator, null)
    }

    /*override*/ fun createFlatBinaryExpression(
        leftOperand: UExpression,
        rightOperand: UExpression,
        operator: UastBinaryOperator,
        context: PsiElement?
    ): UPolyadicExpression? {

        fun unwrapParentheses(exp: KtExpression?) {
            if (exp !is KtParenthesizedExpression) return
            if (!KtPsiUtil.areParenthesesUseless(exp)) return
            exp.expression?.let { exp.replace(it) }
        }

        val binaryExpression = joinBinaryExpression(leftOperand, rightOperand, operator) ?: return null
        unwrapParentheses(binaryExpression.left)
        unwrapParentheses(binaryExpression.right)

        return psiFactory.createExpression(binaryExpression.text).toUElementOfType()!!
    }

    @Deprecated("use version with context parameter")
    override fun createBlockExpression(expressions: List<UExpression>): UBlockExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createBlockExpression(expressions, null)
    }

    /*override*/ fun createBlockExpression(expressions: List<UExpression>, context: PsiElement?): UBlockExpression? {
        val sourceExpressions = expressions.flatMap { it.toSourcePsiFakeAware() }
        val block = psiFactory.createBlock(
            sourceExpressions.joinToString(separator = "\n") { "println()" }
        )
        for ((placeholder, psiElement) in block.statements.zip(sourceExpressions)) {
            placeholder.replace(psiElement)
        }
        return KotlinUBlockExpression(block, null)
    }

    @Deprecated("use version with context parameter")
    override fun createDeclarationExpression(declarations: List<UDeclaration>): UDeclarationsExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createDeclarationExpression(declarations, null)
    }

    /*override*/ fun createDeclarationExpression(declarations: List<UDeclaration>, context: PsiElement?): UDeclarationsExpression? {
        return object : KotlinUDeclarationsExpression(null), KotlinFakeUElement {
            override var declarations: List<UDeclaration> = declarations
            override fun unwrapToSourcePsi(): List<PsiElement> = declarations.flatMap { it.toSourcePsiFakeAware() }
        }
    }

    /*override*/  fun createLambdaExpression(
        parameters: List<UParameterInfo>,
        body: UExpression,
        context: PsiElement?
    ): ULambdaExpression? {
        val resolutionFacade = (context as? KtElement)?.getResolutionFacade()
        val validator = (context as? KtElement)?.let { usedNamesFilter(it) } ?: { true }

        val newLambdaStatements = if (body is UBlockExpression) {
            body.expressions.flatMap { member ->
                when {
                    member is UReturnExpression -> member.returnExpression?.toSourcePsiFakeAware().orEmpty()
                    else -> member.toSourcePsiFakeAware()
                }
            }
        } else
            listOf(body.sourcePsi!!)

        val ktLambdaExpression = psiFactory.createLambdaExpression(
            parameters.joinToString(", ") { p ->
                val ktype = resolutionFacade?.let { p.type?.resolveToKotlinType(it) }
                StringBuilder().apply {
                    append(p.suggestedName ?: ktype?.let { KotlinNameSuggester.suggestNamesByType(it, validator).firstOrNull() })
                        ?: KotlinNameSuggester.suggestNameByName("v", validator)
                    ktype?.fqName?.toString()?.let { append(": ").append(it) }
                }
            },
            newLambdaStatements.joinToString("\n") { "placeholder" }
        )

        for ((old, new) in ktLambdaExpression.bodyExpression!!.statements.zip(newLambdaStatements)) {
            old.replace(new)
        }

        return ktLambdaExpression.toUElementOfType()!!
    }

    @Deprecated("use version with context parameter")
    override fun createLambdaExpression(parameters: List<UParameterInfo>, body: UExpression): ULambdaExpression? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createLambdaExpression(parameters, body, null)
    }

    /*override*/ fun createLocalVariable(
        suggestedName: String?,
        type: PsiType?,
        initializer: UExpression,
        immutable: Boolean,
        context: PsiElement?
    ): ULocalVariable? {
        val resolutionFacade = (context as? KtElement)?.getResolutionFacade()
        val validator = (context as? KtElement)?.let { usedNamesFilter(it) } ?: { true }
        val ktype = resolutionFacade?.let { type?.resolveToKotlinType(it) }

        val function = psiFactory.createFunction(
            buildString {
                append("fun foo() { ")
                append(if (immutable) "val" else "var")
                append(" ")
                append(suggestedName ?: ktype?.let { KotlinNameSuggester.suggestNamesByType(it, validator).firstOrNull() })
                    ?: KotlinNameSuggester.suggestNameByName("v", validator)
                ktype?.fqName?.toString()?.let { append(": ").append(it) }
                append(" = null")
                append("}")
            }
        )

        val ktVariable = PsiTreeUtil.findChildOfType(function, KtVariableDeclaration::class.java)!!
        val newVariable = ktVariable.initializer!!.replace(initializer.sourcePsi!!).parent
        return newVariable.toUElementOfType<UVariable>() as ULocalVariable
    }

    @Deprecated("use version with context parameter")
    override fun createLocalVariable(
        suggestedName: String?,
        type: PsiType?,
        initializer: UExpression,
        immutable: Boolean
    ): ULocalVariable? {
        logger<KotlinUastElementFactory>().error("Please switch caller to the version with a context parameter")
        return createLocalVariable(suggestedName, type, initializer, immutable, null)
    }
}

private fun usedNamesFilter(context: KtElement): (String) -> Boolean {
    val scope = context.getResolutionScope()
    return { name: String -> scope.findClassifier(Name.identifier(name), NoLookupLocation.FROM_IDE) == null }
}