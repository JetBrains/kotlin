/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl

import kotlinx.metadata.*
import kotlinx.metadata.impl.extensions.MetadataExtensions
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.metadata.serialization.StringTable

class WriteContext(val strings: StringTable) {
    internal val extensions = MetadataExtensions.INSTANCES
    val versionRequirements: MutableVersionRequirementTable = MutableVersionRequirementTable()

    operator fun get(string: String): Int =
        strings.getStringIndex(string)

    fun getClassName(name: ClassName): Int =
        strings.getClassNameIndex(name)

    internal fun <T : KmExtensionVisitor> applySingleExtension(type: KmExtensionType, block: MetadataExtensions.() -> T?): T? {
        var result: T? = null
        for (extension in extensions) {
            val current = block(extension) ?: continue
            if (result != null) {
                throw IllegalStateException("Multiple extensions handle the same extension type: $type")
            }
            result = current
        }
        return result
    }
}

private fun writeTypeParameter(
    c: WriteContext, flags: Flags, name: String, id: Int, variance: KmVariance,
    output: (ProtoBuf.TypeParameter.Builder) -> Unit
): KmTypeParameterVisitor =
    object : KmTypeParameterVisitor() {
        private val t = ProtoBuf.TypeParameter.newBuilder()

        override fun visitUpperBound(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.addUpperBound(it) }

        override fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? =
            c.applySingleExtension(type) {
                writeTypeParameterExtensions(type, t, c.strings)
            }

        override fun visitEnd() {
            t.name = c[name]
            t.id = id
            val reified = Flag.TypeParameter.IS_REIFIED(flags)
            if (reified != ProtoBuf.TypeParameter.getDefaultInstance().reified) {
                t.reified = reified
            }
            if (variance == KmVariance.IN) {
                t.variance = ProtoBuf.TypeParameter.Variance.IN
            } else if (variance == KmVariance.OUT) {
                t.variance = ProtoBuf.TypeParameter.Variance.OUT
            }
            output(t)
        }
    }

private fun writeType(c: WriteContext, flags: Flags, output: (ProtoBuf.Type.Builder) -> Unit): KmTypeVisitor =
    object : KmTypeVisitor() {
        private val t = ProtoBuf.Type.newBuilder()

        override fun visitClass(name: ClassName) {
            t.className = c.getClassName(name)
        }

        override fun visitTypeAlias(name: ClassName) {
            t.typeAliasName = c.getClassName(name)
        }

        override fun visitStarProjection() {
            t.addArgument(ProtoBuf.Type.Argument.newBuilder().apply {
                projection = ProtoBuf.Type.Argument.Projection.STAR
            })
        }

        override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor? =
            writeType(c, flags) { argument ->
                t.addArgument(ProtoBuf.Type.Argument.newBuilder().apply {
                    if (variance == KmVariance.IN) {
                        projection = ProtoBuf.Type.Argument.Projection.IN
                    } else if (variance == KmVariance.OUT) {
                        projection = ProtoBuf.Type.Argument.Projection.OUT
                    }
                    type = argument.build()
                })
            }

        override fun visitTypeParameter(id: Int) {
            t.typeParameter = id
        }

        override fun visitAbbreviatedType(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.abbreviatedType = it.build() }

        override fun visitOuterType(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.outerType = it.build() }

        override fun visitFlexibleTypeUpperBound(flags: Flags, typeFlexibilityId: String?): KmTypeVisitor? =
            writeType(c, flags) {
                if (typeFlexibilityId != null) {
                    t.flexibleTypeCapabilitiesId = c[typeFlexibilityId]
                }
                t.flexibleUpperBound = it.build()
            }

        override fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? =
            c.applySingleExtension(type) {
                writeTypeExtensions(type, t, c.strings)
            }

        override fun visitEnd() {
            if (Flag.Type.IS_NULLABLE(flags)) {
                t.nullable = true
            }
            val flagsToWrite = flags shr 1
            if (flagsToWrite != ProtoBuf.Type.getDefaultInstance().flags) {
                t.flags = flagsToWrite
            }
            output(t)
        }
    }

