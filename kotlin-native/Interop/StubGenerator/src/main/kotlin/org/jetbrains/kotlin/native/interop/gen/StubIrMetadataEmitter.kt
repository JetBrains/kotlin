/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.utils.addIfNotNull

class StubIrMetadataEmitter(
        private val context: StubIrContext,
        private val builderResult: StubIrBuilderResult,
        private val moduleName: String,
        private val bridgeBuilderResult: BridgeBuilderResult
) {
    fun emit(): KlibModuleMetadata {
        val annotations = emptyList<KmAnnotation>()
        val fragments = emitModuleFragments()
        return KlibModuleMetadata(moduleName, fragments, annotations)
    }

    private fun emitModuleFragments(): List<KmModuleFragment> =
            ModuleMetadataEmitter(
                    context.configuration.pkgName,
                    builderResult.stubs,
                    bridgeBuilderResult
            ).emit().let { kmModuleFragment ->
                // We need to create module fragment for each part of package name.
                val pkgName = context.configuration.pkgName
                val fakePackages = pkgName.mapIndexedNotNull { idx, char ->
                    if (char == '.') idx else null
                }.map { dotPosition ->
                    KmModuleFragment().also {
                        it.fqName = pkgName.substring(0, dotPosition)
                    }
                }
                fakePackages + kmModuleFragment
            }
}

/**
 * Translates single [StubContainer] to [KmModuleFragment].
 */
