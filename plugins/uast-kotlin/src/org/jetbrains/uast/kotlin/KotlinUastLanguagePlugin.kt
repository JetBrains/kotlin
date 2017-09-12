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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.*
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
import org.jetbrains.uast.kotlin.expressions.*
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import java.lang.ref.WeakReference

interface KotlinUastBindingContextProviderService {
    fun getBindingContext(element: KtElement): BindingContext
    fun getTypeMapper(element: KtElement): KotlinTypeMapper?
}

class KotlinUastLanguagePlugin : UastLanguagePlugin {
    override val priority = 10

    private val javaPlugin by lz { UastLanguagePlugin.getInstances().first { it is JavaUastLanguagePlugin } }

    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".kt", false) || fileName.endsWith(".kts", false)
    }

    override fun convertElement(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        return convertDeclarationOrElement(element, parent, requiredType)
    }
    
    override fun convertElementWithParent(element: PsiElement, requiredType: Class<out UElement>?): UElement? {
        if (element is PsiFile) return convertDeclaration(element, null, requiredType)
        if (element is KtLightClassForFacade) return convertDeclaration(element, null, requiredType)

        return convertDeclarationOrElement(element, null, requiredType)
    }

    private fun convertDeclarationOrElement(element: PsiElement, givenParent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (element is UElement) return element

        if (element.isValid) {
            element.getUserData(KOTLIN_CACHED_UELEMENT_KEY)?.get()?.let { cachedUElement ->
                return if (requiredType == null || requiredType.isInstance(cachedUElement)) cachedUElement else null
            }
        }

        val uElement = convertDeclaration(element, givenParent, requiredType)
                        ?: KotlinConverter.convertPsiElement(element, givenParent, requiredType)
        if (uElement != null) {
            element.putUserData(KOTLIN_CACHED_UELEMENT_KEY, WeakReference(uElement))
        }
        return uElement
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
        
        val parent = element.parent
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

        val parent = KotlinConverter.unwrapElements(element.parent) ?: return null
        val parentUElement = convertElementWithParent(parent, null) ?: return null

        val uExpression = KotlinUFunctionCallExpression(element, parentUElement, resolvedCall)
        val method = uExpression.resolve() ?: return null
        val containingClass = method.containingClass ?: return null
        return UastLanguagePlugin.ResolvedConstructor(uExpression, method, containingClass)
    }

    private fun convertDeclaration(element: PsiElement,
                                   givenParent: UElement?,
                                   requiredType: Class<out UElement>?): UElement? {
        fun <P : PsiElement> build(ctor: (P, UElement?) -> UElement): () -> UElement? {
            return { ctor(element as P, givenParent) }
        }

        val original = element.originalElement
        return with(requiredType) {
            when (original) {
                is KtLightMethod -> el<UMethod>(build(KotlinUMethod.Companion::create))   // .Companion is needed because of KT-13934
                is KtLightClass -> el<UClass>(build(KotlinUClass.Companion::create))

                is KtLightFieldImpl.KtLightEnumConstant -> el<UEnumConstant>(build(::KotlinUEnumConstant))
                is KtLightField -> el<UField>(build(::KotlinUField))
                is KtLightParameter, is UastKotlinPsiParameter -> el<UParameter>(build(::KotlinUParameter))
                is UastKotlinPsiVariable -> el<UVariable>(build(::KotlinUVariable))

                is KtClassOrObject -> el<UClass> {
                    original.toLightClass()?.let { lightClass ->
                        KotlinUClass.create(lightClass, givenParent)
                    }
                }
                is KtFunction -> el<UMethod> {
                    val lightMethod = LightClassUtil.getLightClassMethod(original) ?: return null
                    convertDeclaration(lightMethod, givenParent, requiredType)
                }
                is KtPropertyAccessor -> el<UMethod> {
                    javaPlugin.convertOpt<UMethod>(
                            LightClassUtil.getLightClassAccessorMethod(original), givenParent)
                }
                is KtProperty -> el<UField> {
                    javaPlugin.convertOpt<UField>(
                            LightClassUtil.getLightClassBackingField(original), givenParent)
                    ?: convertDeclaration(element.parent, givenParent, requiredType)
                }

                is KtFile -> el<UFile> { KotlinUFile(original, this@KotlinUastLanguagePlugin) }
                is FakeFileForLightClass -> el<UFile> { KotlinUFile(original.navigationElement, this@KotlinUastLanguagePlugin) }
                is KtAnnotationEntry -> el<UAnnotation>(build(::KotlinUAnnotation))
                else -> null
            }
        }
    }

    override fun isExpressionValueUsed(element: UExpression): Boolean {
        return when (element) {
            is KotlinUSimpleReferenceExpression.KotlinAccessorCallExpression -> element.setterValue != null
            is KotlinAbstractUExpression -> {
                val ktElement = element.psi as? KtElement ?: return false
                ktElement.analyze()[BindingContext.USED_AS_EXPRESSION, ktElement] ?: false
            }
            else -> false
        }
    }
}

