/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package org.jetbrains.uast.java

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.java.expressions.JavaUSynchronizedExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUastLanguagePlugin(override val project: Project) : UastLanguagePlugin {
    override val priority = 0

    override fun isFileSupported(fileName: String) = fileName.endsWith(".java", ignoreCase = true)

    override val language: Language
        get() = JavaLanguage.INSTANCE

    override fun isExpressionValueUsed(element: UExpression): Boolean = when (element) {
        is JavaUVariableDeclarationsExpression -> false
        is UnknownJavaExpression -> (element.containingElement as? UExpression)?.let { isExpressionValueUsed(it) } ?: false
        else -> {
            val statement = (element as? PsiElementBacked)?.psi as? PsiStatement
            statement != null && statement.parent !is PsiExpressionStatement
        }
    }

    override fun getMethodCallExpression(
            element: PsiElement,
            containingClassFqName: String?,
            methodName: String
    ): UastLanguagePlugin.ResolvedMethod? {
        if (element !is PsiMethodCallExpression) return null
        if (element.methodExpression.referenceName != methodName) return null
        
        val uElement = convertElementWithParent(element, null)
        val callExpression = when (uElement) {
            is UCallExpression -> uElement
            is UQualifiedReferenceExpression -> uElement.selector as UCallExpression
            else -> error("Invalid element type: $uElement")
        }
        
        val method = callExpression.resolve() ?: return null
        if (containingClassFqName != null) {
            val containingClass = method.containingClass ?: return null
            if (containingClass.qualifiedName != containingClassFqName) return null
        }
        
        return UastLanguagePlugin.ResolvedMethod(callExpression, method)
    }
    
    override fun getConstructorCallExpression(
            element: PsiElement,
            fqName: String
    ): UastLanguagePlugin.ResolvedConstructor? {
        if (element !is PsiNewExpression) return null
        val simpleName = fqName.substringAfterLast('.')
        if (element.classReference?.referenceName != simpleName) return null
        
        val callExpression = convertElementWithParent(element, null) as? UCallExpression ?: return null
        
        val constructorMethod = element.resolveConstructor() ?: return null
        val containingClass = constructorMethod.containingClass ?: return null
        if (containingClass.qualifiedName != fqName) return null
        
        return UastLanguagePlugin.ResolvedConstructor(callExpression, constructorMethod, containingClass)
    }

    override fun convertElement(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        return convertDeclaration(element, parent, requiredType) ?: JavaConverter.convertPsiElement(element, parent, requiredType)
    }

    override fun convertElementWithParent(element: PsiElement, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiJavaFile) return JavaUFile(element, this)
        JavaConverter.getCached<UElement>(element)?.let { return it }

        val parent = JavaConverter.unwrapElements(element.parent) ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null
        return convertElement(element, parentUElement, requiredType)
    }
    
    private fun convertDeclaration(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element.isValid) element.getUserData(JAVA_CACHED_UELEMENT_KEY)?.let { ref ->
            ref.get()?.let { return it }
        }

        return with (requiredType) { when (element) {
            is PsiJavaFile -> el<UFile> { JavaUFile(element, this@JavaUastLanguagePlugin) }
            is UDeclaration -> element
            is PsiClass -> el<UClass> { JavaUClass.create(element, parent) }
            is PsiMethod -> el<UMethod> { JavaUMethod.create(element, this@JavaUastLanguagePlugin, parent) }
            is PsiClassInitializer -> el<UClassInitializer> { JavaUClassInitializer(element, parent) }
            is PsiVariable -> el<UVariable> { JavaUVariable.create(element, parent) }
            is PsiAnnotation -> el<UAnnotation> { JavaUAnnotation(element, parent) } //???
            else -> null
        }}
    }
}

internal inline fun <reified T : UElement> Class<out UElement>?.el(f: () -> UElement?): UElement? {
    return if (this == null || T::class.java == this) f() else null
}

internal inline fun <reified T : UElement> Class<out UElement>?.expr(f: () -> UExpression): UExpression {
    return if (this == null || T::class.java == this) f() else UastEmptyExpression
}

internal object JavaConverter {
    internal inline fun <reified T : UElement> getCached(element: PsiElement): T? {
        return null
        //todo
    }

    internal tailrec fun unwrapElements(element: PsiElement?): PsiElement? = when (element) {
        is PsiExpressionStatement -> unwrapElements(element.parent)
        is PsiParameterList -> unwrapElements(element.parent)
        is PsiAnnotationParameterList -> unwrapElements(element.parent)
        else -> element
    }

