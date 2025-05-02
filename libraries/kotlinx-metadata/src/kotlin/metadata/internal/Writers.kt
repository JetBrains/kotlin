/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalAnnotationsInMetadata::class)

package kotlin.metadata.internal

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.metadata.serialization.StringTable
import kotlin.contracts.ExperimentalContracts
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.MetadataExtensions

/**
 * Allows to populate [WriteContext] with additional data
 * that can be used when writing metadata in [MetadataExtensions].
 */
public interface WriteContextExtension

public open class WriteContext(public val strings: StringTable, public val contextExtensions: List<WriteContextExtension> = emptyList()) {
    internal val versionRequirements: MutableVersionRequirementTable = MutableVersionRequirementTable()

    internal val extensions = MetadataExtensions.INSTANCES

    public operator fun get(string: String): Int =
        strings.getStringIndex(string)

    internal fun getClassName(name: ClassName): Int =
        strings.getClassNameIndex(name)
}

private fun WriteContext.writeTypeParameter(kmTypeParameter: KmTypeParameter): ProtoBuf.TypeParameter.Builder {
    val t = ProtoBuf.TypeParameter.newBuilder()
    kmTypeParameter.upperBounds.forEach { ub ->
        t.addUpperBound(writeType(ub).build())
    }
    extensions.forEach {
        it.writeTypeParameterExtensions(kmTypeParameter, t, this)
    }
    t.name = this[kmTypeParameter.name]
    t.id = kmTypeParameter.id
    val reified = kmTypeParameter.isReified
    if (reified != ProtoBuf.TypeParameter.getDefaultInstance().reified) {
        t.reified = reified
    }
    if (kmTypeParameter.variance == KmVariance.IN) {
        t.variance = ProtoBuf.TypeParameter.Variance.IN
    } else if (kmTypeParameter.variance == KmVariance.OUT) {
        t.variance = ProtoBuf.TypeParameter.Variance.OUT
    }
    return t
}

private fun WriteContext.writeTypeProjection(argument: KmTypeProjection): ProtoBuf.Type.Argument.Builder {
    val t = ProtoBuf.Type.Argument.newBuilder()
    if (argument == KmTypeProjection.STAR) {
        t.projection = ProtoBuf.Type.Argument.Projection.STAR
    } else {
        val (variance, argType) = argument
        if (variance == null || argType == null)
            throw InconsistentKotlinMetadataException("Variance and type must be set for non-star type projection")
        if (variance == KmVariance.IN) {
            t.projection = ProtoBuf.Type.Argument.Projection.IN
        } else if (variance == KmVariance.OUT) {
            t.projection = ProtoBuf.Type.Argument.Projection.OUT
        }
        t.type = writeType(argType).build()
    }

    return t
}

private fun WriteContext.writeType(kmType: KmType): ProtoBuf.Type.Builder {
    val t = ProtoBuf.Type.newBuilder()
    when (val cls = kmType.classifier) {
        is KmClassifier.Class -> t.className = getClassName(cls.name)
        is KmClassifier.TypeAlias -> t.typeAliasName = getClassName(cls.name)
        is KmClassifier.TypeParameter -> t.typeParameter = cls.id
    }
    kmType.arguments.forEach { argument ->
        t.addArgument(writeTypeProjection(argument))
    }

    kmType.abbreviatedType?.let { t.abbreviatedType = writeType(it).build() }
    kmType.outerType?.let { t.outerType = writeType(it).build() }
    kmType.flexibleTypeUpperBound?.let { fub ->
        val fubType = writeType(fub.type)
        fub.typeFlexibilityId?.let { t.flexibleTypeCapabilitiesId = this[it] }
        t.flexibleUpperBound = fubType.build()
    }

    extensions.forEach { it.writeTypeExtensions(kmType, t, this) }

    if (kmType.isNullable) {
        t.nullable = true
    }
    val flagsToWrite = kmType.flags shr 1
    if (flagsToWrite != ProtoBuf.Type.getDefaultInstance().flags) {
        t.flags = flagsToWrite
    }
    return t
}

private fun WriteContext.writeConstructor(kmConstructor: KmConstructor): ProtoBuf.Constructor.Builder {
    val t = ProtoBuf.Constructor.newBuilder()

    kmConstructor.valueParameters.forEach {
        t.addValueParameter(writeValueParameter(it).build())
    }
    t.addAllVersionRequirement(kmConstructor.versionRequirements.mapNotNull(::writeVersionRequirement))
    extensions.forEach {
        it.writeConstructorExtensions(kmConstructor, t, this)
    }
    val flags = kmConstructor.flags or
            Flags.HAS_ANNOTATIONS.toFlags(kmConstructor.annotations.isNotEmpty())
    if (flags != ProtoBuf.Constructor.getDefaultInstance().flags) {
        t.flags = flags
    }
    return t
}