internal inline fun <reified ActualT : UElement> Class<out UElement>?.el(f: () -> UElement?): UElement? {
    return if (this == null || isAssignableFrom(ActualT::class.java)) f() else null
}

internal inline fun <reified ActualT : UElement> Class<out UElement>?.expr(f: () -> UExpression?): UExpression? {
    return if (this == null || isAssignableFrom(ActualT::class.java)) f() else null
}

internal fun UElement?.toCallback() = if (this != null) fun(): UElement? { return this } else null

internal object KotlinConverter {
    internal tailrec fun unwrapElements(element: PsiElement?): PsiElement? = when (element) {
        is KtValueArgumentList -> unwrapElements(element.parent)
        is KtValueArgument -> unwrapElements(element.parent)
        is KtDeclarationModifierList -> unwrapElements(element.parent)
        is KtContainerNode -> unwrapElements(element.parent)
        else -> element
    }

    internal fun convertPsiElement(element: PsiElement?,
                                   givenParent: UElement?,
                                   requiredType: Class<out UElement>?): UElement? {
        fun <P : PsiElement> build(ctor: (P, UElement?) -> UElement): () -> UElement? {
            return { ctor(element as P, givenParent) }
        }

        return with (requiredType) { when (element) {
            is KtParameterList -> el<UDeclarationsExpression> {
                val declarationsExpression = KotlinUDeclarationsExpression(givenParent)
                declarationsExpression.apply {
                    declarations = element.parameters.mapIndexed { i, p ->
                        KotlinUParameter(UastKotlinPsiParameter.create(p, element, declarationsExpression, i), this)
                    }
                }
            }
            is KtClassBody -> el<UExpressionList>(build(KotlinUExpressionList.Companion::createClassBody))
            is KtCatchClause -> el<UCatchClause>(build(::KotlinUCatchClause))
            is KtExpression -> KotlinConverter.convertExpression(element, givenParent, requiredType)
            is KtLambdaArgument -> KotlinConverter.convertExpression(element.getLambdaExpression(), givenParent, requiredType)
            is KtLightAnnotationForSourceEntry.LightExpressionValue<*> -> {
                val expression = element.originalExpression
                when (expression) {
                    is KtExpression -> KotlinConverter.convertExpression(expression, givenParent, requiredType)
                    else -> el<UExpression> { UastEmptyExpression }
                }
            }
            is KtLiteralStringTemplateEntry, is KtEscapeStringTemplateEntry -> el<ULiteralExpression>(build(::KotlinStringULiteralExpression))
            is KtStringTemplateEntry -> element.expression?.let { convertExpression(it, givenParent, requiredType) } ?: expr<UExpression> { UastEmptyExpression }

            else -> {
                if (element is LeafPsiElement && element.elementType == KtTokens.IDENTIFIER) {
                    el<UIdentifier>(build(::UIdentifier))
                } else {
                    null
                }
            }
        }}
    }


