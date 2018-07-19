/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.deserialization.Flags as F

class ReadContext(
    val strings: NameResolver,
    val types: TypeTable,
    internal val versionRequirements: VersionRequirementTable,
    private val parent: ReadContext? = null
) {
    internal val extensions = MetadataExtensions.INSTANCES
    private val typeParameterNameToId = mutableMapOf<Int, Int>()

    operator fun get(index: Int): String =
        strings.getString(index)

    fun className(index: Int): ClassName =
        strings.getClassName(index)

    fun getTypeParameterId(name: Int): Int? =
        typeParameterNameToId[name] ?: parent?.getTypeParameterId(name)

    fun withTypeParameters(typeParameters: List<ProtoBuf.TypeParameter>): ReadContext =
        ReadContext(strings, types, versionRequirements, this).apply {
            for (typeParameter in typeParameters) {
                typeParameterNameToId[typeParameter.name] = typeParameter.id
            }
        }
}

fun ProtoBuf.Class.accept(v: KmClassVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.create(versionRequirementTable))
        .withTypeParameters(typeParameterList)

    v.visit(flags, c.className(fqName))

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    for (supertype in supertypes(c.types)) {
        v.visitSupertype(supertype.typeFlags)?.let { supertype.accept(it, c) }
    }

    for (constructor in constructorList) {
        v.visitConstructor(constructor.flags)?.let { constructor.accept(it, c) }
    }

    v.visitDeclarations(functionList, propertyList, typeAliasList, c)

    if (hasCompanionObjectName()) {
        v.visitCompanionObject(c[companionObjectName])
    }

    for (nestedClassName in nestedClassNameList) {
        v.visitNestedClass(c[nestedClassName])
    }

    for (enumEntry in enumEntryList) {
        if (!enumEntry.hasName()) {
            throw InconsistentKotlinMetadataException("No name for EnumEntry")
        }
        v.visitEnumEntry(c[enumEntry.name])
    }

    for (sealedSubclassFqName in sealedSubclassFqNameList) {
        v.visitSealedSubclass(c.className(sealedSubclassFqName))
    }

    for (versionRequirement in versionRequirementList) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(versionRequirement, it, c) }
    }

    for (extension in c.extensions) {
        extension.readClassExtensions(v, this, c)
    }

    v.visitEnd()
}

fun ProtoBuf.Package.accept(v: KmPackageVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.create(versionRequirementTable))

    v.visitDeclarations(functionList, propertyList, typeAliasList, c)

    for (extension in c.extensions) {
        extension.readPackageExtensions(v, this, c)
    }

    v.visitEnd()
}

private fun KmDeclarationContainerVisitor.visitDeclarations(
    functions: List<ProtoBuf.Function>,
    properties: List<ProtoBuf.Property>,
    typeAliases: List<ProtoBuf.TypeAlias>,
    c: ReadContext
) {
    for (function in functions) {
        visitFunction(function.flags, c[function.name])?.let { function.accept(it, c) }
    }

    for (property in properties) {
        visitProperty(
            property.flags, c[property.name], property.getPropertyGetterFlags(), property.getPropertySetterFlags()
        )?.let { property.accept(it, c) }
    }

    for (typeAlias in typeAliases) {
        visitTypeAlias(typeAlias.flags, c[typeAlias.name])?.let { typeAlias.accept(it, c) }
    }
}

fun ProtoBuf.Function.accept(v: KmLambdaVisitor, strings: NameResolver) {
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.EMPTY)

    v.visitFunction(flags, c[name])?.let { accept(it, c) }

    v.visitEnd()
}

private fun ProtoBuf.Constructor.accept(v: KmConstructorVisitor, c: ReadContext) {
    for (parameter in valueParameterList) {
        v.visitValueParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    for (versionRequirement in versionRequirementList) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(versionRequirement, it, c) }
    }

    for (extension in c.extensions) {
        extension.readConstructorExtensions(v, this, c)
    }

    v.visitEnd()
}

private fun ProtoBuf.Function.accept(v: KmFunctionVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    receiverType(c.types)?.let { receiverType ->
        v.visitReceiverParameterType(receiverType.typeFlags)?.let { receiverType.accept(it, c) }
    }

    for (parameter in valueParameterList) {
        v.visitValueParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    returnType(c.types).let { returnType ->
        v.visitReturnType(returnType.typeFlags)?.let { returnType.accept(it, c) }
    }

    if (hasContract()) {
        v.visitContract()?.let { contract.accept(it, c) }
    }

    for (versionRequirement in versionRequirementList) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(versionRequirement, it, c) }
    }

    for (extension in c.extensions) {
        extension.readFunctionExtensions(v, this, c)
    }

    v.visitEnd()
}