private fun writeConstructor(c: WriteContext, flags: Flags, output: (ProtoBuf.Constructor.Builder) -> Unit): KmConstructorVisitor =
    object : KmConstructorVisitor() {
        val t = ProtoBuf.Constructor.newBuilder()

        override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            writeValueParameter(c, flags, name) { t.addValueParameter(it.build()) }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            writeVersionRequirement(c) { t.versionRequirement = it }

        override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? =
            c.applySingleExtension(type) {
                writeConstructorExtensions(type, t, c.strings)
            }

        override fun visitEnd() {
            if (flags != ProtoBuf.Constructor.getDefaultInstance().flags) {
                t.flags = flags
            }
            output(t)
        }
    }

private fun writeFunction(c: WriteContext, flags: Flags, name: String, output: (ProtoBuf.Function.Builder) -> Unit): KmFunctionVisitor =
    object : KmFunctionVisitor() {
        val t = ProtoBuf.Function.newBuilder()

        override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
            writeTypeParameter(c, flags, name, id, variance) { t.addTypeParameter(it) }

        override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.receiverType = it.build() }

        override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
            writeValueParameter(c, flags, name) { t.addValueParameter(it) }

        override fun visitReturnType(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.returnType = it.build() }

        override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
            writeVersionRequirement(c) { t.versionRequirement = it }

        override fun visitContract(): KmContractVisitor? =
            writeContract(c) { t.contract = it.build() }

        override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? =
            c.applySingleExtension(type) {
                writeFunctionExtensions(type, t, c.strings)
            }

        override fun visitEnd() {
            t.name = c[name]
            if (flags != ProtoBuf.Function.getDefaultInstance().flags) {
                t.flags = flags
            }
            output(t)
        }
    }

private fun writeProperty(
    c: WriteContext, flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags, output: (ProtoBuf.Property.Builder) -> Unit
): KmPropertyVisitor = object : KmPropertyVisitor() {
    val t = ProtoBuf.Property.newBuilder()

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance) { t.addTypeParameter(it) }

    override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.receiverType = it.build() }

    override fun visitSetterParameter(flags: Flags, name: String): KmValueParameterVisitor? =
        writeValueParameter(c, flags, name) { t.setterValueParameter = it.build() }

    override fun visitReturnType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.returnType = it.build() }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? =
        c.applySingleExtension(type) {
            writePropertyExtensions(type, t, c.strings)
        }

    override fun visitEnd() {
        t.name = c[name]
        if (flags != ProtoBuf.Property.getDefaultInstance().flags) {
            t.flags = flags
        }
        // TODO: do not write getterFlags/setterFlags if not needed
        t.getterFlags = getterFlags
        t.setterFlags = setterFlags
        output(t)
    }
}

private fun writeValueParameter(
    c: WriteContext, flags: Flags, name: String,
    output: (ProtoBuf.ValueParameter.Builder) -> Unit
): KmValueParameterVisitor = object : KmValueParameterVisitor() {
    val t = ProtoBuf.ValueParameter.newBuilder()

    override fun visitType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.type = it.build() }

    override fun visitVarargElementType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.varargElementType = it.build() }

    override fun visitEnd() {
        if (flags != ProtoBuf.ValueParameter.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.name = c[name]
        output(t)
    }
}