    internal fun convertEntry(entry: KtStringTemplateEntry,
                              givenParent: UElement?,
                              requiredType: Class<out UElement>? = null): UExpression? {
        return with(requiredType) {
            if (entry is KtStringTemplateEntryWithExpression) {
                expr<UExpression> {
                    KotlinConverter.convertOrEmpty(entry.expression, givenParent)
                }
            }
            else {
                expr<ULiteralExpression> {
                    if (entry is KtEscapeStringTemplateEntry)
                        KotlinStringULiteralExpression(entry, givenParent, entry.unescapedValue)
                    else
                        KotlinStringULiteralExpression(entry, givenParent)
                }
            }
        }
    }

    internal fun convertExpression(expression: KtExpression,
                                   givenParent: UElement?,
                                   requiredType: Class<out UElement>? = null): UExpression? {
        fun <P : PsiElement> build(ctor: (P, UElement?) -> UExpression): () -> UExpression? {
            return { ctor(expression as P, givenParent) }
        }

        return with (requiredType) { when (expression) {
            is KtVariableDeclaration -> expr<UDeclarationsExpression>(build(::convertVariablesDeclaration))

            is KtStringTemplateExpression -> {
                when {
                    expression.entries.isEmpty() -> {
                        expr<ULiteralExpression> { KotlinStringULiteralExpression(expression, givenParent, "") }
                    }
                    expression.entries.size == 1 -> convertEntry(expression.entries[0], givenParent, requiredType)
                    else -> {
                        expr<UExpression> { KotlinStringTemplateUPolyadicExpression(expression, givenParent) }
                    }
                }
            }
            is KtDestructuringDeclaration -> expr<UDeclarationsExpression> {
                KotlinUDeclarationsExpression(givenParent).apply {
                    val tempAssignment = KotlinULocalVariable(UastKotlinPsiVariable.create(expression, uastParent!!), givenParent)
                    val destructuringAssignments = expression.entries.mapIndexed { i, entry ->
                        val psiFactory = KtPsiFactory(expression.project)
                        val initializer = psiFactory.createAnalyzableExpression("${tempAssignment.name}.component${i + 1}()",
                                                                                expression.containingFile)
                        KotlinULocalVariable(UastKotlinPsiVariable.create(entry, tempAssignment.psi, uastParent!!, initializer), givenParent)
                    }
                    declarations = listOf(tempAssignment) + destructuringAssignments
                }
            }
            is KtLabeledExpression -> expr<ULabeledExpression>(build(::KotlinULabeledExpression))
            is KtClassLiteralExpression -> expr<UClassLiteralExpression>(build(::KotlinUClassLiteralExpression))
            is KtObjectLiteralExpression -> expr<UObjectLiteralExpression>(build(::KotlinUObjectLiteralExpression))
            is KtDotQualifiedExpression -> expr<UQualifiedReferenceExpression>(build(::KotlinUQualifiedReferenceExpression))
            is KtSafeQualifiedExpression -> expr<UQualifiedReferenceExpression>(build(::KotlinUSafeQualifiedExpression))
            is KtSimpleNameExpression -> expr<USimpleNameReferenceExpression>(build(::KotlinUSimpleReferenceExpression))
            is KtCallExpression -> expr<UCallExpression>(build(::KotlinUFunctionCallExpression))
            is KtBinaryExpression -> {
                if (expression.operationToken == KtTokens.ELVIS) {
                    expr<UExpressionList>(build(::createElvisExpression))
                }
                else expr<UBinaryExpression>(build(::KotlinUBinaryExpression))
            }
            is KtParenthesizedExpression -> expr<UParenthesizedExpression>(build(::KotlinUParenthesizedExpression))
            is KtPrefixExpression -> expr<UPrefixExpression>(build(::KotlinUPrefixExpression))
            is KtPostfixExpression -> expr<UPostfixExpression>(build(::KotlinUPostfixExpression))
            is KtThisExpression -> expr<UThisExpression>(build(::KotlinUThisExpression))
            is KtSuperExpression -> expr<USuperExpression>(build(::KotlinUSuperExpression))
            is KtCallableReferenceExpression -> expr<UCallableReferenceExpression>(build(::KotlinUCallableReferenceExpression))
            is KtIsExpression -> expr<UBinaryExpressionWithType>(build(::KotlinUTypeCheckExpression))
            is KtIfExpression -> expr<UIfExpression>(build(::KotlinUIfExpression))
            is KtWhileExpression -> expr<UWhileExpression>(build(::KotlinUWhileExpression))
            is KtDoWhileExpression -> expr<UDoWhileExpression>(build(::KotlinUDoWhileExpression))
            is KtForExpression -> expr<UForEachExpression>(build(::KotlinUForEachExpression))
            is KtWhenExpression -> expr<USwitchExpression>(build(::KotlinUSwitchExpression))
            is KtBreakExpression -> expr<UBreakExpression>(build(::KotlinUBreakExpression))
            is KtContinueExpression -> expr<UContinueExpression>(build(::KotlinUContinueExpression))
            is KtReturnExpression -> expr<UReturnExpression>(build(::KotlinUReturnExpression))
            is KtThrowExpression -> expr<UThrowExpression>(build(::KotlinUThrowExpression))
            is KtBlockExpression -> expr<UBlockExpression>(build(::KotlinUBlockExpression))
            is KtConstantExpression -> expr<ULiteralExpression>(build(::KotlinULiteralExpression))
            is KtTryExpression -> expr<UTryExpression>(build(::KotlinUTryExpression))
            is KtArrayAccessExpression -> expr<UArrayAccessExpression>(build(::KotlinUArrayAccessExpression))
            is KtLambdaExpression -> expr<ULambdaExpression>(build(::KotlinULambdaExpression))
            is KtBinaryExpressionWithTypeRHS -> expr<UBinaryExpressionWithType>(build(::KotlinUBinaryExpressionWithType))
            is KtClassOrObject -> expr<UDeclarationsExpression> {
                expression.toLightClass()?.let { lightClass ->
                    KotlinUDeclarationsExpression(givenParent).apply {
                        declarations = listOf(KotlinUClass.create(lightClass, this))
                    }
                } ?: UastEmptyExpression
            }
            is KtFunction -> if (expression.name.isNullOrEmpty()) {
                expr<ULambdaExpression>(build(::createLocalFunctionLambdaExpression))
            }
            else {
                expr<UDeclarationsExpression>(build(::createLocalFunctionDeclaration))
            }

            else -> expr<UExpression>(build(::UnknownKotlinExpression))
        }}
    }
    