fun ProtoBuf.Property.accept(v: KmPropertyVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    receiverType(c.types)?.let { receiverType ->
        v.visitReceiverParameterType(receiverType.typeFlags)?.let { receiverType.accept(it, c) }
    }

    if (hasSetterValueParameter()) {
        val parameter = setterValueParameter
        v.visitSetterParameter(parameter.flags, c[parameter.name])?.let { parameter.accept(it, c) }
    }

    returnType(c.types).let { returnType ->
        v.visitReturnType(returnType.typeFlags)?.let { returnType.accept(it, c) }
    }

    for (versionRequirement in versionRequirementList) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(versionRequirement, it, c) }
    }

    for (extension in c.extensions) {
        extension.readPropertyExtensions(v, this, c)
    }

    v.visitEnd()
}

private fun ProtoBuf.TypeAlias.accept(v: KmTypeAliasVisitor, outer: ReadContext) {
    val c = outer.withTypeParameters(typeParameterList)

    for (typeParameter in typeParameterList) {
        typeParameter.accept(v::visitTypeParameter, c)
    }

    underlyingType(c.types).let { underlyingType ->
        v.visitUnderlyingType(underlyingType.typeFlags)?.let { underlyingType.accept(it, c) }
    }

    expandedType(c.types).let { expandedType ->
        v.visitExpandedType(expandedType.typeFlags)?.let { expandedType.accept(it, c) }
    }

    for (annotation in annotationList) {
        v.visitAnnotation(annotation.readAnnotation(c.strings))
    }

    for (versionRequirement in versionRequirementList) {
        v.visitVersionRequirement()?.let { acceptVersionRequirementVisitor(versionRequirement, it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.ValueParameter.accept(v: KmValueParameterVisitor, c: ReadContext) {
    type(c.types).let { type ->
        v.visitType(type.typeFlags)?.let { type.accept(it, c) }
    }

    varargElementType(c.types)?.let { varargElementType ->
        v.visitVarargElementType(varargElementType.typeFlags)?.let { varargElementType.accept(it, c) }
    }

    v.visitEnd()
}

private inline fun ProtoBuf.TypeParameter.accept(
    visit: (flags: Flags, name: String, id: Int, variance: KmVariance) -> KmTypeParameterVisitor?,
    c: ReadContext
) {
    val variance = when (variance!!) {
        ProtoBuf.TypeParameter.Variance.IN -> KmVariance.IN
        ProtoBuf.TypeParameter.Variance.OUT -> KmVariance.OUT
        ProtoBuf.TypeParameter.Variance.INV -> KmVariance.INVARIANT
    }

    visit(typeParameterFlags, c[name], id, variance)?.let { accept(it, c) }
}

private fun ProtoBuf.TypeParameter.accept(v: KmTypeParameterVisitor, c: ReadContext) {
    for (upperBound in upperBounds(c.types)) {
        v.visitUpperBound(upperBound.typeFlags)?.let { upperBound.accept(it, c) }
    }

    for (extension in c.extensions) {
        extension.readTypeParameterExtensions(v, this, c)
    }

    v.visitEnd()
}

private fun ProtoBuf.Type.accept(v: KmTypeVisitor, c: ReadContext) {
    when {
        hasClassName() -> v.visitClass(c.className(className))
        hasTypeAliasName() -> v.visitTypeAlias(c.className(typeAliasName))
        hasTypeParameter() -> v.visitTypeParameter(typeParameter)
        hasTypeParameterName() -> {
            val id = c.getTypeParameterId(typeParameterName)
                    ?: throw InconsistentKotlinMetadataException("No type parameter id for ${c[typeParameterName]}")
            v.visitTypeParameter(id)
        }
        else -> {
            throw InconsistentKotlinMetadataException("No classifier (class, type alias or type parameter) recorded for Type")
        }
    }

    for (argument in argumentList) {
        val variance = when (argument.projection!!) {
            ProtoBuf.Type.Argument.Projection.IN -> KmVariance.IN
            ProtoBuf.Type.Argument.Projection.OUT -> KmVariance.OUT
            ProtoBuf.Type.Argument.Projection.INV -> KmVariance.INVARIANT
            ProtoBuf.Type.Argument.Projection.STAR -> null
        }

        if (variance != null) {
            val argumentType = argument.type(c.types)
                    ?: throw InconsistentKotlinMetadataException("No type argument for non-STAR projection in Type")
            v.visitArgument(argumentType.typeFlags, variance)?.let { argumentType.accept(it, c) }
        } else {
            v.visitStarProjection()
        }
    }

    abbreviatedType(c.types)?.let { abbreviatedType ->
        v.visitAbbreviatedType(abbreviatedType.typeFlags)?.let { abbreviatedType.accept(it, c) }
    }

    outerType(c.types)?.let { outerType ->
        v.visitOuterType(outerType.typeFlags)?.let { outerType.accept(it, c) }
    }

    flexibleUpperBound(c.types)?.let { upperBound ->
        v.visitFlexibleTypeUpperBound(
            upperBound.typeFlags,
            if (hasFlexibleTypeCapabilitiesId()) c[flexibleTypeCapabilitiesId] else null
        )?.let { upperBound.accept(it, c) }
    }

    for (extension in c.extensions) {
        extension.readTypeExtensions(v, this, c)
    }

    v.visitEnd()
}

private fun acceptVersionRequirementVisitor(id: Int, v: KmVersionRequirementVisitor, c: ReadContext) {
    val message = VersionRequirement.create(id, c.strings, c.versionRequirements)
        ?: throw InconsistentKotlinMetadataException("No VersionRequirement with the given id in the table")

    val kind = when (message.kind) {
        ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION -> KmVersionRequirementVersionKind.LANGUAGE_VERSION
        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION -> KmVersionRequirementVersionKind.COMPILER_VERSION
        ProtoBuf.VersionRequirement.VersionKind.API_VERSION -> KmVersionRequirementVersionKind.API_VERSION
    }

    val level = when (message.level) {
        DeprecationLevel.WARNING -> KmVersionRequirementLevel.WARNING
        DeprecationLevel.ERROR -> KmVersionRequirementLevel.ERROR
        DeprecationLevel.HIDDEN -> KmVersionRequirementLevel.HIDDEN
    }

    v.visit(kind, level, message.errorCode, message.message)

    val (major, minor, patch) = message.version
    v.visitVersion(major, minor, patch)

    v.visitEnd()
}

private fun ProtoBuf.Contract.accept(v: KmContractVisitor, c: ReadContext) {
    for (effect in effectList) {
        if (!effect.hasEffectType()) continue

        val effectType = when (effect.effectType!!) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> KmEffectType.RETURNS_CONSTANT
            ProtoBuf.Effect.EffectType.CALLS -> KmEffectType.CALLS
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> KmEffectType.RETURNS_NOT_NULL
        }

        val effectKind = if (!effect.hasKind()) null else when (effect.kind!!) {
            ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> KmEffectInvocationKind.AT_MOST_ONCE
            ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> KmEffectInvocationKind.EXACTLY_ONCE
            ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> KmEffectInvocationKind.AT_LEAST_ONCE
        }

        v.visitEffect(effectType, effectKind)?.let { effect.accept(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Effect.accept(v: KmEffectVisitor, c: ReadContext) {
    for (constructorArgument in effectConstructorArgumentList) {
        v.visitConstructorArgument()?.let { constructorArgument.accept(it, c) }
    }

    if (hasConclusionOfConditionalEffect()) {
        v.visitConclusionOfConditionalEffect()?.let { conclusionOfConditionalEffect.accept(it, c) }
    }

    v.visitEnd()
}

private fun ProtoBuf.Expression.accept(v: KmEffectExpressionVisitor, c: ReadContext) {
    v.visit(
        flags,
        if (hasValueParameterReference()) valueParameterReference else null
    )

    if (hasConstantValue()) {
        v.visitConstantValue(
            when (constantValue!!) {
                ProtoBuf.Expression.ConstantValue.TRUE -> true
                ProtoBuf.Expression.ConstantValue.FALSE -> false
                ProtoBuf.Expression.ConstantValue.NULL -> null
            }
        )
    }

    isInstanceType(c.types)?.let { type ->
        v.visitIsInstanceType(type.typeFlags)?.let { type.accept(it, c) }
    }

    for (andArgument in andArgumentList) {
        v.visitAndArgument()?.let { andArgument.accept(it, c) }
    }

    for (orArgument in orArgumentList) {
        v.visitOrArgument()?.let { orArgument.accept(it, c) }
    }

    v.visitEnd()
}

private val ProtoBuf.Type.typeFlags: Flags
    get() = (if (nullable) 1 shl 0 else 0) +
            (flags shl 1)

private val ProtoBuf.TypeParameter.typeParameterFlags: Flags
    get() = if (reified) 1 else 0

fun ProtoBuf.Property.getPropertyGetterFlags(): Flags =
    if (hasGetterFlags()) getterFlags else getDefaultPropertyAccessorFlags(flags)

fun ProtoBuf.Property.getPropertySetterFlags(): Flags =
    if (hasSetterFlags()) setterFlags else getDefaultPropertyAccessorFlags(flags)

private fun getDefaultPropertyAccessorFlags(flags: Flags): Flags =
    F.getAccessorFlags(F.HAS_ANNOTATIONS.get(flags), F.VISIBILITY.get(flags), F.MODALITY.get(flags), false, false, false)
