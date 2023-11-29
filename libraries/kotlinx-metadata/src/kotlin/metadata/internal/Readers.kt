/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR") // flags will become internal eventually

package kotlin.metadata.internal

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import kotlin.contracts.ExperimentalContracts
import org.jetbrains.kotlin.metadata.deserialization.Flags as F

/**
 * Allows to populate [ReadContext] with additional data
 * that can be used when reading metadata in [MetadataExtensions].
 */
public interface ReadContextExtension

public class ReadContext(
    public val strings: NameResolver,
    public val types: TypeTable,
    @get:IgnoreInApiDump internal val versionRequirements: VersionRequirementTable,
    internal val ignoreUnknownVersionRequirements: Boolean,
    private val parent: ReadContext? = null,
    internal val contextExtensions: List<ReadContextExtension> = emptyList()
) {
    private val typeParameterNameToId = mutableMapOf<Int, Int>()

    internal val extensions = MetadataExtensions.INSTANCES

    public operator fun get(index: Int): String =
        strings.getString(index)

    internal fun className(index: Int): ClassName =
        strings.getClassName(index)

    internal fun getTypeParameterId(name: Int): Int? =
        typeParameterNameToId[name] ?: parent?.getTypeParameterId(name)

    internal fun withTypeParameters(typeParameters: List<ProtoBuf.TypeParameter>): ReadContext =
        ReadContext(strings, types, versionRequirements, ignoreUnknownVersionRequirements, this, contextExtensions).apply {
            for (typeParameter in typeParameters) {
                typeParameterNameToId[typeParameter.name] = typeParameter.id
            }
        }
}

@OptIn(ExperimentalContextReceivers::class)
public fun ProtoBuf.Class.toKmClass(
    strings: NameResolver,
    ignoreUnknownVersionRequirements: Boolean = false,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): KmClass {
    val v = KmClass()
    val c = ReadContext(
        strings,
        TypeTable(typeTable),
        VersionRequirementTable.create(versionRequirementTable),
        ignoreUnknownVersionRequirements,
        contextExtensions = contextExtensions
    ).withTypeParameters(typeParameterList)

    v.flags = flags
    v.name = c.className(fqName)

    typeParameterList.mapTo(v.typeParameters) { it.toKmTypeParameter(c) }
    supertypes(c.types).mapTo(v.supertypes) { it.toKmType(c) }
    constructorList.mapTo(v.constructors) { it.toKmConstructor(c) }
    v.visitDeclarations(functionList, propertyList, typeAliasList, c)
    if (hasCompanionObjectName()) {
        v.companionObject = c[companionObjectName]
    }

    nestedClassNameList.mapTo(v.nestedClasses) { c[it] }
    for (enumEntry in enumEntryList) {
        if (!enumEntry.hasName()) throw InconsistentKotlinMetadataException("No name for EnumEntry")
        v.enumEntries.add(c[enumEntry.name])
    }
    sealedSubclassFqNameList.mapTo(v.sealedSubclasses) { c.className(it) }
    if (hasInlineClassUnderlyingPropertyName()) {
        v.inlineClassUnderlyingPropertyName = c[inlineClassUnderlyingPropertyName]
    }
    v.inlineClassUnderlyingType = loadInlineClassUnderlyingType(c)?.toKmType(c)

    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toKmType(c) }
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readClassExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.Class.loadInlineClassUnderlyingType(c: ReadContext): ProtoBuf.Type? {
    val type = inlineClassUnderlyingType(c.types)
    if (type != null) return type

    if (!hasInlineClassUnderlyingPropertyName()) return null

    // Kotlin compiler doesn't write underlying type to metadata in case it can be loaded from the underlying property.
    return propertyList
        .singleOrNull { it.receiverType(c.types) == null && c[it.name] == c[inlineClassUnderlyingPropertyName] }
        ?.returnType(c.types)
}