    internal fun convertOrEmpty(expression: KtExpression?, parent: UElement?): UExpression {
        return expression?.let { convertExpression(it, parent, null) } ?: UastEmptyExpression
    }

    internal fun convertOrNull(expression: KtExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent, null) else null
    }

    internal fun KtPsiFactory.createAnalyzableExpression(text: String, context: PsiElement): KtExpression =
            createAnalyzableProperty("val x = $text", context).initializer ?: error("Failed to create expression from text: '$text'")

    internal fun KtPsiFactory.createAnalyzableProperty(text: String, context: PsiElement): KtProperty =
            createAnalyzableDeclaration(text, context)

    internal fun <TDeclaration : KtDeclaration> KtPsiFactory.createAnalyzableDeclaration(text: String, context: PsiElement): TDeclaration {
        val file = createAnalyzableFile("dummy.kt", text, context)
        val declarations = file.declarations
        assert(declarations.size == 1) { "${declarations.size} declarations in $text" }
        return declarations.first() as TDeclaration
    }
}

private fun convertVariablesDeclaration(
        psi: KtVariableDeclaration,
        parent: UElement?
): UDeclarationsExpression {
    val declarationsExpression = KotlinUDeclarationsExpression(parent)
    val parentPsiElement = parent?.psi
    val variable = KotlinUAnnotatedLocalVariable(
            UastKotlinPsiVariable.create(psi, parentPsiElement, declarationsExpression), declarationsExpression) { annotationParent ->
        psi.annotationEntries.map { KotlinUAnnotation(it, annotationParent) }
    }
    return declarationsExpression.apply { declarations = listOf(variable) }
}
