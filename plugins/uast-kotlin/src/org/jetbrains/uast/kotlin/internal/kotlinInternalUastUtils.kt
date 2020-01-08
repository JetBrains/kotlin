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
import com.intellij.openapi.progress.ProcessCanceledException
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
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.type.MapPsiToAsmDesc
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.expressions.KotlinLocalFunctionUVariable
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
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
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
        null -> resolveDeserialized(context, descriptor) as? PsiMethod
        else -> null
    }
}

internal fun resolveContainingDeserializedClass(context: KtElement, memberDescriptor: DeserializedCallableMemberDescriptor): PsiClass? {
    val containingDeclaration = memberDescriptor.containingDeclaration
    return when (containingDeclaration) {
        is LazyJavaPackageFragment -> {
            val binaryPackageSourceElement = containingDeclaration.source as? KotlinJvmBinaryPackageSourceElement ?: return null
            val containingBinaryClass = binaryPackageSourceElement.getContainingBinaryClass(memberDescriptor) ?: return null
            val containingClassQualifiedName = containingBinaryClass.classId.asSingleFqName().asString()
            JavaPsiFacade.getInstance(context.project).findClass(containingClassQualifiedName, context.resolveScope) ?: return null
        }
        is DeserializedClassDescriptor -> {
            val declaredPsiType = containingDeclaration.defaultType.toPsiType(null, context, false)
            (declaredPsiType as? PsiClassType)?.resolve() ?: return null
        }
        else -> return null
    }
}

internal fun resolveToPsiClass(uElement: UElement, declarationDescriptor: DeclarationDescriptor, context: KtElement): PsiClass? =
    when (declarationDescriptor) {
        is ConstructorDescriptor -> declarationDescriptor.returnType
        is ClassDescriptor -> declarationDescriptor.defaultType
        is TypeParameterDescriptor -> declarationDescriptor.defaultType
        is TypeAliasDescriptor -> declarationDescriptor.expandedType
        else -> null
    }?.toPsiType(uElement, context, true).let { PsiTypesUtil.getPsiClass(it) }


internal fun resolveDeserialized(
    context: KtElement,
    descriptor: DeclarationDescriptor,
    accessHint: ReferenceAccess? = null
): PsiModifierListOwner? {
    if (descriptor !is DeserializedCallableMemberDescriptor) return null

    val psiClass = resolveContainingDeserializedClass(context, descriptor) ?: return null

    val proto = descriptor.proto
    val nameResolver = descriptor.nameResolver
    val typeTable = descriptor.typeTable

    return when (proto) {
        is ProtoBuf.Function -> {
            psiClass.getMethodBySignature(
                JvmProtoBufUtil.getJvmMethodSignature(proto, nameResolver, typeTable)
                    ?: getMethodSignatureFromDescriptor(context, descriptor)
            )
        }
        is ProtoBuf.Constructor -> {
            val signature = JvmProtoBufUtil.getJvmConstructorSignature(proto, nameResolver, typeTable)
                ?: getMethodSignatureFromDescriptor(context, descriptor)
                ?: return null

            psiClass.constructors.firstOrNull { it.matchesDesc(signature.desc) }
        }
        is ProtoBuf.Property -> {
            JvmProtoBufUtil.getJvmFieldSignature(proto, nameResolver, typeTable, false)
                ?.let { signature -> psiClass.fields.firstOrNull { it.name == signature.name } }
                ?.let { return it }

            val propertySignature = proto.getExtensionOrNull(JvmProtoBuf.propertySignature)
            if (propertySignature != null) {
                with(propertySignature) {
                    when {
                        hasGetter() && accessHint?.isRead != false -> getter
                        hasSetter() && accessHint?.isWrite != false -> setter
                        else -> null // it should have been handled by the previous case
                    }
                }?.let { methodSignature ->
                    psiClass.getMethodBySignature(
                        nameResolver.getString(methodSignature.name),
                        if (methodSignature.hasDesc()) nameResolver.getString(methodSignature.desc) else null
                    )
                }?.let { return it }

            } else if (proto.hasName()) {
                // Property without a Property signature, looks like a @JvmField
                val name = nameResolver.getString(proto.name)
                psiClass.fields
                    .firstOrNull { it.name == name }
                    ?.let { return it }
            }

            getMethodSignatureFromDescriptor(context, descriptor)
                ?.let { signature -> psiClass.getMethodBySignature(signature) }
                ?.let { return it }
        }
        else -> null
    }
}