private fun writeTypeAlias(
    c: WriteContext, flags: Flags, name: String,
    output: (ProtoBuf.TypeAlias.Builder) -> Unit
): KmTypeAliasVisitor = object : KmTypeAliasVisitor() {
    val t = ProtoBuf.TypeAlias.newBuilder()

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance) { t.addTypeParameter(it) }

    override fun visitUnderlyingType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.underlyingType = it.build() }

    override fun visitExpandedType(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.expandedType = it.build() }

    override fun visitAnnotation(annotation: KmAnnotation) {
        t.addAnnotation(annotation.writeAnnotation(c.strings))
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitEnd() {
        if (flags != ProtoBuf.TypeAlias.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.name = c[name]
        output(t)
    }
}

private fun writeVersionRequirement(
    c: WriteContext, output: (Int) -> Unit
): KmVersionRequirementVisitor = object : KmVersionRequirementVisitor() {
    var t: ProtoBuf.VersionRequirement.Builder? = null

    override fun visit(kind: KmVersionRequirementVersionKind, level: KmVersionRequirementLevel, errorCode: Int?, message: String?) {
        t = ProtoBuf.VersionRequirement.newBuilder().apply {
            if (kind != defaultInstanceForType.versionKind) {
                this.versionKind = when (kind) {
                    KmVersionRequirementVersionKind.LANGUAGE_VERSION -> ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
                    KmVersionRequirementVersionKind.COMPILER_VERSION -> ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
                    KmVersionRequirementVersionKind.API_VERSION -> ProtoBuf.VersionRequirement.VersionKind.API_VERSION
                }
            }
            if (level != defaultInstanceForType.level) {
                this.level = when (level) {
                    KmVersionRequirementLevel.WARNING -> ProtoBuf.VersionRequirement.Level.WARNING
                    KmVersionRequirementLevel.ERROR -> ProtoBuf.VersionRequirement.Level.ERROR
                    KmVersionRequirementLevel.HIDDEN -> ProtoBuf.VersionRequirement.Level.HIDDEN
                }
            }
            if (errorCode != null) {
                this.errorCode = errorCode
            }
            if (message != null) {
                this.message = c[message]
            }
        }
    }

    override fun visitVersion(major: Int, minor: Int, patch: Int) {
        if (t == null) {
            throw IllegalStateException("KmVersionRequirementVisitor.visit has not been called")
        }
        VersionRequirement.Version(major, minor, patch).encode(
            writeVersion = { t!!.version = it },
            writeVersionFull = { t!!.versionFull = it }
        )
    }

    override fun visitEnd() {
        if (t == null) {
            throw IllegalStateException("KmVersionRequirementVisitor.visit has not been called")
        }
        output(c.versionRequirements[t!!])
    }
}

private fun writeContract(c: WriteContext, output: (ProtoBuf.Contract.Builder) -> Unit): KmContractVisitor =
    object : KmContractVisitor() {
        val t = ProtoBuf.Contract.newBuilder()

        override fun visitEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?): KmEffectVisitor? =
            writeEffect(c, type, invocationKind) { t.addEffect(it) }

        override fun visitEnd() {
            output(t)
        }
    }

private fun writeEffect(
    c: WriteContext, type: KmEffectType, invocationKind: KmEffectInvocationKind?,
    output: (ProtoBuf.Effect.Builder) -> Unit
): KmEffectVisitor = object : KmEffectVisitor() {
    val t = ProtoBuf.Effect.newBuilder()

    override fun visitConstructorArgument(): KmEffectExpressionVisitor? =
        writeEffectExpression(c) { t.addEffectConstructorArgument(it) }

    override fun visitConclusionOfConditionalEffect(): KmEffectExpressionVisitor? =
        writeEffectExpression(c) { t.conclusionOfConditionalEffect = it.build() }

    @Suppress("UNUSED_VARIABLE") // force exhaustive whens
    override fun visitEnd() {
        val unused = when (type) {
            KmEffectType.RETURNS_CONSTANT -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_CONSTANT
            KmEffectType.CALLS -> t.effectType = ProtoBuf.Effect.EffectType.CALLS
            KmEffectType.RETURNS_NOT_NULL -> t.effectType = ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL
        }
        val unused2 = when (invocationKind) {
            KmEffectInvocationKind.AT_MOST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE
            KmEffectInvocationKind.EXACTLY_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE
            KmEffectInvocationKind.AT_LEAST_ONCE -> t.kind = ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE
            null -> null
        }
        output(t)
    }
}

