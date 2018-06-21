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
import com.intellij.psi.*
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalTypeOrSubtype
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.type.MapPsiToAsmDesc
import org.jetbrains.kotlin.types.*
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

internal fun resolveSource(context: KtElement, descriptor: DeclarationDescriptor, source: PsiElement?): PsiMethod? {

    if (descriptor is ConstructorDescriptor && descriptor.isPrimary
        && source is KtClassOrObject && source.primaryConstructor == null
        && source.secondaryConstructors.isEmpty()
    ) {
        return source.toLightClass()?.constructors?.firstOrNull()
    }

    return when (source) {
        is KtFunction -> LightClassUtil.getLightClassMethod(source)
        is PsiMethod -> source
        null -> resolveDeserialized(context, descriptor)
        else -> null
    }
}

private fun resolveDeserialized(context: KtElement, descriptor: DeclarationDescriptor): PsiMethod? {
    if (descriptor !is DeserializedCallableMemberDescriptor) return null

    val containingDeclaration = descriptor.containingDeclaration
    val psiClass = when (containingDeclaration) {
        is LazyJavaPackageFragment -> {
            val binaryPackageSourceElement = containingDeclaration.source as? KotlinJvmBinaryPackageSourceElement ?: return null
            val containingBinaryClass = binaryPackageSourceElement.getContainingBinaryClass(descriptor) ?: return null
            val containingClassQualifiedName = containingBinaryClass.classId.asSingleFqName().asString()
            JavaPsiFacade.getInstance(context.project).findClass(containingClassQualifiedName, context.resolveScope) ?: return null
        }
        is DeserializedClassDescriptor -> {
            val declaredPsiType = containingDeclaration.defaultType.toPsiType(null, context, false)
            (declaredPsiType as? PsiClassType)?.resolve() ?: return null
        }
        else -> return null
    }


    val proto = descriptor.proto
    val nameResolver = descriptor.nameResolver
    val typeTable = descriptor.typeTable

    return when (proto) {
        is ProtoBuf.Function -> {
            val signature = JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable)
                    ?: getMethodSignatureFromDescriptor(context, descriptor)
                    ?: return null

            psiClass.methods.firstOrNull { it.name == signature.name && it.matchesDesc(signature.desc) }
        }
        is ProtoBuf.Constructor -> {
            val signature = JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable)
                    ?: getMethodSignatureFromDescriptor(context, descriptor)
                    ?: return null

            psiClass.constructors.firstOrNull { it.matchesDesc(signature.desc) }
        }
        else -> null
    }
}

private fun PsiMethod.matchesDesc(desc: String) = desc == buildString {
    parameterList.parameters.joinTo(this, separator = "", prefix = "(", postfix = ")") { MapPsiToAsmDesc.typeDesc(it.type) }
    append(MapPsiToAsmDesc.typeDesc(returnType ?: PsiType.VOID))
}

private fun getMethodSignatureFromDescriptor(context: KtElement, descriptor: CallableDescriptor): JvmMemberSignature? {
    fun PsiType.raw() = (this as? PsiClassType)?.rawType() ?: PsiPrimitiveType.getUnboxedType(this) ?: this
    fun KotlinType.toPsiType() = toPsiType(null, context, false).raw()

    val originalDescriptor = descriptor.original
    val receiverType = originalDescriptor.extensionReceiverParameter?.type?.toPsiType()
    val parameterTypes = listOfNotNull(receiverType) + originalDescriptor.valueParameters.map { it.type.toPsiType() }
    val returnType = originalDescriptor.returnType?.toPsiType() ?: PsiType.VOID

    val desc = parameterTypes.joinToString("", prefix = "(", postfix = ")") { MapPsiToAsmDesc.typeDesc(it) } +
            MapPsiToAsmDesc.typeDesc(returnType)

    return JvmMemberSignature.Method(descriptor.name.asString(), desc)
}

internal fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.SYNCHRONIZED, initializer)

internal fun KotlinType.toPsiType(source: UElement, element: KtElement, boxed: Boolean): PsiType =
    toPsiType(source.getParentOfType<UDeclaration>(false)?.psi, element, boxed)

internal fun KotlinType.toPsiType(lightDeclaration: PsiModifierListOwner?, context: KtElement, boxed: Boolean): PsiType {
    if (this.isError) return UastErrorType

    (constructor.declarationDescriptor as? TypeAliasDescriptor)?.let { typeAlias ->
        return typeAlias.expandedType.toPsiType(lightDeclaration, context, boxed)
    }

    (constructor.declarationDescriptor as? TypeParameterDescriptor)?.let { typeParameter ->
        return CommonSupertypes.commonSupertype(typeParameter.upperBounds).toPsiType(lightDeclaration, context, boxed)
    }

    if (arguments.isEmpty()) {
        val typeFqName = this.constructor.declarationDescriptor?.fqNameSafe?.asString()
        fun PsiPrimitiveType.orBoxed() = if (boxed) getBoxedType(context) else this
        val psiType = when (typeFqName) {
            "kotlin.Int" -> PsiType.INT.orBoxed()
            "kotlin.Long" -> PsiType.LONG.orBoxed()
            "kotlin.Short" -> PsiType.SHORT.orBoxed()
            "kotlin.Boolean" -> PsiType.BOOLEAN.orBoxed()
            "kotlin.Byte" -> PsiType.BYTE.orBoxed()
            "kotlin.Char" -> PsiType.CHAR.orBoxed()
            "kotlin.Double" -> PsiType.DOUBLE.orBoxed()
            "kotlin.Float" -> PsiType.FLOAT.orBoxed()
            "kotlin.String" -> PsiType.getJavaLangString(context.manager, context.resolveScope)
            else -> {
                val typeConstructor = this.constructor
                if (typeConstructor is IntegerValueTypeConstructor) {
                    TypeUtils.getDefaultPrimitiveNumberType(typeConstructor).toPsiType(lightDeclaration, context, boxed)
                } else {
                    null
                }
            }
        }
        if (psiType != null) return psiType
    }

    if (this.containsLocalTypes()) return UastErrorType

    val project = context.project
    val typeMapper = ServiceManager.getService(project, KotlinUastBindingContextProviderService::class.java)
        .getTypeMapper(context) ?: return UastErrorType

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
    val typeMappingMode = if (boxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT
    val approximatedType = TypeApproximator().approximateDeclarationType(this, true, context.languageVersionSettings)
    typeMapper.mapType(approximatedType, signatureWriter, typeMappingMode)

    val signature = StringCharacterIterator(signatureWriter.toString())

    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return UastErrorType

    return ClsTypeElementImpl(lightDeclaration ?: context, typeText, '\u0000').type
}

private fun KotlinType.containsLocalTypes(): Boolean {
    val typeDeclarationDescriptor = this.constructor.declarationDescriptor
    if (typeDeclarationDescriptor is ClassDescriptor && DescriptorUtils.isLocal(typeDeclarationDescriptor)) {
        return true
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
    if(containingFile !is KtFile) return BindingContext.EMPTY // EA-114080, EA-113475
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

internal fun unwrapFakeFileForLightClass(file: PsiFile): PsiFile = (file as? FakeFileForLightClass)?.ktFile ?: file