@OptIn(ExperimentalContextParameters::class)
private fun WriteContext.writeFunction(kmFunction: KmFunction): ProtoBuf.Function.Builder {
    val t = ProtoBuf.Function.newBuilder()
    t.addAllTypeParameter(kmFunction.typeParameters.map { writeTypeParameter(it).build() })
    kmFunction.receiverParameterType?.let { t.receiverType = writeType(it).build() }

    t.addAllContextParameter(kmFunction.contextParameters.map { writeValueParameter(it).build() })
    t.addAllContextReceiverType(kmFunction.contextParameters.map { writeType(it.type).build() })
    t.addAllValueParameter(kmFunction.valueParameters.map { writeValueParameter(it).build() })
    t.returnType = writeType(kmFunction.returnType).build()
    t.addAllVersionRequirement(kmFunction.versionRequirements.mapNotNull(::writeVersionRequirement))

    @OptIn(ExperimentalContracts::class)
    kmFunction.contract?.let { t.contract = writeContract(it) }

    extensions.forEach { it.writeFunctionExtensions(kmFunction, t, this) }

    t.name = this[kmFunction.name]
    val flags = kmFunction.flags or
            Flags.HAS_ANNOTATIONS.toFlags(kmFunction.annotations.isNotEmpty())
    if (flags != ProtoBuf.Function.getDefaultInstance().flags) {
        t.flags = flags
    }
    return t
}

@OptIn(ExperimentalContextParameters::class)
public fun WriteContext.writeProperty(kmProperty: KmProperty): ProtoBuf.Property.Builder {
    val t = ProtoBuf.Property.newBuilder()

    kmProperty.typeParameters.forEach { tp ->
        t.addTypeParameter(writeTypeParameter(tp).build())
    }
    kmProperty.receiverParameterType?.let { t.receiverType = writeType(it).build() }

    t.addAllContextParameter(kmProperty.contextParameters.map { writeValueParameter(it).build() })
    t.addAllContextReceiverType(kmProperty.contextParameters.map { writeType(it.type).build() })

    kmProperty.setterParameter?.let { t.setterValueParameter = writeValueParameter(it).build() }
    t.returnType = writeType(kmProperty.returnType).build()
    t.addAllVersionRequirement(kmProperty.versionRequirements.mapNotNull { writeVersionRequirement(it) })

    extensions.forEach { it.writePropertyExtensions(kmProperty, t, this) }

    t.name = this[kmProperty.name]
    val flags = kmProperty.flags or
            Flags.HAS_ANNOTATIONS.toFlags(kmProperty.annotations.isNotEmpty())
    if (flags != ProtoBuf.Property.getDefaultInstance().flags) {
        t.flags = flags
    }

    // TODO: do not write getterFlags/setterFlags if not needed
    t.getterFlags = kmProperty.getter.flags or
            Flags.HAS_ANNOTATIONS.toFlags(kmProperty.getter.annotations.isNotEmpty())

    kmProperty.setter?.let { setter ->
        t.setterFlags = setter.flags or
                Flags.HAS_ANNOTATIONS.toFlags(setter.annotations.isNotEmpty())
    }
    return t
}

private fun WriteContext.writeValueParameter(
    kmValueParameter: KmValueParameter,
): ProtoBuf.ValueParameter.Builder {
    val t = ProtoBuf.ValueParameter.newBuilder()
    t.type = writeType(kmValueParameter.type).build()
    kmValueParameter.varargElementType?.let { t.varargElementType = writeType(it).build() }
    kmValueParameter.annotationParameterDefaultValue?.let {
        t.annotationParameterDefaultValue = it.writeAnnotationArgument(strings).build()
    }
    extensions.forEach { it.writeValueParameterExtensions(kmValueParameter, t, this) }
    val flags = kmValueParameter.flags or
            Flags.HAS_ANNOTATIONS.toFlags(kmValueParameter.annotations.isNotEmpty())
    if (flags != ProtoBuf.ValueParameter.getDefaultInstance().flags) {
        t.flags = flags
    }
    t.name = this[kmValueParameter.name]
    return t
}

private fun WriteContext.writeTypeAlias(
    typeAlias: KmTypeAlias,
): ProtoBuf.TypeAlias.Builder {
    val t = ProtoBuf.TypeAlias.newBuilder()
    t.addAllTypeParameter(typeAlias.typeParameters.map { writeTypeParameter(it).build() })
    t.underlyingType = writeType(typeAlias.underlyingType).build()
    t.expandedType = writeType(typeAlias.expandedType).build()
    t.addAllAnnotation(typeAlias.annotations.map { it.writeAnnotation(strings).build() })
    t.addAllVersionRequirement(typeAlias.versionRequirements.mapNotNull(::writeVersionRequirement))
    extensions.forEach { it.writeTypeAliasExtensions(typeAlias, t, this) }

    val flags = typeAlias.flags or
            Flags.HAS_ANNOTATIONS.toFlags(typeAlias.annotations.isNotEmpty())
    if (flags != ProtoBuf.TypeAlias.getDefaultInstance().flags) {
        t.flags = flags
    }
    t.name = this[typeAlias.name]
    return t
}