internal class ModuleMetadataEmitter(
        private val packageFqName: String,
        private val module: SimpleStubContainer,
        private val bridgeBuilderResult: BridgeBuilderResult
) {

    fun emit(): KmModuleFragment {
        val context = VisitingContext(bridgeBuilderResult = bridgeBuilderResult)
        val elements = KmElements(visitor.visitSimpleStubContainer(module, context))
        return writeModule(elements)
    }

    private fun writeModule(elements: KmElements) = KmModuleFragment().also { km ->
        km.fqName = packageFqName
        km.classes += elements.classes.toList()
        km.className += elements.classes.map(KmClass::name)
        km.pkg = writePackage(elements)
    }

    private fun writePackage(elements: KmElements) = KmPackage().also { km ->
        km.fqName = packageFqName
        km.typeAliases += elements.typeAliases.toList()
        km.properties += elements.properties.toList()
        km.functions += elements.functions.toList()
    }

    /**
     * StubIr translation result. Since Km* classes don't have common hierarchy we need
     * to use list of Any.
     */
    private class KmElements(result: List<Any>) {
        val classes: List<KmClass> = result.filterIsInstance<List<KmClass>>().flatten()
        val properties: List<KmProperty> = result.filterIsInstance<KmProperty>()
        val typeAliases: List<KmTypeAlias> = result.filterIsInstance<KmTypeAlias>()
        val functions: List<KmFunction> = result.filterIsInstance<KmFunction>()
        val constructors: List<KmConstructor> = result.filterIsInstance<KmConstructor>()
    }

    /**
     * Used to pass data between parents and children when visiting StubIr elements.
     */
    private data class VisitingContext(
            val container: StubContainer? = null,
            val typeParametersInterner: Interner<TypeParameterStub> = Interner(),
            val bridgeBuilderResult: BridgeBuilderResult
    ) {
        inline fun <R> withMappingExtensions(block: MappingExtensions.() -> R) =
                with (MappingExtensions(typeParametersInterner, bridgeBuilderResult), block)
    }

    private fun isTopLevelContainer(container: StubContainer?): Boolean =
            container == null

    private fun getPropertyNameInScope(property: PropertyStub, container: StubContainer?): String =
        if (isTopLevelContainer(container)) {
            getTopLevelPropertyDeclarationName(bridgeBuilderResult.kotlinFile, property)
        } else {
            property.name
        }

    private val visitor = object : StubIrVisitor<VisitingContext, Any?> {

        override fun visitClass(element: ClassStub, data: VisitingContext): List<KmClass> {
            val classVisitingContext = VisitingContext(
                    container = element,
                    typeParametersInterner = Interner(data.typeParametersInterner),
                    bridgeBuilderResult = data.bridgeBuilderResult
            )
            val children = element.children + if (element is ClassStub.Companion) {
                listOf(ConstructorStub(isPrimary = true, visibility = VisibilityModifier.PRIVATE, origin = StubOrigin.Synthetic.DefaultConstructor))
            } else emptyList()
            val elements = KmElements(children.mapNotNull { it.accept(this, classVisitingContext) })
            val kmClass = data.withMappingExtensions {
                KmClass().also { km ->
                    element.annotations.mapTo(km.annotations) { it.map() }
                    km.flags = element.flags
                    km.name = element.classifier.fqNameSerialized
                    element.superClassInit?.let { km.supertypes += it.type.map() }
                    element.interfaces.mapTo(km.supertypes) { it.map() }
                    element.classes.mapTo(km.nestedClasses) { it.nestedName() }
                    km.typeAliases += elements.typeAliases.toList()
                    km.properties += elements.properties.toList()
                    km.functions += elements.functions.toList()
                    km.constructors += elements.constructors.toList()
                    km.companionObject = element.companion?.nestedName()
                    if (element is ClassStub.Enum) {
                        element.entries.mapTo(km.klibEnumEntries) { mapEnumEntry(it, classVisitingContext) }
                    }
                }
            }
            // Metadata stores classes as flat list.
            return listOf(kmClass) + elements.classes
        }

        override fun visitTypealias(element: TypealiasStub, data: VisitingContext): KmTypeAlias =
                data.withMappingExtensions {
                    KmTypeAlias(element.flags, element.alias.topLevelName).also { km ->
                        km.underlyingType = element.aliasee.map(shouldExpandTypeAliases = false)
                        km.expandedType = element.aliasee.map()
                    }
                }

        override fun visitFunction(element: FunctionStub, data: VisitingContext) =
                data.withMappingExtensions {
                    val function = if (bridgeBuilderResult.nativeBridges.isSupported(element)) {
                        element
                    } else {
                        element.copy(
                                external = false,
                                annotations = listOf(AnnotationStub.Deprecated.unableToImport)
                        )
                    }
                    KmFunction(function.flags, function.name).also { km ->
                        km.receiverParameterType = function.receiver?.type?.map()
                        function.typeParameters.mapTo(km.typeParameters) { it.map() }
                        function.parameters.mapTo(km.valueParameters) { it.map() }
                        function.annotations.mapTo(km.annotations) { it.map() }
                        km.returnType = function.returnType.map()
                    }
                }

        override fun visitProperty(element: PropertyStub, data: VisitingContext) =
                data.withMappingExtensions {
                    val property = when (val bridgeSupportedKind = element.bridgeSupportedKind) {
                        null -> element.copy(
                                kind = PropertyStub.Kind.Val(PropertyAccessor.Getter.SimpleGetter()),
                                annotations = listOf(AnnotationStub.Deprecated.unableToImport)
                        )
                        element.kind -> element
                        else -> element.copy(kind = bridgeSupportedKind)
                    }
                    val name = getPropertyNameInScope(property, data.container)
                    KmProperty(property.flags, name, property.getterFlags, property.setterFlags).also { km ->
                        property.annotations.mapTo(km.annotations) { it.map() }
                        km.receiverParameterType = property.receiverType?.map()
                        km.returnType = property.type.map()
                        val kind = property.kind
                        if (kind is PropertyStub.Kind.Var) {
                            kind.setter.annotations.mapTo(km.setterAnnotations) { it.map() }
                            // TODO: Maybe it's better to explicitly add setter parameter in stub.
                            km.setterParameter = FunctionParameterStub("value", property.type).map()
                        }
                        km.getterAnnotations += when (kind) {
                            is PropertyStub.Kind.Val -> kind.getter.annotations.map { it.map() }
                            is PropertyStub.Kind.Var -> kind.getter.annotations.map { it.map() }
                            is PropertyStub.Kind.Constant -> emptyList()
                        }
                        if (kind is PropertyStub.Kind.Constant) {
                            km.compileTimeValue = kind.constant.mapToAnnotationArgument()
                        }
                    }
                }

        override fun visitConstructor(constructorStub: ConstructorStub, data: VisitingContext) =
                data.withMappingExtensions {
                    KmConstructor(constructorStub.flags).apply {
                        constructorStub.parameters.mapTo(valueParameters, { it.map() })
                        constructorStub.annotations.mapTo(annotations, { it.map() })
                    }
                }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: VisitingContext) {
            // TODO("not implemented")
        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: VisitingContext): List<Any> =
                simpleStubContainer.children.mapNotNull { it.accept(this, data) } +
                        simpleStubContainer.simpleContainers.flatMap { visitSimpleStubContainer(it, data) }

        private fun mapEnumEntry(enumEntry: EnumEntryStub, data: VisitingContext): KlibEnumEntry =
                data.withMappingExtensions {
                    KlibEnumEntry(
                            name = enumEntry.name,
                            ordinal = enumEntry.ordinal,
                            annotations = mutableListOf(enumEntry.constant.mapToConstantAnnotation())
                    )
                }
    }
}