private fun writeEffectExpression(c: WriteContext, output: (ProtoBuf.Expression.Builder) -> Unit): KmEffectExpressionVisitor =
    object : KmEffectExpressionVisitor() {
        val t = ProtoBuf.Expression.newBuilder()

        override fun visit(flags: Flags, parameterIndex: Int?) {
            if (flags != ProtoBuf.Expression.getDefaultInstance().flags) {
                t.flags = flags
            }
            if (parameterIndex != null) {
                t.valueParameterReference = parameterIndex
            }
        }

        override fun visitConstantValue(value: Any?) {
            @Suppress("UNUSED_VARIABLE") // force exhaustive when
            val unused = when (value) {
                true -> t.constantValue = ProtoBuf.Expression.ConstantValue.TRUE
                false -> t.constantValue = ProtoBuf.Expression.ConstantValue.FALSE
                null -> null
                else -> throw IllegalArgumentException("Only true, false or null constant values are allowed for effects (was=$value)")
            }
        }

        override fun visitIsInstanceType(flags: Flags): KmTypeVisitor? =
            writeType(c, flags) { t.isInstanceType = it.build() }

        override fun visitAndArgument(): KmEffectExpressionVisitor? =
            writeEffectExpression(c) { t.addAndArgument(it) }

        override fun visitOrArgument(): KmEffectExpressionVisitor? =
            writeEffectExpression(c) { t.addOrArgument(it) }

        override fun visitEnd() {
            output(t)
        }
    }

open class ClassWriter(stringTable: StringTable) : KmClassVisitor() {
    val t = ProtoBuf.Class.newBuilder()!!
    val c = WriteContext(stringTable)

    override fun visit(flags: Flags, name: ClassName) {
        if (flags != ProtoBuf.Class.getDefaultInstance().flags) {
            t.flags = flags
        }
        t.fqName = c.getClassName(name)
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        writeTypeParameter(c, flags, name, id, variance) { t.addTypeParameter(it) }

    override fun visitSupertype(flags: Flags): KmTypeVisitor? =
        writeType(c, flags) { t.addSupertype(it) }

    override fun visitConstructor(flags: Flags): KmConstructorVisitor? =
        writeConstructor(c, flags) { t.addConstructor(it) }

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        writeFunction(c, flags, name) { t.addFunction(it) }

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        writeProperty(c, flags, name, getterFlags, setterFlags) { t.addProperty(it) }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? =
        writeTypeAlias(c, flags, name) { t.addTypeAlias(it) }

    override fun visitCompanionObject(name: String) {
        t.companionObjectName = c[name]
    }

    override fun visitNestedClass(name: String) {
        t.addNestedClassName(c[name])
    }

    override fun visitEnumEntry(name: String) {
        t.addEnumEntry(ProtoBuf.EnumEntry.newBuilder().also { enumEntry ->
            enumEntry.name = c[name]
        })
    }

    override fun visitSealedSubclass(name: ClassName) {
        t.addSealedSubclassFqName(c.getClassName(name))
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        writeVersionRequirement(c) { t.versionRequirement = it }

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? =
        c.applySingleExtension(type) {
            writeClassExtensions(type, t, c.strings)
        }

    override fun visitEnd() {
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class PackageWriter(stringTable: StringTable) : KmPackageVisitor() {
    val t = ProtoBuf.Package.newBuilder()!!
    val c = WriteContext(stringTable)

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        writeFunction(c, flags, name) { t.addFunction(it) }

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        writeProperty(c, flags, name, getterFlags, setterFlags) { t.addProperty(it) }

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? =
        writeTypeAlias(c, flags, name) { t.addTypeAlias(it) }

    override fun visitEnd() {
        c.versionRequirements.serialize()?.let {
            t.versionRequirementTable = it
        }
    }
}

open class LambdaWriter(stringTable: StringTable) : KmLambdaVisitor() {
    var t: ProtoBuf.Function.Builder? = null
    val c = WriteContext(stringTable)

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        writeFunction(c, flags, name) { t = it }
}
