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

package org.jetbrains.uast.kotlin

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.uast.*
import org.jetbrains.uast.java.JavaUastLanguagePlugin
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.kotlin.expressions.KotlinUBreakExpression
import org.jetbrains.uast.kotlin.expressions.KotlinUContinueExpression
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import org.jetbrains.uast.psi.PsiElementBacked

interface KotlinUastBindingContextProviderService {
    fun getBindingContext(element: KtElement): BindingContext
    fun getTypeMapper(element: KtElement): KotlinTypeMapper?
}

class KotlinUastLanguagePlugin(override val project: Project) : UastLanguagePlugin {
    override val priority = 10

    private val javaPlugin by lz { UastLanguagePlugin.getInstances(project).first { it is JavaUastLanguagePlugin } }

    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".kt", false) || fileName.endsWith(".kts", false)
    }

    override fun convertElement(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        return convertDeclaration(element, parent, requiredType) ?: KotlinConverter.convertPsiElement(element, parent, requiredType)
    }
    
    override fun convertElementWithParent(element: PsiElement, requiredType: Class<out UElement>?): UElement? {
        if (element !is PsiElement) return null
        if (element is PsiFile) return convertDeclaration(element, null, requiredType)

        val parent = element.parent ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null
        return convertElement(element, parentUElement, requiredType)
    }

    override fun getMethodCallExpression(
            element: PsiElement,
            containingClassFqName: String?,
            methodName: String
    ): UastLanguagePlugin.ResolvedMethod? {
        if (element !is KtCallExpression) return null
        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor || resultingDescriptor.name.asString() != methodName) return null
        
        val parent = element.parent ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null

        val uExpression = KotlinUFunctionCallExpression(element, parentUElement, resolvedCall)
        val method = uExpression.resolve() ?: return null
        if (method.name != methodName) return null
        return UastLanguagePlugin.ResolvedMethod(uExpression, method)
    }

    override fun getConstructorCallExpression(
            element: PsiElement,
            fqName: String
    ): UastLanguagePlugin.ResolvedConstructor? {
        if (element !is KtCallExpression) return null
        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is ConstructorDescriptor 
                || resultingDescriptor.returnType.constructor.declarationDescriptor?.name?.asString() != fqName) {
            return null
        }

        val parent = element.parent ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null

        val uExpression = KotlinUFunctionCallExpression(element, parentUElement, resolvedCall)
        val method = uExpression.resolve() ?: return null
        val containingClass = method.containingClass ?: return null
        return UastLanguagePlugin.ResolvedConstructor(uExpression, method, containingClass)
    }

    private fun convertDeclaration(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element is UElement) return element

        if (element.isValid) element.getUserData(KOTLIN_CACHED_UELEMENT_KEY)?.let { ref ->
            ref.get()?.let { return it }
        }
        
        val original = element.originalElement
        return when (original) {
            is KtLightMethod -> KotlinUMethod.create(original, parent)
            is KtLightClass -> KotlinUClass.create(original, parent)
            is KtLightField, is KtLightParameter, is UastKotlinPsiParameter, is UastKotlinPsiVariable -> {
                KotlinUVariable.create(original as PsiVariable, parent)
            }

            is KtClassOrObject -> original.toLightClass()?.let { lightClass -> KotlinUClass.create(lightClass, parent) }
            is KtFunction -> {
                val lightMethod = LightClassUtil.getLightClassMethod(original) ?: return null
                convertDeclaration(lightMethod, parent, requiredType)
            }
            is KtPropertyAccessor -> javaPlugin.convertOpt<UMethod>(
                    LightClassUtil.getLightClassAccessorMethod(original), parent)
            is KtProperty -> javaPlugin.convertOpt<UField>(
                    LightClassUtil.getLightClassBackingField(original), parent)
                             ?: convertDeclaration(element.parent, parent, requiredType)
            
            is KtFile -> KotlinUFile(original, this)
            is FakeFileForLightClass -> KotlinUFile(original.navigationElement, this)
            
            else -> null
        }
    }

    override fun isExpressionValueUsed(element: UExpression): Boolean {
        return when (element) {
            is KotlinUSimpleReferenceExpression.KotlinAccessorCallExpression -> element.setterValue != null
            is KotlinAbstractUExpression -> {
                val ktElement = ((element as? PsiElementBacked)?.psi as? KtElement) ?: return false
                ktElement.analyze()[BindingContext.USED_AS_EXPRESSION, ktElement] ?: false
            }
            else -> false
        }
    }
}