/**
 * Collection of extension functions that simplify translation of
 * StubIr elements to Kotlin Metadata.
 */
private class MappingExtensions(
        private val typeParametersInterner: Interner<TypeParameterStub>,
        private val bridgeBuilderResult: BridgeBuilderResult
) {

    private fun flagsOfNotNull(vararg flags: Flag?): Flags =
            flagsOf(*listOfNotNull(*flags).toTypedArray())

    private fun <K, V> mapOfNotNull(vararg entries: Pair<K, V>?): Map<K, V> =
            listOfNotNull(*entries).toMap()

    private val VisibilityModifier.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC.takeIf { this == VisibilityModifier.PUBLIC },
                Flag.IS_PROTECTED.takeIf { this == VisibilityModifier.PROTECTED },
                Flag.IS_INTERNAL.takeIf { this == VisibilityModifier.INTERNAL },
                Flag.IS_PRIVATE.takeIf { this == VisibilityModifier.PRIVATE }
        )

    private val MemberStubModality.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_FINAL.takeIf { this == MemberStubModality.FINAL },
                Flag.IS_OPEN.takeIf { this == MemberStubModality.OPEN },
                Flag.IS_ABSTRACT.takeIf { this == MemberStubModality.ABSTRACT }
        )

    val FunctionStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC,
                Flag.Function.IS_EXTERNAL.takeIf { this.external },
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES.takeIf { !this.hasStableParameterNames }
        ) or modality.flags

    val Classifier.fqNameSerialized: String
        get() = buildString {
            if (pkg.isNotEmpty()) {
                append(pkg.replace('.', '/'))
                append('/')
            }
            // Nested classes should dot-separated.
            append(getRelativeFqName(asSimpleName = false))
        }

    val PropertyStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC,
                Flag.Property.IS_DECLARATION,
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.Property.HAS_CONSTANT.takeIf { kind is PropertyStub.Kind.Constant },
                Flag.Property.HAS_GETTER,
                Flag.Property.HAS_SETTER.takeIf { kind is PropertyStub.Kind.Var },
                when (kind) {
                    is PropertyStub.Kind.Val -> null
                    is PropertyStub.Kind.Var -> Flag.Property.IS_VAR
                    is PropertyStub.Kind.Constant -> Flag.Property.IS_CONST
                }
        ) or modality.flags

    val PropertyStub.getterFlags: Flags
        get() = when (val kind = kind) {
            is PropertyStub.Kind.Val -> kind.getter.flags(modality)
            is PropertyStub.Kind.Var -> kind.getter.flags(modality)
            is PropertyStub.Kind.Constant -> kind.flags
        }

    val PropertyStub.Kind.Constant.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC,
                Flag.IS_FINAL
        )

    private fun PropertyAccessor.Getter.flags(propertyModality: MemberStubModality): Flags =
        flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.IS_PUBLIC,
                Flag.PropertyAccessor.IS_NOT_DEFAULT,
                Flag.PropertyAccessor.IS_EXTERNAL.takeIf { this is PropertyAccessor.Getter.ExternalGetter }
        ) or propertyModality.flags

    val PropertyStub.setterFlags: Flags
        get() = when (val kind = kind) {
            is PropertyStub.Kind.Var -> kind.setter.flags(modality)
            else -> flagsOf()
        }

    private fun PropertyAccessor.Setter.flags(propertyModality: MemberStubModality): Flags =
        flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.IS_PUBLIC,
                Flag.PropertyAccessor.IS_NOT_DEFAULT,
                Flag.PropertyAccessor.IS_EXTERNAL.takeIf { this is PropertyAccessor.Setter.ExternalSetter }
        ) or propertyModality.flags

    val StubType.flags: Flags
        get() = flagsOfNotNull(
                Flag.Type.IS_NULLABLE.takeIf { nullable }
        )

    val AbbreviatedType.expandedTypeFlags: Flags
        get() = flagsOfNotNull(
                Flag.Type.IS_NULLABLE.takeIf { isEffectivelyNullable() }
        )

    val TypealiasStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC
        )

    val FunctionParameterStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() }
        )

    val ClassStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.IS_PUBLIC,
                Flag.IS_OPEN.takeIf { this is ClassStub.Simple && modality == ClassStubModality.OPEN },
                Flag.IS_FINAL.takeIf { this is ClassStub.Simple && modality == ClassStubModality.NONE },
                Flag.IS_ABSTRACT.takeIf { this is ClassStub.Simple
                        && (modality == ClassStubModality.ABSTRACT || modality == ClassStubModality.INTERFACE) },
                Flag.Class.IS_INTERFACE.takeIf { this is ClassStub.Simple && modality == ClassStubModality.INTERFACE },
                Flag.Class.IS_COMPANION_OBJECT.takeIf { this is ClassStub.Companion },
                Flag.Class.IS_CLASS.takeIf { this is ClassStub.Simple && modality != ClassStubModality.INTERFACE },
                Flag.Class.IS_ENUM_CLASS.takeIf { this is ClassStub.Enum }
        )


    val ConstructorStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.Constructor.IS_SECONDARY.takeIf { !isPrimary },
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() }
        ) or visibility.flags

    private tailrec fun StubType.isEffectivelyNullable(): Boolean =
            when {
                nullable -> true
                this !is AbbreviatedType -> false
                else -> underlyingType.isEffectivelyNullable()
            }

    fun AnnotationStub.map(): KmAnnotation {
        fun Pair<String, String>.asAnnotationArgument() =
                (first to KmAnnotationArgument.StringValue(second)).takeIf { second.isNotEmpty() }

        fun replaceWith(replaceWith: String) = KmAnnotationArgument.AnnotationValue(KmAnnotation(
                Classifier.topLevel("kotlin", "ReplaceWith").fqNameSerialized,
                mapOfNotNull(
                        "imports" to KmAnnotationArgument.ArrayValue(emptyList()),
                        ("expression" to replaceWith).asAnnotationArgument()
                )
        ))

        fun deprecationLevel(level: DeprecationLevel) = KmAnnotationArgument.EnumValue(
                Classifier.topLevel("kotlin", "DeprecationLevel").fqNameSerialized,
                level.name
        )

        val args = when (this) {
            AnnotationStub.ObjC.ConsumesReceiver -> emptyMap()
            AnnotationStub.ObjC.ReturnsRetained -> emptyMap()
            is AnnotationStub.ObjC.Method -> mapOfNotNull(
                    ("selector" to selector).asAnnotationArgument(),
                    ("encoding" to encoding).asAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            is AnnotationStub.ObjC.Factory -> mapOfNotNull(
                    ("selector" to selector).asAnnotationArgument(),
                    ("encoding" to encoding).asAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            AnnotationStub.ObjC.Consumed -> emptyMap()
            is AnnotationStub.ObjC.Constructor -> mapOfNotNull(
                    ("designated" to KmAnnotationArgument.BooleanValue(designated)),
                    ("initSelector" to selector).asAnnotationArgument()
            )
            is AnnotationStub.ObjC.ExternalClass -> mapOfNotNull(
                    ("protocolGetter" to protocolGetter).asAnnotationArgument(),
                    ("binaryName" to binaryName).asAnnotationArgument()
            )
            AnnotationStub.CCall.CString -> emptyMap()
            AnnotationStub.CCall.WCString -> emptyMap()
            is AnnotationStub.CCall.Symbol -> mapOfNotNull(
                    ("id" to symbolName).asAnnotationArgument()
            )
            is AnnotationStub.CStruct -> mapOfNotNull(
                    ("spelling" to struct).asAnnotationArgument()
            )
            is AnnotationStub.CNaturalStruct ->
                error("@CNaturalStruct should not be used for Kotlin/Native interop")
            is AnnotationStub.CLength -> mapOfNotNull(
                    "value" to KmAnnotationArgument.LongValue(length)
            )
            is AnnotationStub.Deprecated -> mapOfNotNull(
                    ("message" to message).asAnnotationArgument(),
                    ("replaceWith" to replaceWith(replaceWith)),
                    ("level" to deprecationLevel(level))
            )
            is AnnotationStub.CEnumEntryAlias -> mapOfNotNull(
                    ("entryName" to entryName).asAnnotationArgument()
            )
            is AnnotationStub.CEnumVarTypeSize -> mapOfNotNull(
                    ("size" to KmAnnotationArgument.IntValue(size))
            )
            is AnnotationStub.CStruct.MemberAt -> mapOfNotNull(
                    ("offset" to KmAnnotationArgument.LongValue(offset))
            )
            is AnnotationStub.CStruct.ArrayMemberAt -> mapOfNotNull(
                    ("offset" to KmAnnotationArgument.LongValue(offset))
            )
            is AnnotationStub.CStruct.BitField -> mapOfNotNull(
                    ("offset" to KmAnnotationArgument.LongValue(offset)),
                    ("size" to KmAnnotationArgument.IntValue(size))
            )
            is AnnotationStub.CStruct.VarType -> mapOfNotNull(
                    ("size" to KmAnnotationArgument.LongValue(size)),
                    ("align" to KmAnnotationArgument.IntValue(align))
            )
        }
        return KmAnnotation(classifier.fqNameSerialized, args)
    }

    /**
     * @param shouldExpandTypeAliases describes how should we write type aliases.
     * If [shouldExpandTypeAliases] is true then type alias-based types are written as
     * ```
     * Type {
     *  abbreviatedType = AbbreviatedType.abbreviatedClassifier
     *  classifier = AbbreviatedType.underlyingType
     *  arguments = AbbreviatedType.underlyingType.typeArguments
     * }
     * ```
     * So we basically replacing type alias with underlying class.
     * Otherwise:
     * ```
     * Type {
     *  classifier = AbbreviatedType.abbreviatedClassifier
     * }
     * ```
     * As of 25 Nov 2019, the latter form is used only for KmTypeAlias.underlyingType.
     */
    // TODO: Add caching if needed.
    fun StubType.map(shouldExpandTypeAliases: Boolean = true): KmType = when (this) {
        is AbbreviatedType -> {
            val typeAliasClassifier = KmClassifier.TypeAlias(abbreviatedClassifier.fqNameSerialized)
            val typeArguments = typeArguments.map { it.map(shouldExpandTypeAliases) }
            val abbreviatedType = KmType(flags).also { km ->
                km.classifier = typeAliasClassifier
                km.arguments += typeArguments
            }
            if (shouldExpandTypeAliases) {
                KmType(expandedTypeFlags).also { km ->
                    km.abbreviatedType = abbreviatedType
                    val kmUnderlyingType = underlyingType.map(true)
                    km.arguments += kmUnderlyingType.arguments
                    km.classifier = kmUnderlyingType.classifier
                }
            } else {
                abbreviatedType
            }
        }
        is ClassifierStubType -> KmType(flags).also { km ->
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is FunctionalType -> KmType(flags).also { km ->
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is TypeParameterType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeParameter(id)
        }
    }

    fun FunctionParameterStub.map(): KmValueParameter =
            KmValueParameter(flags, name).also { km ->
                val kmType = type.map()
                if (isVararg) {
                    km.varargElementType = kmType
                    km.type = ClassifierStubType(
                            Classifier.topLevel("kotlin", "Array"),
                            listOf(TypeArgumentStub(type))
                    ).map()
                } else {
                    km.type = kmType
                }
                annotations.mapTo(km.annotations, { it.map() })
            }

    fun TypeParameterStub.map(): KmTypeParameter =
            KmTypeParameter(flagsOf(), name, id, KmVariance.INVARIANT).also { km ->
                km.upperBounds.addIfNotNull(upperBound?.map())
            }

    private fun TypeArgument.map(expanded: Boolean = true): KmTypeProjection = when (this) {
        TypeArgument.StarProjection -> KmTypeProjection.STAR
        is TypeArgumentStub -> KmTypeProjection(variance.map(), type.map(expanded))
        else -> error("Unexpected TypeArgument: $this")
    }

    private fun TypeArgument.Variance.map(): KmVariance = when (this) {
        TypeArgument.Variance.INVARIANT -> KmVariance.INVARIANT
        TypeArgument.Variance.IN -> KmVariance.IN
        TypeArgument.Variance.OUT -> KmVariance.OUT
    }

    fun ConstantStub.mapToAnnotationArgument(): KmAnnotationArgument<*> = when (this) {
        is StringConstantStub -> KmAnnotationArgument.StringValue(value)
        is IntegralConstantStub -> when (size) {
            1 -> if (isSigned) {
                KmAnnotationArgument.ByteValue(value.toByte())
            } else {
                KmAnnotationArgument.UByteValue(value.toByte())
            }
            2 -> if (isSigned) {
                KmAnnotationArgument.ShortValue(value.toShort())
            } else {
                KmAnnotationArgument.UShortValue(value.toShort())
            }
            4 -> if (isSigned) {
                KmAnnotationArgument.IntValue(value.toInt())
            } else {
                KmAnnotationArgument.UIntValue(value.toInt())
            }
            8 -> if (isSigned) {
                KmAnnotationArgument.LongValue(value)
            } else {
                KmAnnotationArgument.ULongValue(value)
            }

            else -> error("Integral constant of value $value with unexpected size of $size.")
        }
        is DoubleConstantStub -> when (size) {
            4 -> KmAnnotationArgument.FloatValue(value.toFloat())
            8 -> KmAnnotationArgument.DoubleValue(value)
            else -> error("Floating-point constant of value $value with unexpected size of $size.")
        }
    }

    fun ConstantStub.mapToConstantAnnotation(): KmAnnotation =
            KmAnnotation(
                    determineConstantAnnotationClassifier().fqNameSerialized,
                    mapOf("value" to mapToAnnotationArgument())
            )

    private val TypeParameterType.id: Int
        get() = typeParameterDeclaration.id

    private val TypeParameterStub.id: Int
        get() = typeParametersInterner.intern(this)

    /**
     * Sometimes we can't generate bridge for getter or setter.
     * For example, it may happen due to bug in libclang which may
     * erroneously skip `const` qualifier of global variable.
     *
     * In this case we should change effective property's kind to either `val`
     * or even omit the declaration at all.
     */
    val PropertyStub.bridgeSupportedKind: PropertyStub.Kind?
        get() = when (kind) {
            is PropertyStub.Kind.Var -> {
                val isGetterSupported = bridgeBuilderResult.nativeBridges.isSupported(kind.getter)
                val isSetterSupported = bridgeBuilderResult.nativeBridges.isSupported(kind.setter)
                when {
                    isGetterSupported && isSetterSupported -> kind
                    !isGetterSupported -> null
                    else -> PropertyStub.Kind.Val(kind.getter)
                }
            }
            is PropertyStub.Kind.Val -> {
                val isGetterSupported = bridgeBuilderResult.nativeBridges.isSupported(kind.getter)
                if (isGetterSupported) {
                    kind
                } else {
                    null
                }
            }
            is PropertyStub.Kind.Constant -> kind
        }
}