public fun ProtoBuf.Package.toKmPackage(
    strings: NameResolver,
    ignoreUnknownVersionRequirements: Boolean = false,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): KmPackage {
    val v = KmPackage()
    val c = ReadContext(
        strings,
        TypeTable(typeTable),
        VersionRequirementTable.create(versionRequirementTable),
        ignoreUnknownVersionRequirements,
        contextExtensions = contextExtensions
    )

    v.visitDeclarations(functionList, propertyList, typeAliasList, c)

    c.extensions.forEach { it.readPackageExtensions(v, this, c) }

    return v
}

public fun ProtoBuf.PackageFragment.toKmModuleFragment(
    strings: NameResolver,
    contextExtensions: List<ReadContextExtension> = emptyList(),
): KmModuleFragment {
    val v = KmModuleFragment()
    val c = ReadContext(
        strings,
        TypeTable(ProtoBuf.TypeTable.newBuilder().build()),
        VersionRequirementTable.EMPTY,
        false, // toKmModuleFragment is used for klib only
        contextExtensions = contextExtensions
    )

    v.pkg = `package`.toKmPackage(strings, false, contextExtensions)
    class_List.mapTo(v.classes) { it.toKmClass(strings, false, contextExtensions) }

    c.extensions.forEach { it.readModuleFragmentExtensions(v, this, c) }

    return v
}

private fun KmDeclarationContainer.visitDeclarations(
    protoFunctions: List<ProtoBuf.Function>,
    protoProperties: List<ProtoBuf.Property>,
    protoTypeAliases: List<ProtoBuf.TypeAlias>,
    c: ReadContext,
) {
    protoFunctions.mapTo(functions) { it.toKmFunction(c) }
    protoProperties.mapTo(properties) { it.toKmProperty(c) }
    protoTypeAliases.mapTo(typeAliases) { it.toKmTypeAlias(c) }
}

public fun ProtoBuf.Function.toKmLambda(strings: NameResolver, ignoreUnknownVersionRequirements: Boolean = false): KmLambda {
    val v = KmLambda()
    val c = ReadContext(strings, TypeTable(typeTable), VersionRequirementTable.EMPTY, ignoreUnknownVersionRequirements)
    v.function = this.toKmFunction(c)
    return v
}

private fun ProtoBuf.Constructor.toKmConstructor(c: ReadContext): KmConstructor {
    val v = KmConstructor(flags)
    valueParameterList.mapTo(v.valueParameters) { it.toKmValueParameter(c) }
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readConstructorExtensions(v, this, c) }

    return v
}

@OptIn(ExperimentalContextReceivers::class)
private fun ProtoBuf.Function.toKmFunction(outer: ReadContext): KmFunction {
    val v = KmFunction(flags, outer[name])
    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toKmTypeParameter(c) }
    v.receiverParameterType = receiverType(c.types)?.toKmType(c)
    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toKmType(c) }
    valueParameterList.mapTo(v.valueParameters) { it.toKmValueParameter(c) }
    v.returnType = returnType(c.types).toKmType(c)

    @OptIn(ExperimentalContracts::class)
    if (hasContract()) {
        v.contract = contract.toKmContract(c)
    }

    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readFunctionExtensions(v, this, c) }

    return v
}