private fun WriteContext.writeVersionRequirement(kmVersionRequirement: KmVersionRequirement): Int? {
    val kind = kmVersionRequirement.kind
    val level = kmVersionRequirement.level
    val errorCode = kmVersionRequirement.errorCode
    val message = kmVersionRequirement.message
    val t = ProtoBuf.VersionRequirement.newBuilder().apply {
        val versionKind = when (kind) {
            KmVersionRequirementVersionKind.LANGUAGE_VERSION -> ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
            KmVersionRequirementVersionKind.COMPILER_VERSION -> ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            KmVersionRequirementVersionKind.API_VERSION -> ProtoBuf.VersionRequirement.VersionKind.API_VERSION
            KmVersionRequirementVersionKind.UNKNOWN -> return null
        }
        if (versionKind != defaultInstanceForType.versionKind) {
            this.versionKind = versionKind
        }
        val requirementLevel = when (level) {
            KmVersionRequirementLevel.WARNING -> ProtoBuf.VersionRequirement.Level.WARNING
            KmVersionRequirementLevel.ERROR -> ProtoBuf.VersionRequirement.Level.ERROR
            KmVersionRequirementLevel.HIDDEN -> ProtoBuf.VersionRequirement.Level.HIDDEN
        }
        if (requirementLevel != defaultInstanceForType.level) {
            this.level = requirementLevel
        }
        if (errorCode != null) {
            this.errorCode = errorCode
        }
        if (message != null) {
            this.message = this@writeVersionRequirement[message]
        }
    }
    val (major, minor, patch) = kmVersionRequirement.version

    VersionRequirement.Version(major, minor, patch).encode(
        writeVersion = { t!!.version = it },
        writeVersionFull = { t!!.versionFull = it }
    )

    return this.versionRequirements[t]
}

private fun WriteContext.writeEnumEntry(kmEnumEntry: KmEnumEntry): ProtoBuf.EnumEntry.Builder {
    val t = ProtoBuf.EnumEntry.newBuilder()

    t.name = this[kmEnumEntry.name]

    extensions.forEach { it.writeEnumEntryExtensions(kmEnumEntry, t, this) }

    return t
}

@ExperimentalContracts
private fun WriteContext.writeContract(contract: KmContract): ProtoBuf.Contract {
    val t = ProtoBuf.Contract.newBuilder()

    t.addAllEffect(contract.effects.map(::writeEffect))
    return t.build()
}

@ExperimentalContracts
private fun WriteContext.writeEffect(
    effect: KmEffect,
): ProtoBuf.Effect {
    val t = ProtoBuf.Effect.newBuilder()

    t.addAllEffectConstructorArgument(effect.constructorArguments.map(::writeEffectExpression))
    effect.conclusion?.let { t.conclusionOfConditionalEffect = writeEffectExpression(it) }

    when (effect.type) {
        KmEffectType.RETURNS_CONSTANT -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
        KmEffectType.CALLS -> t.effectType = ProtoBuf.Effect.EffectType.CALLS
        KmEffectType.RETURNS_NOT_NULL -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
    }
    when (effect.invocationKind) {
        KmEffectInvocationKind.AT_MOST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
        KmEffectInvocationKind.EXACTLY_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
        KmEffectInvocationKind.AT_LEAST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
        null -> {} // nop
    }
    return t.build()
}

@ExperimentalContracts
private fun WriteContext.writeEffectExpression(effectExpression: KmEffectExpression): ProtoBuf.Expression {
    val t = ProtoBuf.Expression.newBuilder()

    if (effectExpression.flags != ProtoBuf.Expression.getDefaultInstance().flags) {
        t.flags = effectExpression.flags
    }
    effectExpression.parameterIndex?.let { t.valueParameterReference = it }
    val cv = effectExpression.constantValue
    if (cv != null) {
        when (val value = cv.value) {
            true -> t.constantValue = ProtoBuf.Expression.ConstantValue.TRUE
            false -> t.constantValue = ProtoBuf.Expression.ConstantValue.FALSE
            null -> t.constantValue = ProtoBuf.Expression.ConstantValue.NULL
            else -> throw IllegalArgumentException("Only true, false or null constant values are allowed for effects (was=$value)")
        }
    }
    effectExpression.isInstanceType?.let { t.isInstanceType = writeType(it).build() }
    t.addAllAndArgument(effectExpression.andArguments.map(::writeEffectExpression))
    t.addAllOrArgument(effectExpression.orArguments.map(::writeEffectExpression))
    return t.build()
}