private fun PsiClass.getMethodBySignature(methodSignature: JvmMemberSignature?) = methodSignature?.let { signature ->
    getMethodBySignature(signature.name, signature.desc)
}

private fun PsiClass.getMethodBySignature(name: String, descr: String?) =
    methods.firstOrNull { method -> method.name == name && descr?.let { method.matchesDesc(it) } ?: true }

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
    toPsiType(source.getParentOfType<UDeclaration>(false)?.javaPsi as? PsiModifierListOwner, element, boxed)

internal fun KotlinType.toPsiType(lightDeclaration: PsiModifierListOwner?, context: KtElement, boxed: Boolean): PsiType {
    if (this.isError) return UastErrorType

    (constructor.declarationDescriptor as? TypeAliasDescriptor)?.let { typeAlias ->
        return typeAlias.expandedType.toPsiType(lightDeclaration, context, boxed)
    }

    if (contains { type -> type.constructor is TypeVariableTypeConstructor }) {
        return UastErrorType
    }

    (constructor.declarationDescriptor as? TypeParameterDescriptor)?.let { typeParameter ->
        (typeParameter.containingDeclaration.toSource()?.getMaybeLightElement() as? PsiTypeParameterListOwner)
            ?.typeParameterList?.typeParameters?.getOrNull(typeParameter.index)
            ?.let { return PsiTypesUtil.getClassType(it) }
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
            "kotlin.Unit" -> PsiType.VOID.orBoxed()
            "kotlin.String" -> PsiType.getJavaLangString(context.manager, context.resolveScope)
            else -> {
                val typeConstructor = this.constructor
                when (typeConstructor) {
                    is IntegerValueTypeConstructor -> TypeUtils.getDefaultPrimitiveNumberType(typeConstructor).toPsiType(lightDeclaration, context, boxed)
                    is IntegerLiteralTypeConstructor -> typeConstructor.getApproximatedType().toPsiType(lightDeclaration, context, boxed)
                    else -> null
                }
            }
        }
        if (psiType != null) return psiType
    }

    if (this.containsLocalTypes()) return UastErrorType

    val project = context.project

    val typeMapper = ServiceManager.getService(project, KotlinUastResolveProviderService::class.java)
        .getTypeMapper(context) ?: return UastErrorType

    val languageVersionSettings = ServiceManager.getService(project, KotlinUastResolveProviderService::class.java)
        .getLanguageVersionSettings(context)

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
    val typeMappingMode = if (boxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT
    val approximatedType = TypeApproximator(this.builtIns).approximateDeclarationType(this, true, languageVersionSettings)
    typeMapper.mapType(approximatedType, signatureWriter, typeMappingMode)

    val signature = StringCharacterIterator(signatureWriter.toString())

    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return UastErrorType

    val parent: PsiElement = lightDeclaration ?: context
    if (parent.containingFile == null) {
        Logger.getInstance("org.jetbrains.uast.kotlin.KotlinInternalUastUtils")
            .error(
                "initialising ClsTypeElementImpl with null-file parent = $parent (of ${parent.javaClass}) " +
                        "containing class = ${parent.safeAs<PsiMethod>()?.containingClass}, " +
                        "lightDeclaration = $lightDeclaration (of ${lightDeclaration?.javaClass})," +
                        " context = $context (of ${context.javaClass})"
            )
    }
    return ClsTypeElementImpl(parent, typeText, '\u0000').type
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

internal fun PsiElement.getMaybeLightElement(): PsiElement? {
    return when (this) {
        is KtDeclaration -> {
            val lightElement = toLightElements().firstOrNull()
            if (lightElement != null) return lightElement

            if (this is KtPrimaryConstructor) {
                // annotations don't have constructors (but in Kotlin they do), so resolving to the class here
                (this.parent as? KtClassOrObject)?.takeIf { it.isAnnotation() }?.toLightClass()?.let { return it }
            }

            when (val uElement = this.toUElement()) {
                is UDeclaration -> uElement.javaPsi
                is UDeclarationsExpression -> uElement.declarations.firstOrNull()?.javaPsi
                is ULambdaExpression -> (uElement.uastParent as? KotlinLocalFunctionUVariable)?.javaPsi
                else -> null
            }
        }
        is KtElement -> null
        else -> this
    }
}

internal fun KtElement.resolveCallToDeclaration(resultingDescriptor: DeclarationDescriptor? = null): PsiElement? {
    val descriptor = resultingDescriptor ?: run {
        val resolvedCall = getResolvedCall(analyze()) ?: return null
        resolvedCall.resultingDescriptor
    }

    return descriptor.toSource()?.getMaybeLightElement()
}

internal fun KtExpression.unwrapBlockOrParenthesis(): KtExpression {
    val innerExpression = KtPsiUtil.safeDeparenthesize(this)
    if (innerExpression is KtBlockExpression) {
        val statement = innerExpression.statements.singleOrNull() ?: return this
        return KtPsiUtil.safeDeparenthesize(statement)
    }
    return innerExpression
}

internal fun KtElement.canAnalyze(): Boolean {
    if (!isValid) return false
    val containingFile = containingFile as? KtFile ?: return false // EA-114080, EA-113475, EA-134193
    if (containingFile.doNotAnalyze != null) return false // To prevent exceptions during analysis
    return true
}

internal fun KtElement.analyze(): BindingContext {
    if (!canAnalyze()) return BindingContext.EMPTY
    return ServiceManager.getService(project, KotlinUastResolveProviderService::class.java)
        ?.getBindingContext(this) ?: BindingContext.EMPTY
}

internal inline fun <reified T : UDeclaration, reified P : PsiElement> unwrap(element: P): P {
    val unwrapped = if (element is T) element.javaPsi else element
    assert(unwrapped !is UElement)
    return unwrapped as P
}

internal fun KtExpression.getExpectedType(): KotlinType? = analyze()[BindingContext.EXPECTED_EXPRESSION_TYPE, this]

internal fun KtTypeReference.getType(): KotlinType? = analyze()[BindingContext.TYPE, this]

internal val KtTypeReference.nameElement: PsiElement?
    get() = this.typeElement?.let {
        (it as? KtUserType)?.referenceExpression?.getReferencedNameElement() ?: it.navigationElement
    }

internal fun KotlinType.getFunctionalInterfaceType(source: UElement, element: KtElement): PsiType? =
    takeIf { it.isInterface() && !it.isBuiltinFunctionalTypeOrSubtype }?.toPsiType(source, element, false)

internal fun KotlinULambdaExpression.getFunctionalInterfaceType(): PsiType? {
    val parent = sourcePsi.parent
    if (parent is KtBinaryExpressionWithTypeRHS) return parent.right?.getType()?.getFunctionalInterfaceType(this, sourcePsi)
    if (parent is KtValueArgument) run {
        val callExpression = parent.parents.take(2).firstIsInstanceOrNull<KtCallExpression>() ?: return@run
        val resolvedCall = callExpression.getResolvedCall(callExpression.analyze()) ?: return@run

        // NewResolvedCallImpl can be used as a marker meaning that this code is working under *new* inference
        if (resolvedCall is NewResolvedCallImpl) {
            val samConvertedArgument = resolvedCall.getExpectedTypeForSamConvertedArgument(parent)

            // Same as if in old inference we would get SamDescriptor
            if (samConvertedArgument != null) {
                val type = getTypeByArgument(resolvedCall, resolvedCall.candidateDescriptor, parent) ?: return@run
                return type.getFunctionalInterfaceType(this, sourcePsi)
            }
        }

        val candidateDescriptor = resolvedCall.candidateDescriptor as? SyntheticMemberDescriptor<*> ?: return@run
        when (candidateDescriptor) {
            is SamConstructorDescriptor -> return candidateDescriptor.returnType?.getFunctionalInterfaceType(this, sourcePsi)
            is SamAdapterDescriptor<*>, is SamAdapterExtensionFunctionDescriptor -> {
                val functionDescriptor = candidateDescriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return@run

                val type = getTypeByArgument(resolvedCall, functionDescriptor, parent) ?: return@run
                return type.getFunctionalInterfaceType(this, sourcePsi)
            }
        }
    }
    return sourcePsi.getExpectedType()?.getFunctionalInterfaceType(this, sourcePsi)
}

private fun getTypeByArgument(
    resolvedCall: ResolvedCall<*>,
    descriptor: CallableDescriptor,
    argument: ValueArgument
): KotlinType? {
    val index = (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter?.index ?: return null
    val parameterDescriptor = descriptor.valueParameters.getOrNull(index) ?: return null

    return parameterDescriptor.type
}

internal fun unwrapFakeFileForLightClass(file: PsiFile): PsiFile = (file as? FakeFileForLightClass)?.ktFile ?: file

// mb merge with org.jetbrains.kotlin.idea.references.ReferenceAccess ?
internal enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}