    internal fun convertPsiElement(el: PsiElement, parent: UElement?, requiredType: Class<out UElement>? = null): UElement? {
        getCached<UElement>(el)?.let { return it }

        return with (requiredType) { when (el) {
            is PsiCodeBlock -> el<UBlockExpression> { convertBlock(el, parent) }
            is PsiResourceExpression -> convertExpression(el.expression, parent, requiredType)
            is PsiExpression -> convertExpression(el, parent, requiredType)
            is PsiStatement -> convertStatement(el, parent, requiredType)
            is PsiIdentifier -> el<USimpleNameReferenceExpression> { JavaUSimpleNameReferenceExpression(el, el.text, parent) }
            is PsiNameValuePair -> el<UNamedExpression> { convertNameValue(el, parent) }
            is PsiArrayInitializerMemberValue -> el<UCallExpression> { JavaAnnotationArrayInitializerUCallExpression(el, parent) }
            else -> null
        }}
    }
    
    internal fun convertBlock(block: PsiCodeBlock, parent: UElement?): UBlockExpression =
        getCached(block) ?: JavaUCodeBlockExpression(block, parent)

    internal fun convertNameValue(pair: PsiNameValuePair, parent: UElement?): UNamedExpression {
        return UNamedExpression.create(pair.name.orAnonymous(), parent) {
            val value = pair.value as? PsiElement
            value?.let { convertPsiElement(it, this, null) as? UExpression } ?: UnknownJavaExpression(value ?: pair, this)
        }
    }

    internal fun convertReference(expression: PsiReferenceExpression, parent: UElement?, requiredType: Class<out UElement>?): UExpression {
        return with (requiredType) {
            if (expression.isQualified) {
                expr<UQualifiedReferenceExpression> { JavaUQualifiedReferenceExpression(expression, parent) }
            } else {
                val name = expression.referenceName ?: "<error name>"
                expr<USimpleNameReferenceExpression> { JavaUSimpleNameReferenceExpression(expression, name, parent, expression) }
            }
        }
    }

    private fun convertPolyadicExpression(
        expression: PsiPolyadicExpression,
        parent: UElement?,
        i: Int
    ): UBinaryExpression {
        return if (i == 1) JavaSeparatedPolyadicUBinaryExpression(expression, parent).apply {
            leftOperand = convertExpression(expression.operands[0], this)
            rightOperand = convertExpression(expression.operands[1], this)
        } else JavaSeparatedPolyadicUBinaryExpression(expression, parent).apply {
            leftOperand = convertPolyadicExpression(expression, parent, i - 1)
            rightOperand = convertExpression(expression.operands[i], this)
        }
    }
    