internal inline fun <reified T : UElement> Class<out UElement>?.el(f: () -> UElement?): UElement? {
    return if (this == null || T::class.java == this) f() else null
}

internal inline fun <reified T : UElement> Class<out UElement>?.expr(f: () -> UExpression): UExpression {
    return if (this == null || T::class.java == this) f() else UastEmptyExpression
}

internal object KotlinConverter {
    internal fun convertPsiElement(element: PsiElement?, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        return with (requiredType) { when (element) {
            is KtParameterList -> el<UVariableDeclarationsExpression> {
                KotlinUVariableDeclarationsExpression(parent).apply {
                    variables = element.parameters.mapIndexed { i, p ->
                        KotlinUVariable.create(UastKotlinPsiParameter.create(p, element, parent!!, i), this)
                    }
                }
            }
            is KtClassBody -> KotlinUExpressionList(element, KotlinSpecialExpressionKinds.CLASS_BODY, parent).apply {
                expressions = emptyList()
            }
            is KtCatchClause -> el<UCatchClause> { KotlinUCatchClause(element, parent) }
            is KtExpression -> KotlinConverter.convertExpression(element, parent, requiredType)
            else -> {
                if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
                    el<UIdentifier> { UIdentifier(element, parent) }
                } else {
                    null
                }
            }
        }}
    }
    
    private fun convertVariablesDeclaration(
            psi: KtVariableDeclaration, 
            parent: UElement?
    ): UVariableDeclarationsExpression {
        val parentPsiElement = (parent as? PsiElementBacked)?.psi
        val variable = KotlinUVariable.create(UastKotlinPsiVariable.create(psi, parentPsiElement, parent!!), parent)
        return KotlinUVariableDeclarationsExpression(parent).apply { variables = listOf(variable) }
    }
    
    private fun convertStringTemplateExpression(
            expression: KtStringTemplateExpression,
            parent: UElement?,
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

    internal fun convert(entry: KtStringTemplateEntry, parent: UElement?): UExpression = when (entry) {
        is KtStringTemplateEntryWithExpression -> convertOrEmpty(entry.expression, parent)
        is KtEscapeStringTemplateEntry -> KotlinStringULiteralExpression(entry, parent, entry.unescapedValue)
        else -> {
            KotlinStringULiteralExpression(entry, parent)
        }
    }

    internal fun convertExpression(expression: KtExpression, parent: UElement?, requiredType: Class<out UElement>? = null): UExpression {
        return with (requiredType) { when (expression) {
            is KtVariableDeclaration -> expr<UVariableDeclarationsExpression> { convertVariablesDeclaration(expression, parent) }

            is KtStringTemplateExpression -> expr<ULiteralExpression> {
                if (expression.entries.isEmpty())
                    KotlinStringULiteralExpression(expression, parent, "")
                else if (expression.entries.size == 1)
                    convert(expression.entries[0], parent)
                else
                    convertStringTemplateExpression(expression, parent, expression.entries.size - 1)
            }
            is KtDestructuringDeclaration -> expr<UVariableDeclarationsExpression> {
                KotlinUVariableDeclarationsExpression(parent).apply {
                    val tempAssignment = KotlinUVariable.create(UastKotlinPsiVariable.create(expression, parent!!), parent)
                    val destructuringAssignments = expression.entries.mapIndexed { i, entry ->
                        val psiFactory = KtPsiFactory(expression.project)
                        val initializer = psiFactory.createExpression("${tempAssignment.name}.component${i + 1}()")
                        KotlinUVariable.create(UastKotlinPsiVariable.create(
                                entry, tempAssignment.psi, parent, initializer), parent)
                    }
                    variables = listOf(tempAssignment) + destructuringAssignments
                }
            }
            is KtLabeledExpression -> expr<ULabeledExpression> { KotlinULabeledExpression(expression, parent) }
            is KtClassLiteralExpression -> expr<UClassLiteralExpression> { KotlinUClassLiteralExpression(expression, parent) }
            is KtObjectLiteralExpression -> expr<UObjectLiteralExpression> { KotlinUObjectLiteralExpression(expression, parent) }
            is KtStringTemplateEntry -> expression.expression?.let { convertExpression(it, parent, requiredType) } ?: UastEmptyExpression
            is KtDotQualifiedExpression -> expr<UQualifiedReferenceExpression> { KotlinUQualifiedReferenceExpression(expression, parent) }
            is KtSafeQualifiedExpression -> expr<UQualifiedReferenceExpression> { KotlinUSafeQualifiedExpression(expression, parent) }
            is KtSimpleNameExpression -> expr<USimpleNameReferenceExpression> {
                KotlinUSimpleReferenceExpression(expression, expression.getReferencedName(), parent) 
            }
            is KtCallExpression -> expr<UCallExpression> { KotlinUFunctionCallExpression(expression, parent) }
            is KtBinaryExpression -> expr<UBinaryExpression> { KotlinUBinaryExpression(expression, parent) }
            is KtParenthesizedExpression -> expr<UParenthesizedExpression> { KotlinUParenthesizedExpression(expression, parent) }
            is KtPrefixExpression -> expr<UPrefixExpression> { KotlinUPrefixExpression(expression, parent) }
            is KtPostfixExpression -> expr<UPostfixExpression> { KotlinUPostfixExpression(expression, parent) }
            is KtThisExpression -> expr<UThisExpression> { KotlinUThisExpression(expression, parent) }
            is KtSuperExpression -> expr<USuperExpression> { KotlinUSuperExpression(expression, parent) }
            is KtCallableReferenceExpression -> expr<UCallableReferenceExpression> { KotlinUCallableReferenceExpression(expression, parent) }
            is KtIsExpression -> expr<UBinaryExpressionWithType> { KotlinUTypeCheckExpression(expression, parent) }
            is KtIfExpression -> expr<UIfExpression> { KotlinUIfExpression(expression, parent) }
            is KtWhileExpression -> expr<UWhileExpression> { KotlinUWhileExpression(expression, parent) }
            is KtDoWhileExpression -> expr<UDoWhileExpression> { KotlinUDoWhileExpression(expression, parent) }
            is KtForExpression -> expr<UForEachExpression> { KotlinUForEachExpression(expression, parent) }
            is KtWhenExpression -> expr<USwitchExpression> { KotlinUSwitchExpression(expression, parent) }
            is KtBreakExpression -> expr<UBreakExpression> { KotlinUBreakExpression(expression, parent) }
            is KtContinueExpression -> expr<UContinueExpression> { KotlinUContinueExpression(expression, parent) }
            is KtReturnExpression -> expr<UReturnExpression> { KotlinUReturnExpression(expression, parent) }
            is KtThrowExpression -> expr<UThrowExpression> { KotlinUThrowExpression(expression, parent) }
            is KtBlockExpression -> expr<UBlockExpression> { KotlinUBlockExpression(expression, parent) }
            is KtConstantExpression -> expr<ULiteralExpression> { KotlinULiteralExpression(expression, parent) }
            is KtTryExpression -> expr<UTryExpression> { KotlinUTryExpression(expression, parent) }
            is KtArrayAccessExpression -> expr<UArrayAccessExpression> { KotlinUArrayAccessExpression(expression, parent) }
            is KtLambdaExpression -> expr<ULambdaExpression> { KotlinULambdaExpression(expression, parent) }
            is KtBinaryExpressionWithTypeRHS -> expr<UBinaryExpressionWithType> { KotlinUBinaryExpressionWithType(expression, parent) }

            else -> UnknownKotlinExpression(expression, parent)
        }}
    }
    
    internal fun convertOrEmpty(expression: KtExpression?, parent: UElement?): UExpression {
        return if (expression != null) convertExpression(expression, parent, null) else UastEmptyExpression
    }

    internal fun convertOrNull(expression: KtExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent, null) else null
    }
}