/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.KlibEnumEntry
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlin.metadata.*
import kotlinx.metadata.klib.*
import kotlin.metadata.internal.common.*
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
                    km.modifiersFrom(element)
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
                    KmTypeAlias(element.alias.topLevelName).also { km ->
                        element.annotations.mapTo(km.annotations) { it.map() }
                        km.visibility = Visibility.PUBLIC
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
                                annotations = mutableListOf(AnnotationStub.Deprecated.unableToImport)
                        )
                    }
                    KmFunction(function.name).also { km ->
                        km.modifiersFrom(function)
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
                                annotations = mutableListOf(AnnotationStub.Deprecated.unableToImport)
                        )
                        element.kind -> element
                        else -> element.copy(kind = bridgeSupportedKind)
                    }
                    val name = getPropertyNameInScope(property, data.container)
                    KmProperty(name).also { km ->
                        km.modifiersFrom(property)
                        km.getter.getterModifiersFrom(property)
                        km.setter = setterFrom(property)
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
                    KmConstructor().apply {
                        modifiersFrom(constructorStub)
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

    private fun <K, V> mapOfNotNull(vararg entries: Pair<K, V>?): Map<K, V> =
            listOfNotNull(*entries).toMap()

    private val VisibilityModifier.kmVisibility: Visibility
        get() = when (this) {
            VisibilityModifier.PUBLIC -> Visibility.PUBLIC
            VisibilityModifier.PROTECTED -> Visibility.PROTECTED
            VisibilityModifier.INTERNAL -> Visibility.INTERNAL
            VisibilityModifier.PRIVATE -> Visibility.PRIVATE
        }

    private val MemberStubModality.kmModality: Modality
        get() = when (this) {
            MemberStubModality.FINAL -> Modality.FINAL
            MemberStubModality.OPEN -> Modality.OPEN
            MemberStubModality.ABSTRACT -> Modality.ABSTRACT
        }

    fun KmFunction.modifiersFrom(fs: FunctionStub) {
        visibility = Visibility.PUBLIC
        modality = fs.modality.kmModality
        isExternal = fs.external
        hasAnnotations = fs.annotations.isNotEmpty()
        hasNonStableParameterNames = !fs.hasStableParameterNames
    }

    val Classifier.fqNameSerialized: String
        get() = buildString {
            if (pkg.isNotEmpty()) {
                append(pkg.replace('.', '/'))
                append('/')
            }
            // Nested classes should dot-separated.
            append(getRelativeFqName(asSimpleName = false))
        }

    fun KmProperty.modifiersFrom(ps: PropertyStub) {
        visibility = Visibility.PUBLIC
        modality = ps.modality.kmModality
        kind = MemberKind.DECLARATION
        hasAnnotations = ps.annotations.isNotEmpty()
        when (ps.kind) {
            is PropertyStub.Kind.Val -> {}
            is PropertyStub.Kind.Var -> {
                isVar = true
            }
            is PropertyStub.Kind.Constant -> {
                isConst = true
                hasConstant = true
            }
        }
    }

    fun KmPropertyAccessorAttributes.getterModifiersFrom(ps: PropertyStub) {
        val getter = when (val kind = ps.kind) {
            is PropertyStub.Kind.Val -> kind.getter
            is PropertyStub.Kind.Var -> kind.getter
            is PropertyStub.Kind.Constant -> null
        }
        if (getter == null) {
            // constant
            visibility = Visibility.PUBLIC
            modality = Modality.FINAL
        } else {
            visibility = Visibility.PUBLIC
            modality = ps.modality.kmModality
            hasAnnotations = getter.annotations.isNotEmpty()
            isNotDefault = true
            isExternal = getter is PropertyAccessor.Getter.ExternalGetter
        }
    }

    fun setterFrom(ps: PropertyStub): KmPropertyAccessorAttributes? {
        val setter = if (ps.kind is PropertyStub.Kind.Var) ps.kind.setter else return null
        return KmPropertyAccessorAttributes().apply {
            hasAnnotations = setter.annotations.isNotEmpty()
            visibility = Visibility.PUBLIC
            modality = ps.modality.kmModality
            isNotDefault = true
            isExternal = setter is PropertyAccessor.Setter.ExternalSetter
        }
    }

    fun KmType.modifiersFrom(st: StubType) {
        isNullable = st.nullable
    }

    fun KmClass.modifiersFrom(cs: ClassStub) {
        hasAnnotations = cs.annotations.isNotEmpty()
        visibility = Visibility.PUBLIC
        kind = when (cs) {
            is ClassStub.Simple -> {
                modality = when (cs.modality) {
                    ClassStubModality.NONE -> Modality.FINAL
                    ClassStubModality.OPEN -> Modality.OPEN
                    ClassStubModality.ABSTRACT, ClassStubModality.INTERFACE -> Modality.ABSTRACT
                }
                if (cs.modality == ClassStubModality.INTERFACE) ClassKind.INTERFACE else ClassKind.CLASS
            }
            is ClassStub.Companion -> ClassKind.COMPANION_OBJECT
            is ClassStub.Enum -> ClassKind.ENUM_CLASS
        }
    }

    fun KmConstructor.modifiersFrom(cs: ConstructorStub) {
        visibility = cs.visibility.kmVisibility
        isSecondary = !cs.isPrimary
        hasAnnotations = cs.annotations.isNotEmpty()
    }

    private tailrec fun StubType.isEffectivelyNullable(): Boolean =
            when {
                nullable -> true
                this !is AbbreviatedType -> false
                else -> underlyingType.isEffectivelyNullable()
            }

    fun AnnotationStub.map(): KmAnnotation {
        fun Pair<String, String>.asOptionalAnnotationArgument(): Pair<String, KmAnnotationArgument.StringValue>? {
            val (argumentName, argumentValue) = this
            return if (argumentValue.isEmpty()) null else argumentName to KmAnnotationArgument.StringValue(argumentValue)
        }

        fun replaceWith(replaceWith: String) = KmAnnotationArgument.AnnotationValue(KmAnnotation(
                Classifier.topLevel("kotlin", "ReplaceWith").fqNameSerialized,
                mapOfNotNull(
                        "imports" to KmAnnotationArgument.ArrayValue(emptyList()),
                        "expression" to KmAnnotationArgument.StringValue(replaceWith)
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
                    ("selector" to selector).asOptionalAnnotationArgument(),
                    ("encoding" to encoding).asOptionalAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            is AnnotationStub.ObjC.Direct -> mapOfNotNull(
                    ("symbol" to symbol).asOptionalAnnotationArgument(),
            )
            is AnnotationStub.ObjC.Factory -> mapOfNotNull(
                    ("selector" to selector).asOptionalAnnotationArgument(),
                    ("encoding" to encoding).asOptionalAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            AnnotationStub.ObjC.Consumed -> emptyMap()
            is AnnotationStub.ObjC.Constructor -> mapOfNotNull(
                    ("designated" to KmAnnotationArgument.BooleanValue(designated)),
                    ("initSelector" to selector).asOptionalAnnotationArgument()
            )
            is AnnotationStub.ObjC.ExternalClass -> mapOfNotNull(
                    ("protocolGetter" to protocolGetter).asOptionalAnnotationArgument(),
                    ("binaryName" to binaryName).asOptionalAnnotationArgument()
            )
            AnnotationStub.CCall.CString -> emptyMap()
            AnnotationStub.CCall.WCString -> emptyMap()
            is AnnotationStub.CCall.Symbol -> mapOfNotNull(
                    ("id" to symbolName).asOptionalAnnotationArgument()
            )
            is AnnotationStub.CCall.CppClassConstructor -> emptyMap()
            is AnnotationStub.CStruct -> mapOfNotNull(
                    ("spelling" to struct).asOptionalAnnotationArgument()
            )
            is AnnotationStub.CNaturalStruct ->
                error("@CNaturalStruct should not be used for Kotlin/Native interop")
            is AnnotationStub.CLength -> mapOfNotNull(
                    "value" to KmAnnotationArgument.LongValue(length)
            )
            is AnnotationStub.Deprecated -> mapOfNotNull(
                    ("message" to message).asOptionalAnnotationArgument(),
                    ("replaceWith" to replaceWith(replaceWith)),
                    ("level" to deprecationLevel(level))
            )
            is AnnotationStub.CEnumEntryAlias -> mapOfNotNull(
                    ("entryName" to entryName).asOptionalAnnotationArgument()
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
            is AnnotationStub.CStruct.CPlusPlusClass -> emptyMap()
            is AnnotationStub.CStruct.ManagedType -> emptyMap()
            is AnnotationStub.ExperimentalForeignApi -> emptyMap()
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
            val abbreviatedType = KmType().also { km ->
                km.modifiersFrom(this)
                km.classifier = typeAliasClassifier
                km.arguments += typeArguments
            }
            if (shouldExpandTypeAliases) {
                KmType().also { km ->
                    km.isNullable = this.isEffectivelyNullable()
                    km.abbreviatedType = abbreviatedType
                    val kmUnderlyingType = underlyingType.map(true)
                    km.arguments += kmUnderlyingType.arguments
                    km.classifier = kmUnderlyingType.classifier
                }
            } else {
                abbreviatedType
            }
        }
        is ClassifierStubType -> KmType().also { km ->
            km.modifiersFrom(this)
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is FunctionalType -> KmType().also { km ->
            km.modifiersFrom(this)
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is TypeParameterType -> KmType().also { km ->
            km.modifiersFrom(this)
            km.classifier = KmClassifier.TypeParameter(id)
        }
    }

    fun FunctionParameterStub.map(): KmValueParameter =
            KmValueParameter(name).also { km ->
                km.hasAnnotations = annotations.isNotEmpty()
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
            KmTypeParameter(name, id, KmVariance.INVARIANT).also { km ->
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

    fun ConstantStub.mapToAnnotationArgument(): KmAnnotationArgument = when (this) {
        is StringConstantStub -> KmAnnotationArgument.StringValue(value)
        is IntegralConstantStub -> when (size) {
            1 -> if (isSigned) {
                KmAnnotationArgument.ByteValue(value.toByte())
            } else {
                KmAnnotationArgument.UByteValue(value.toUByte())
            }
            2 -> if (isSigned) {
                KmAnnotationArgument.ShortValue(value.toShort())
            } else {
                KmAnnotationArgument.UShortValue(value.toUShort())
            }
            4 -> if (isSigned) {
                KmAnnotationArgument.IntValue(value.toInt())
            } else {
                KmAnnotationArgument.UIntValue(value.toUInt())
            }
            8 -> if (isSigned) {
                KmAnnotationArgument.LongValue(value)
            } else {
                KmAnnotationArgument.ULongValue(value.toULong())
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