public open class ClassWriter(stringTable: StringTable, contextExtensions: List<WriteContextExtension> = emptyList()) {
    public val t: ProtoBuf.Class.Builder = ProtoBuf.Class.newBuilder()!!
    public val c: WriteContext = WriteContext(stringTable, contextExtensions)

    public fun writeClass(kmClass: KmClass) {
        val flags = kmClass.flags or
                Flags.HAS_ANNOTATIONS.toFlags(kmClass.annotations.isNotEmpty())
        if (flags != ProtoBuf.Class.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.fqName = c.getClassName(kmClass.name)

        t.addAllTypeParameter(kmClass.typeParameters.map { c.writeTypeParameter(it).build() })
        t.addAllSupertype(kmClass.supertypes.map { c.writeType(it).build() })
        t.addAllConstructor(kmClass.constructors.map { c.writeConstructor(it).build() })
        t.addAllFunction(kmClass.functions.map { c.writeFunction(it).build() })
        t.addAllProperty(kmClass.properties.map { c.writeProperty(it).build() })
        t.addAllTypeAlias(kmClass.typeAliases.map { c.writeTypeAlias(it).build() })

        kmClass.companionObject?.let { t.companionObjectName = c[it] }
        kmClass.nestedClasses.forEach { t.addNestedClassName(c[it]) }

        if (kmClass.kmEnumEntries.isNotEmpty()) {
            kmClass.kmEnumEntries.forEach { entry ->
                t.addEnumEntry(c.writeEnumEntry(entry).build())
            }
        } else {
            @Suppress("DEPRECATION")
            kmClass.enumEntries.forEach { name ->
                t.addEnumEntry(ProtoBuf.EnumEntry.newBuilder().also { enumEntry ->
                    enumEntry.name = c[name]
                })
            }
        }

        t.addAllSealedSubclassFqName(kmClass.sealedSubclasses.map { c.getClassName(it) })

        kmClass.inlineClassUnderlyingPropertyName?.let { t.inlineClassUnderlyingPropertyName = c[it] }
        kmClass.inlineClassUnderlyingType?.let { t.inlineClassUnderlyingType = c.writeType(it).build() }

        @[Suppress("DEPRECATION") OptIn(ExperimentalContextReceivers::class)]
        t.addAllContextReceiverType(kmClass.contextReceiverTypes.map { c.writeType(it).build() })

        t.addAllVersionRequirement(kmClass.versionRequirements.mapNotNull { c.writeVersionRequirement(it) })

        c.extensions.forEach { it.writeClassExtensions(kmClass, t, c) }

        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

public open class PackageWriter(stringTable: StringTable, contextExtensions: List<WriteContextExtension> = emptyList()) {
    public val t: ProtoBuf.Package.Builder = ProtoBuf.Package.newBuilder()
    public val c: WriteContext = WriteContext(stringTable, contextExtensions)

    public fun writePackage(kmPackage: KmPackage) {
        t.addAllFunction(kmPackage.functions.map { c.writeFunction(it).build() })
        t.addAllProperty(kmPackage.properties.map { c.writeProperty(it).build() })
        t.addAllTypeAlias(kmPackage.typeAliases.map { c.writeTypeAlias(it).build() })
        c.extensions.forEach { it.writePackageExtensions(kmPackage, t, c) }
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

public open class ModuleFragmentWriter(stringTable: StringTable, contextExtensions: List<WriteContextExtension> = emptyList()) {
    protected val t: ProtoBuf.PackageFragment.Builder = ProtoBuf.PackageFragment.newBuilder()!!
    protected val c: WriteContext = WriteContext(stringTable, contextExtensions)

    public open fun writeModuleFragment(kmPackageFragment: KmModuleFragment) {
        kmPackageFragment.pkg?.let {
            val pkgWriter = PackageWriter(c.strings, c.contextExtensions)
            pkgWriter.writePackage(it)
            t.setPackage(pkgWriter.t)
        }

        kmPackageFragment.classes.forEach {
            val classWriter = ClassWriter(c.strings, c.contextExtensions)
            classWriter.writeClass(it)
            t.addClass_(classWriter.t)
        }

        c.extensions.forEach { it.writeModuleFragmentExtensions(kmPackageFragment, t, c) }
    }
}

public open class LambdaWriter(stringTable: StringTable) {
    public var t: ProtoBuf.Function.Builder? = null
    public val c: WriteContext = WriteContext(stringTable)

    public fun writeLambda(kmLambda: KmLambda) {
        t = c.writeFunction(kmLambda.function)
    }
}