@OptIn(ExperimentalContextReceivers::class)
public fun ProtoBuf.Property.toKmProperty(outer: ReadContext): KmProperty {
    val v = KmProperty(flags, outer[name], getPropertyGetterFlags(), getPropertySetterFlags())
    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toKmTypeParameter(c) }
    v.receiverParameterType = receiverType(c.types)?.toKmType(c)
    contextReceiverTypes(c.types).mapTo(v.contextReceiverTypes) { it.toKmType(c) }
    if (hasSetterValueParameter()) {
        v.setterParameter = setterValueParameter.toKmValueParameter(c)
    }
    v.returnType = returnType(c.types).toKmType(c)
    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readPropertyExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.TypeAlias.toKmTypeAlias(outer: ReadContext): KmTypeAlias {
    val v = KmTypeAlias(flags, outer[name])

    val c = outer.withTypeParameters(typeParameterList)

    typeParameterList.mapTo(v.typeParameters) { it.toKmTypeParameter(c) }
    v.underlyingType = underlyingType(c.types).toKmType(c)
    v.expandedType = expandedType(c.types).toKmType(c)
    annotationList.mapTo(v.annotations) { it.readAnnotation(c.strings) }

    versionRequirementList.mapTo(v.versionRequirements) { readVersionRequirement(it, c) }

    c.extensions.forEach { it.readTypeAliasExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.ValueParameter.toKmValueParameter(c: ReadContext): KmValueParameter {
    val v = KmValueParameter(flags, c[name])
    v.type = type(c.types).toKmType(c)

    v.varargElementType = varargElementType(c.types)?.toKmType(c)

    c.extensions.forEach { it.readValueParameterExtensions(v, this, c) }

    return v
}

private fun ProtoBuf.TypeParameter.toKmTypeParameter(
    c: ReadContext,
): KmTypeParameter {
    val variance = when (requireNotNull(variance)) {
        ProtoBuf.TypeParameter.Variance.IN -> KmVariance.IN
        ProtoBuf.TypeParameter.Variance.OUT -> KmVariance.OUT
        ProtoBuf.TypeParameter.Variance.INV -> KmVariance.INVARIANT
    }
    val ktp = KmTypeParameter(typeParameterFlags, c[name], id, variance)

    upperBounds(c.types).mapTo(ktp.upperBounds) { it.toKmType(c) }

    c.extensions.forEach { it.readTypeParameterExtensions(ktp, this, c) }

    return ktp
}

private fun ProtoBuf.Type.toKmType(c: ReadContext): KmType {
    val v = KmType(typeFlags)
    v.classifier = when {
        hasClassName() -> KmClassifier.Class(c.className(className))
        hasTypeAliasName() -> KmClassifier.TypeAlias(c.className(typeAliasName))
        hasTypeParameter() -> KmClassifier.TypeParameter(typeParameter)
        hasTypeParameterName() -> {
            val id = c.getTypeParameterId(typeParameterName)
                ?: throw InconsistentKotlinMetadataException("No type parameter id for ${c[typeParameterName]}")
            KmClassifier.TypeParameter(id)
        }
        else -> throw InconsistentKotlinMetadataException("No classifier (class, type alias or type parameter) recorded for Type")
    }

    for (argument in argumentList) {
        val variance = when (requireNotNull(argument.projection)) {
            ProtoBuf.Type.Argument.Projection.IN -> KmVariance.IN
            ProtoBuf.Type.Argument.Projection.OUT -> KmVariance.OUT
            ProtoBuf.Type.Argument.Projection.INV -> KmVariance.INVARIANT
            ProtoBuf.Type.Argument.Projection.STAR -> null
        }

        if (variance != null) {
            val argumentType = argument.type(c.types)
                ?: throw InconsistentKotlinMetadataException("No type argument for non-STAR projection in Type")
            v.arguments.add(KmTypeProjection(variance, argumentType.toKmType(c)))
        } else {
            v.arguments.add(KmTypeProjection.STAR)
        }
    }

    v.abbreviatedType = abbreviatedType(c.types)?.toKmType(c)
    v.outerType = outerType(c.types)?.toKmType(c)

    v.flexibleTypeUpperBound = flexibleUpperBound(c.types)?.toKmType(c)?.let {
        KmFlexibleTypeUpperBound(it, if (hasFlexibleTypeCapabilitiesId()) c[flexibleTypeCapabilitiesId] else null)
    }

    c.extensions.forEach { it.readTypeExtensions(v, this, c) }

    return v
}

private fun readVersionRequirement(id: Int, c: ReadContext): KmVersionRequirement {
    val v = KmVersionRequirement()
    val message = VersionRequirement.create(id, c.strings, c.versionRequirements)
    if (message == null && !c.ignoreUnknownVersionRequirements) throw InconsistentKotlinMetadataException("No VersionRequirement with the given id in the table")

    val kind = when (message?.kind) {
        ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION -> KmVersionRequirementVersionKind.LANGUAGE_VERSION
        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION -> KmVersionRequirementVersionKind.COMPILER_VERSION
        ProtoBuf.VersionRequirement.VersionKind.API_VERSION -> KmVersionRequirementVersionKind.API_VERSION
        null -> KmVersionRequirementVersionKind.UNKNOWN
    }

    val level = when (message?.level) {
        DeprecationLevel.WARNING -> KmVersionRequirementLevel.WARNING
        DeprecationLevel.ERROR -> KmVersionRequirementLevel.ERROR
        DeprecationLevel.HIDDEN, null -> KmVersionRequirementLevel.HIDDEN
    }

    v.kind = kind
    v.level = level
    v.errorCode = message?.errorCode
    v.message = message?.message

    val (major, minor, patch) = message?.version ?: VersionRequirement.Version.INFINITY
    v.version = KmVersion(major, minor, patch)
    return v
}

@ExperimentalContracts
private fun ProtoBuf.Contract.toKmContract(c: ReadContext): KmContract {
    val v = KmContract()
    for (effect in effectList) {
        if (!effect.hasEffectType()) continue

        val effectType = when (requireNotNull(effect.effectType)) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> KmEffectType.RETURNS_CONSTANT
            ProtoBuf.Effect.EffectType.CALLS -> KmEffectType.CALLS
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> KmEffectType.RETURNS_NOT_NULL
        }

        val effectKind = if (!effect.hasKind()) null else when (requireNotNull(effect.kind)) {
            ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> KmEffectInvocationKind.AT_MOST_ONCE
            ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> KmEffectInvocationKind.EXACTLY_ONCE
            ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> KmEffectInvocationKind.AT_LEAST_ONCE
        }

        v.effects.add(effect.toKmEffect(effectType, effectKind, c))
    }

    return v
}

@ExperimentalContracts
private fun ProtoBuf.Effect.toKmEffect(type: KmEffectType, kind: KmEffectInvocationKind?, c: ReadContext): KmEffect {
    val v = KmEffect(type, kind)
    effectConstructorArgumentList.mapTo(v.constructorArguments) { it.toKmEffectExpression(c) }

    if (hasConclusionOfConditionalEffect()) {
        v.conclusion = conclusionOfConditionalEffect.toKmEffectExpression(c)
    }

    return v
}

@ExperimentalContracts
private fun ProtoBuf.Expression.toKmEffectExpression(c: ReadContext): KmEffectExpression {
    val v = KmEffectExpression()
    v.flags = flags
    v.parameterIndex = if (hasValueParameterReference()) valueParameterReference else null

    if (hasConstantValue()) {
        v.constantValue = KmConstantValue(
            when (requireNotNull(constantValue)) {
                ProtoBuf.Expression.ConstantValue.TRUE -> true
                ProtoBuf.Expression.ConstantValue.FALSE -> false
                ProtoBuf.Expression.ConstantValue.NULL -> null
            }
        )
    }

    v.isInstanceType = isInstanceType(c.types)?.toKmType(c)

    andArgumentList.mapTo(v.andArguments) { it.toKmEffectExpression(c) }
    orArgumentList.mapTo(v.orArguments) { it.toKmEffectExpression(c) }

    return v
}

private val ProtoBuf.Type.typeFlags: Int
    get() = (if (nullable) 1 shl 0 else 0) +
            (flags shl 1)

private val ProtoBuf.TypeParameter.typeParameterFlags: Int
    get() = if (reified) 1 else 0

public fun ProtoBuf.Property.getPropertyGetterFlags(): Int =
    if (hasGetterFlags()) getterFlags else getDefaultPropertyAccessorFlags(flags)

public fun ProtoBuf.Property.getPropertySetterFlags(): Int =
    if (hasSetterFlags()) setterFlags else getDefaultPropertyAccessorFlags(flags)

internal fun getDefaultPropertyAccessorFlags(flags: Int): Int =
    F.getAccessorFlags(F.HAS_ANNOTATIONS.get(flags), F.VISIBILITY.get(flags), F.MODALITY.get(flags), false, false, false)
