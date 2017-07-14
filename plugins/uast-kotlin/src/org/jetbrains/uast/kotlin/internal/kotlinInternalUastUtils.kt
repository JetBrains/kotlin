/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.uast.*
import java.lang.ref.WeakReference
import java.text.StringCharacterIterator

internal val KOTLIN_CACHED_UELEMENT_KEY = Key.create<WeakReference<UElement>>("cached-kotlin-uelement")

@Suppress("NOTHING_TO_INLINE")
internal inline fun String?.orAnonymous(kind: String = ""): String = this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"

internal fun DeclarationDescriptor.toSource(): PsiElement? {
    return try {
        DescriptorToSourceUtils.getEffectiveReferencedDescriptors(this)
                .asSequence()
                .mapNotNull { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                .firstOrNull()
    }
    catch (e: Exception) {
        Logger.getInstance("DeclarationDescriptor.toSource").error(e)
        null
    }
}

internal fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

internal fun KotlinType.toPsiType(source: UElement, element: KtElement, boxed: Boolean): PsiType {
    if (this.isError) return UastErrorType

    if (arguments.isEmpty()) {
        val typeFqName = this.constructor.declarationDescriptor?.fqNameSafe?.asString()
        fun PsiPrimitiveType.orBoxed() = if (boxed) getBoxedType(element) else this
        val psiType = when (typeFqName) {
            "kotlin.Int" -> PsiType.INT.orBoxed()
            "kotlin.Long" -> PsiType.LONG.orBoxed()
            "kotlin.Short" -> PsiType.SHORT.orBoxed()
            "kotlin.Boolean" -> PsiType.BOOLEAN.orBoxed()
            "kotlin.Byte" -> PsiType.BYTE.orBoxed()
            "kotlin.Char" -> PsiType.CHAR.orBoxed()
            "kotlin.Double" -> PsiType.DOUBLE.orBoxed()
            "kotlin.Float" -> PsiType.FLOAT.orBoxed()
            "kotlin.String" -> PsiType.getJavaLangString(element.manager, GlobalSearchScope.projectScope(element.project))
            else -> null
        }
        if (psiType != null) return psiType
    }

    if (this.containsLocalTypes()) return UastErrorType

    val project = element.project
    val typeMapper = ServiceManager.getService(project, KotlinUastBindingContextProviderService::class.java)
            .getTypeMapper(element) ?: return UastErrorType

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
    val typeMappingMode = if (boxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT
    typeMapper.mapType(this, signatureWriter, typeMappingMode)

    val signature = StringCharacterIterator(signatureWriter.toString())

    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return UastErrorType

    return ClsTypeElementImpl(source.getParentOfType<UDeclaration>(false)?.psi ?: element, typeText, '\u0000').type
}

private fun KotlinType.containsLocalTypes(): Boolean {
    val typeDeclarationDescriptor = this.constructor.declarationDescriptor
    if (typeDeclarationDescriptor is ClassDescriptor) {
        val containerDescriptor = typeDeclarationDescriptor.containingDeclaration
        if (containerDescriptor is PropertyDescriptor || containerDescriptor is FunctionDescriptor) {
            return true
        }
    }

    return arguments.any { !it.isStarProjection && it.type.containsLocalTypes() }
}

internal fun KtTypeReference?.toPsiType(source: UElement, boxed: Boolean = false): PsiType {
    if (this == null) return UastErrorType
    return (analyze()[BindingContext.TYPE, this] ?: return UastErrorType).toPsiType(source, this, boxed)
}

internal fun KtClassOrObject.toPsiType(): PsiType {
    val lightClass = toLightClass() ?: return UastErrorType
    return PsiTypesUtil.getClassType(lightClass)
}

internal fun PsiElement.getMaybeLightElement(context: UElement): PsiElement? {
    return when (this) {
        is KtVariableDeclaration -> {
            val lightElement = toLightElements().firstOrNull()
            if (lightElement != null) return lightElement

            val languagePlugin = context.getLanguagePlugin()
            val uElement = languagePlugin.convertElementWithParent(this, null)
            when (uElement) {
                is UDeclaration -> uElement.psi
                is UDeclarationsExpression -> uElement.declarations.firstOrNull()?.psi
                else -> null
            }
        }
        is KtDeclaration -> toLightElements().firstOrNull()
        is KtElement -> null
        else -> this
    }
}

internal fun KtElement.resolveCallToDeclaration(
        context: KotlinAbstractUElement,
        resultingDescriptor: DeclarationDescriptor? = null
): PsiElement? {
    val descriptor = resultingDescriptor ?: run {
        val resolvedCall = getResolvedCall(analyze()) ?: return null
        resolvedCall.resultingDescriptor
    }

    return descriptor.toSource()?.getMaybeLightElement(context)
}

internal fun KtExpression.unwrapBlockOrParenthesis(): KtExpression {
    val innerExpression = KtPsiUtil.safeDeparenthesize(this)
    if (innerExpression is KtBlockExpression) {
        val statement = innerExpression.statements.singleOrNull() ?: return this
        return KtPsiUtil.safeDeparenthesize(statement)
    }
    return innerExpression
}

internal fun KtElement.analyze(): BindingContext {
    return ServiceManager.getService(project, KotlinUastBindingContextProviderService::class.java)
            ?.getBindingContext(this) ?: BindingContext.EMPTY
}

internal inline fun <reified T : UDeclaration, reified P : PsiElement> unwrap(element: P): P {
    val unwrapped = if (element is T) element.psi else element
    assert(unwrapped !is UElement)
    return unwrapped as P
}

internal fun KtExpression.getExpectedType(): KotlinType? = analyze()[BindingContext.EXPECTED_EXPRESSION_TYPE, this]

internal fun KtTypeReference.getType(): KotlinType? = analyze()[BindingContext.TYPE, this]

internal fun KotlinType.getFunctionalInterfaceType(source: UElement, element: KtElement): PsiType? =
        takeIf { it.isInterface() && !it.isBuiltinFunctionalTypeOrSubtype }?.toPsiType(source, element, false)

internal fun KotlinULambdaExpression.getFunctionalInterfaceType(): PsiType? {
    val parent = psi.parent
    return when(parent) {
        is KtBinaryExpressionWithTypeRHS -> parent.right?.getType()?.getFunctionalInterfaceType(this, psi)
        else -> psi.getExpectedType()?.getFunctionalInterfaceType(this, psi)
    }
}