    internal fun convertExpression(el: PsiExpression, parent: UElement?, requiredType: Class<out UElement>? = null): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return with (requiredType) { when (el) {
            is PsiPolyadicExpression -> expr<UBinaryExpression> { convertPolyadicExpression(el, parent, el.operands.size - 1) }
            is PsiAssignmentExpression -> expr<UBinaryExpression> { JavaUAssignmentExpression(el, parent) }
            is PsiConditionalExpression -> expr<UIfExpression> { JavaUTernaryIfExpression(el, parent) }
            is PsiNewExpression -> {
                if (el.anonymousClass != null)
                    expr<UObjectLiteralExpression> { JavaUObjectLiteralExpression(el, parent) }
                else
                    expr<UCallExpression> { JavaConstructorUCallExpression(el, parent) }
            }
            is PsiMethodCallExpression -> {
                if (el.methodExpression.qualifierExpression != null)
                    expr<UQualifiedReferenceExpression> {
                        JavaUCompositeQualifiedExpression(el, parent).apply {
                            receiver = convertExpression(el.methodExpression.qualifierExpression!!, this)
                            selector = JavaUCallExpression(el, this)
                        }
                    }
                else
                    expr<UCallExpression> { JavaUCallExpression(el, parent) }
            }
            is PsiArrayInitializerExpression -> expr<UCallExpression> { JavaArrayInitializerUCallExpression(el, parent) }
            is PsiBinaryExpression -> expr<UBinaryExpression> { JavaUBinaryExpression(el, parent) }
            is PsiParenthesizedExpression -> expr<UParenthesizedExpression> { JavaUParenthesizedExpression(el, parent) }
            is PsiPrefixExpression -> expr<UPrefixExpression> { JavaUPrefixExpression(el, parent) }
            is PsiPostfixExpression -> expr<UPostfixExpression> { JavaUPostfixExpression(el, parent) }
            is PsiLiteralExpression -> expr<ULiteralExpression> { JavaULiteralExpression(el, parent) }
            is PsiReferenceExpression -> convertReference(el, parent, requiredType)
            is PsiThisExpression -> expr<UThisExpression> { JavaUThisExpression(el, parent) }
            is PsiSuperExpression -> expr<USuperExpression> { JavaUSuperExpression(el, parent) }
            is PsiInstanceOfExpression -> expr<UBinaryExpressionWithType> { JavaUInstanceCheckExpression(el, parent) }
            is PsiTypeCastExpression -> expr<UBinaryExpressionWithType> { JavaUTypeCastExpression(el, parent) }
            is PsiClassObjectAccessExpression -> expr<UClassLiteralExpression> { JavaUClassLiteralExpression(el, parent) }
            is PsiArrayAccessExpression -> expr<UArrayAccessExpression> { JavaUArrayAccessExpression(el, parent) }
            is PsiLambdaExpression -> expr<ULambdaExpression> { JavaULambdaExpression(el, parent) }
            is PsiMethodReferenceExpression -> expr<UCallableReferenceExpression> { JavaUCallableReferenceExpression(el, parent) }
            else -> UnknownJavaExpression(el, parent)
        }}
    }
    
    internal fun convertStatement(el: PsiStatement, parent: UElement?, requiredType: Class<out UElement>? = null): UExpression {
        getCached<UExpression>(el)?.let { return it }

        return with (requiredType) { when (el) {
            is PsiDeclarationStatement -> expr<UVariableDeclarationsExpression> { convertDeclarations(el.declaredElements, parent!!) }
            is PsiExpressionListStatement -> expr<UVariableDeclarationsExpression> { convertDeclarations(el.expressionList.expressions, parent!!) }
            is PsiBlockStatement -> expr<UBlockExpression> { JavaUBlockExpression(el, parent) }
            is PsiLabeledStatement -> expr<ULabeledExpression> { JavaULabeledExpression(el, parent) }
            is PsiExpressionStatement -> convertExpression(el.expression, parent, requiredType)
            is PsiIfStatement -> expr<UIfExpression> { JavaUIfExpression(el, parent) }
            is PsiSwitchStatement -> expr<USwitchExpression> { JavaUSwitchExpression(el, parent) }
            is PsiSwitchLabelStatement -> expr<USwitchClauseExpression> {
                if (el.isDefaultCase)
                    DefaultUSwitchClauseExpression(parent)
                else JavaUCaseSwitchClauseExpression(el, parent)
            }
            is PsiWhileStatement -> expr<UWhileExpression> { JavaUWhileExpression(el, parent) }
            is PsiDoWhileStatement -> expr<UDoWhileExpression> { JavaUDoWhileExpression(el, parent) }
            is PsiForStatement -> expr<UForExpression> { JavaUForExpression(el, parent) }
            is PsiForeachStatement -> expr<UForEachExpression> { JavaUForEachExpression(el, parent) }
            is PsiBreakStatement -> expr<UBreakExpression> { JavaUBreakExpression(el, parent) }
            is PsiContinueStatement -> expr<UContinueExpression> { JavaUContinueExpression(el, parent) }
            is PsiReturnStatement -> expr<UReturnExpression> { JavaUReturnExpression(el, parent) }
            is PsiAssertStatement -> expr<UCallExpression> { JavaUAssertExpression(el, parent) }
            is PsiThrowStatement -> expr<UThrowExpression> { JavaUThrowExpression(el, parent) }
            is PsiSynchronizedStatement -> expr<UBlockExpression> { JavaUSynchronizedExpression(el, parent) }
            is PsiTryStatement -> expr<UTryExpression> { JavaUTryExpression(el, parent) }
            else -> UnknownJavaExpression(el, parent)
        }}
    }

    private fun convertDeclarations(elements: Array<out PsiElement>, parent: UElement): UVariableDeclarationsExpression {
        return JavaUVariableDeclarationsExpression(parent).apply {
            val variables = mutableListOf<UVariable>()
            for (element in elements) {
                if (element !is PsiVariable) continue
                variables += JavaUVariable.create(element, this)
            }
            this.variables = variables
        }
    }

    internal fun convertOrEmpty(statement: PsiStatement?, parent: UElement?): UExpression {
        return statement?.let { convertStatement(it, parent, null) } ?: UastEmptyExpression
    }

    internal fun convertOrEmpty(expression: PsiExpression?, parent: UElement?): UExpression {
        return expression?.let { convertExpression(it, parent) } ?: UastEmptyExpression
    }

    internal fun convertOrNull(expression: PsiExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent) else null
    }

    internal fun convertOrEmpty(block: PsiCodeBlock?, parent: UElement?): UExpression {
        return if (block != null) convertBlock(block, parent) else UastEmptyExpression
    }
}