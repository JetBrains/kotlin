/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*

internal class MacroConstantStubBuilder(
        override val context: StubsBuildingContext,
        private val constant: ConstantDef
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val kotlinName = constant.name
        val origin = StubOrigin.Constant(constant)
        val declaration = when (constant) {
            is IntegerConstantDef -> {
                val literal = context.tryCreateIntegralStub(constant.type, constant.value) ?: return emptyList()
                val kotlinType = context.mirror(constant.type).argType.toStubIrType()
                when (context.platform) {
                    KotlinPlatform.NATIVE -> PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Constant(literal), origin = origin)
                    // No reason to make it const val with backing field on Kotlin/JVM yet:
                    KotlinPlatform.JVM -> {
                        val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
                        PropertyStub(kotlinName, kotlinType, PropertyStub.Kind.Val(getter), origin = origin)
                    }
                }
            }
            is FloatingConstantDef -> {
                val literal = context.tryCreateDoubleStub(constant.type, constant.value) ?: return emptyList()
                val kind = when (context.generationMode) {
                    GenerationMode.SOURCE_CODE -> {
                        PropertyStub.Kind.Val(PropertyAccessor.Getter.SimpleGetter(constant = literal))
                    }
                    GenerationMode.METADATA -> {
                        PropertyStub.Kind.Constant(literal)
                    }
                }
                val kotlinType = context.mirror(constant.type).argType.toStubIrType()
                PropertyStub(kotlinName, kotlinType, kind, origin = origin)
            }
            is StringConstantDef -> {
                val literal = StringConstantStub(constant.value)
                val kind = when (context.generationMode) {
                    GenerationMode.SOURCE_CODE -> {
                        PropertyStub.Kind.Val(PropertyAccessor.Getter.SimpleGetter(constant = literal))
                    }
                    GenerationMode.METADATA -> {
                        PropertyStub.Kind.Constant(literal)
                    }
                }
                PropertyStub(kotlinName, KotlinTypes.string.toStubIrType(), kind, origin = origin)
            }
            else -> return emptyList()
        }
        return listOf(declaration)
    }
}

internal class StructStubBuilder(
        override val context: StubsBuildingContext,
        private val decl: StructDecl
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val platform = context.platform
        val def = decl.def ?: return generateForwardStruct(decl)

        val structAnnotation: AnnotationStub? = if (platform == KotlinPlatform.JVM) {
            if (def.kind == StructDef.Kind.STRUCT && def.fieldsHaveDefaultAlignment()) {
                AnnotationStub.CNaturalStruct(def.members)
            } else {
                null
            }
        } else {
            tryRenderStructOrUnion(def)?.let {
                AnnotationStub.CStruct(it)
            }
        }
        val classifier = context.getKotlinClassForPointed(decl)

        val fields: List<PropertyStub?> = def.fields.map { field ->
            try {
                assert(field.name.isNotEmpty())
                assert(field.offset % 8 == 0L)
                val offset = field.offset / 8
                val fieldRefType = context.mirror(field.type)
                val unwrappedFieldType = field.type.unwrapTypedefs()
                val origin = StubOrigin.StructMember(field)
                val fieldName = mangleSimple(field.name)
                if (unwrappedFieldType is ArrayType) {
                    val type = (fieldRefType as TypeMirror.ByValue).valueType
                    val annotations = if (platform == KotlinPlatform.JVM) {
                        val length = getArrayLength(unwrappedFieldType)
                        // TODO: @CLength should probably be used on types instead of properties.
                        listOf(AnnotationStub.CLength(length))
                    } else {
                        emptyList()
                    }
                    val getter = when (context.generationMode) {
                        GenerationMode.SOURCE_CODE -> PropertyAccessor.Getter.ArrayMemberAt(offset)
                        GenerationMode.METADATA -> PropertyAccessor.Getter.ExternalGetter(listOf(AnnotationStub.CStruct.ArrayMemberAt(offset)))
                    }
                    val kind = PropertyStub.Kind.Val(getter)
                    // TODO: Should receiver be added?
                    PropertyStub(fieldName, type.toStubIrType(), kind, annotations = annotations, origin = origin)
                } else {
                    val pointedType = fieldRefType.pointedType.toStubIrType()
                    val pointedTypeArgument = TypeArgumentStub(pointedType)
                    if (fieldRefType is TypeMirror.ByValue) {
                        val getter: PropertyAccessor.Getter
                        val setter: PropertyAccessor.Setter
                        when (context.generationMode) {
                            GenerationMode.SOURCE_CODE -> {
                                getter = PropertyAccessor.Getter.MemberAt(offset, typeArguments = listOf(pointedTypeArgument), hasValueAccessor = true)
                                setter = PropertyAccessor.Setter.MemberAt(offset, typeArguments = listOf(pointedTypeArgument))
                            }
                            GenerationMode.METADATA -> {
                                getter = PropertyAccessor.Getter.ExternalGetter(listOf(AnnotationStub.CStruct.MemberAt(offset)))
                                setter = PropertyAccessor.Setter.ExternalSetter(listOf(AnnotationStub.CStruct.MemberAt(offset)))
                            }
                        }
                        val kind = PropertyStub.Kind.Var(getter, setter)
                        PropertyStub(fieldName, fieldRefType.argType.toStubIrType(), kind, origin = origin)
                    } else {
                        val accessor = when (context.generationMode) {
                            GenerationMode.SOURCE_CODE -> PropertyAccessor.Getter.MemberAt(offset, hasValueAccessor = false)
                            GenerationMode.METADATA -> PropertyAccessor.Getter.ExternalGetter(listOf(AnnotationStub.CStruct.MemberAt(offset)))
                        }
                        val kind = PropertyStub.Kind.Val(accessor)
                        PropertyStub(fieldName, pointedType, kind, origin = origin)
                    }
                }
            } catch (e: Throwable) {
                null
            }
        }

        val bitFields: List<PropertyStub> = def.bitFields.map { field ->
            val typeMirror = context.mirror(field.type)
            val typeInfo = typeMirror.info
            val kotlinType = typeMirror.argType
            val signed = field.type.isIntegerTypeSigned()
            val fieldName = mangleSimple(field.name)
            val kind = when (context.generationMode) {
                GenerationMode.SOURCE_CODE -> {
                    val readBits = PropertyAccessor.Getter.ReadBits(field.offset, field.size, signed)
                    val writeBits = PropertyAccessor.Setter.WriteBits(field.offset, field.size)
                    context.bridgeComponentsBuilder.getterToBridgeInfo[readBits] = BridgeGenerationInfo("", typeInfo)
                    context.bridgeComponentsBuilder.setterToBridgeInfo[writeBits] = BridgeGenerationInfo("", typeInfo)
                    PropertyStub.Kind.Var(readBits, writeBits)
                }
                GenerationMode.METADATA -> {
                    val readBits = PropertyAccessor.Getter.ExternalGetter(listOf(AnnotationStub.CStruct.BitField(field.offset, field.size)))
                    val writeBits = PropertyAccessor.Setter.ExternalSetter(listOf(AnnotationStub.CStruct.BitField(field.offset, field.size)))
                    PropertyStub.Kind.Var(readBits, writeBits)
                }
            }
            PropertyStub(fieldName, kotlinType.toStubIrType(), kind, origin = StubOrigin.StructMember(field))
        }

        val superClass = context.platform.getRuntimeType("CStructVar")
        require(superClass is ClassifierStubType)
        val rawPtrConstructorParam = FunctionParameterStub("rawPtr", context.platform.getRuntimeType("NativePtr"))
        val origin = StubOrigin.Struct(decl)
        val primaryConstructor = ConstructorStub(
                parameters = listOf(rawPtrConstructorParam),
                isPrimary = true,
                annotations = emptyList(),
                origin = origin
        )
        val superClassInit = SuperClassInit(superClass, listOf(GetConstructorParameter(rawPtrConstructorParam)))

        val companionSuper = superClass.nested("Type")
        val typeSize = listOf(IntegralConstantStub(def.size, 4, true), IntegralConstantStub(def.align.toLong(), 4, true))
        val companionSuperInit = SuperClassInit(companionSuper, typeSize)
        val companionClassifier = classifier.nested("Companion")
        val annotation = AnnotationStub.CStruct.VarType(def.size, def.align).takeIf {
            context.generationMode == GenerationMode.METADATA
        }
        val companion = ClassStub.Companion(
                companionClassifier,
                superClassInit = companionSuperInit,
                annotations = listOfNotNull(annotation, AnnotationStub.Deprecated.deprecatedCVariableCompanion)
        )

        return listOf(ClassStub.Simple(
                classifier,
                origin = origin,
                properties = fields.filterNotNull() + if (platform == KotlinPlatform.NATIVE) bitFields else emptyList(),
                constructors = listOf(primaryConstructor),
                methods = emptyList(),
                modality = ClassStubModality.NONE,
                annotations = listOfNotNull(structAnnotation),
                superClassInit = superClassInit,
                companion = companion
        ))
    }

    private fun getArrayLength(type: ArrayType): Long {
        val unwrappedElementType = type.elemType.unwrapTypedefs()
        val elementLength = if (unwrappedElementType is ArrayType) {
            getArrayLength(unwrappedElementType)
        } else {
            1L
        }

        val elementCount = when (type) {
            is ConstArrayType -> type.length
            is IncompleteArrayType -> 0L
            else -> TODO(type.toString())
        }

        return elementLength * elementCount
    }

    private tailrec fun Type.isIntegerTypeSigned(): Boolean = when (this) {
        is IntegerType -> this.isSigned
        is BoolType -> false
        is EnumType -> this.def.baseType.isIntegerTypeSigned()
        is Typedef -> this.def.aliased.isIntegerTypeSigned()
        else -> error(this)
    }

    /**
     * Produces to [out] the definition of Kotlin class representing the reference to given forward (incomplete) struct.
     */
    private fun generateForwardStruct(s: StructDecl): List<StubIrElement> = when (context.platform) {
        KotlinPlatform.JVM -> {
            val classifier = context.getKotlinClassForPointed(s)
            val superClass = context.platform.getRuntimeType("COpaque")
            val rawPtrConstructorParam = FunctionParameterStub("rawPtr", context.platform.getRuntimeType("NativePtr"))
            val superClassInit = SuperClassInit(superClass, listOf(GetConstructorParameter(rawPtrConstructorParam)))
            val origin = StubOrigin.Struct(s)
            val primaryConstructor = ConstructorStub(listOf(rawPtrConstructorParam), emptyList(), isPrimary = true, origin = origin)
            listOf(ClassStub.Simple(
                    classifier,
                    ClassStubModality.NONE,
                    constructors = listOf(primaryConstructor),
                    superClassInit = superClassInit,
                    origin = origin))
        }
        KotlinPlatform.NATIVE -> emptyList()
    }
}

internal class EnumStubBuilder(
        override val context: StubsBuildingContext,
        private val enumDef: EnumDef
) : StubElementBuilder {

    private val classifier = (context.mirror(EnumType(enumDef)) as TypeMirror.ByValue).valueType.classifier
    private val baseTypeMirror = context.mirror(enumDef.baseType)
    private val baseType = baseTypeMirror.argType.toStubIrType()

    override fun build(): List<StubIrElement> {
        if (!context.isStrictEnum(enumDef)) {
            return generateEnumAsConstants(enumDef)
        }
        val constructorParameter = FunctionParameterStub("value", baseType)
        val valueProperty = PropertyStub(
                name = "value",
                type = baseType,
                kind = PropertyStub.Kind.Val(PropertyAccessor.Getter.GetConstructorParameter(constructorParameter)),
                modality = MemberStubModality.OPEN,
                origin = StubOrigin.Synthetic.EnumValueField(enumDef),
                isOverride = true)

        val canonicalsByValue = enumDef.constants
                .groupingBy { it.value }
                .reduce { _, accumulator, element ->
                    if (element.isMoreCanonicalThan(accumulator)) {
                        element
                    } else {
                        accumulator
                    }
                }
        val (canonicalConstants, aliasConstants) = enumDef.constants.partition { canonicalsByValue[it.value] == it }

        val canonicalEntriesWithAliases = canonicalConstants
                .sortedBy { it.value } // TODO: Is it stable enough?
                .mapIndexed { index, constant ->
                    val literal = context.tryCreateIntegralStub(enumDef.baseType, constant.value)
                            ?: error("Cannot create enum value ${constant.value} of type ${enumDef.baseType}")
                    val entry = EnumEntryStub(mangleSimple(constant.name), literal, StubOrigin.EnumEntry(constant), index)
                    val aliases = aliasConstants
                            .filter { it.value == constant.value }
                            .map { constructAliasProperty(it, entry) }
                    entry to aliases
                }
        val origin = StubOrigin.Enum(enumDef)
        val primaryConstructor = ConstructorStub(
                parameters = listOf(constructorParameter),
                annotations = emptyList(),
                isPrimary = true,
                origin = origin,
                visibility = VisibilityModifier.PRIVATE
        )

        val byValueFunction = FunctionStub(
                name = "byValue",
                returnType = ClassifierStubType(classifier),
                parameters = listOf(FunctionParameterStub("value", baseType)),
                origin = StubOrigin.Synthetic.EnumByValue(enumDef),
                receiver = null,
                modality = MemberStubModality.FINAL,
                annotations = listOf(AnnotationStub.Deprecated.deprecatedCEnumByValue)
        )

        val companion = ClassStub.Companion(
                classifier = classifier.nested("Companion"),
                properties = canonicalEntriesWithAliases.flatMap { it.second },
                methods = listOf(byValueFunction)
        )
        val enumVarClass = constructEnumVarClass().takeIf { context.generationMode == GenerationMode.METADATA }
        val kotlinEnumType = ClassifierStubType(Classifier.topLevel("kotlin", "Enum"),
                listOf(TypeArgumentStub(ClassifierStubType(classifier))))
        val enum = ClassStub.Enum(
                classifier = classifier,
                superClassInit = SuperClassInit(kotlinEnumType),
                entries = canonicalEntriesWithAliases.map { it.first },
                companion = companion,
                constructors = listOf(primaryConstructor),
                properties = listOf(valueProperty),
                origin = origin,
                interfaces = listOf(context.platform.getRuntimeType("CEnum")),
                childrenClasses = listOfNotNull(enumVarClass)
        )
        context.bridgeComponentsBuilder.enumToTypeMirror[enum] = baseTypeMirror
        return listOf(enum)
    }

    private fun constructAliasProperty(enumConstant: EnumConstant, entry: EnumEntryStub): PropertyStub {
        val aliasAnnotation = AnnotationStub.CEnumEntryAlias(entry.name)
                .takeIf { context.generationMode == GenerationMode.METADATA }
        return PropertyStub(
                enumConstant.name,
                ClassifierStubType(classifier),
                kind = PropertyStub.Kind.Val(PropertyAccessor.Getter.GetEnumEntry(entry)),
                origin = StubOrigin.EnumEntry(enumConstant),
                annotations = listOfNotNull(aliasAnnotation)
        )
    }

    private fun constructEnumVarClass(): ClassStub.Simple {

        val enumVarClassifier = classifier.nested("Var")

        val rawPtrConstructorParam = FunctionParameterStub("rawPtr", context.platform.getRuntimeType("NativePtr"))
        val superClass = context.platform.getRuntimeType("CEnumVar")
        require(superClass is ClassifierStubType)
        val primaryConstructor = ConstructorStub(
                parameters = listOf(rawPtrConstructorParam),
                isPrimary = true,
                annotations = emptyList(),
                origin = StubOrigin.Synthetic.DefaultConstructor
        )
        val superClassInit = SuperClassInit(superClass, listOf(GetConstructorParameter(rawPtrConstructorParam)))

        val baseIntegerTypeSize = when (val unwrappedType = enumDef.baseType.unwrapTypedefs()) {
            is IntegerType -> unwrappedType.size.toLong()
            CharType -> 1L
            else -> error("Incorrect base type for enum ${classifier.fqName}")
        }
        val typeSize = IntegralConstantStub(baseIntegerTypeSize, 4, true)
        val companionSuper = (context.platform.getRuntimeType("CPrimitiveVar") as ClassifierStubType).nested("Type")
        val varSizeAnnotation = AnnotationStub.CEnumVarTypeSize(baseIntegerTypeSize.toInt())
                .takeIf { context.generationMode == GenerationMode.METADATA }
        val companion = ClassStub.Companion(
                classifier = enumVarClassifier.nested("Companion"),
                superClassInit = SuperClassInit(companionSuper, listOf(typeSize)),
                annotations = listOfNotNull(varSizeAnnotation, AnnotationStub.Deprecated.deprecatedCVariableCompanion)
        )
        val valueProperty = PropertyStub(
                name = "value",
                type = ClassifierStubType(classifier),
                kind = PropertyStub.Kind.Var(
                        PropertyAccessor.Getter.ExternalGetter(),
                        PropertyAccessor.Setter.ExternalSetter()
                ),
                origin = StubOrigin.Synthetic.EnumVarValueField(enumDef)
        )
        return ClassStub.Simple(
                classifier = enumVarClassifier,
                constructors = listOf(primaryConstructor),
                superClassInit = superClassInit,
                companion = companion,
                modality = ClassStubModality.NONE,
                origin = StubOrigin.VarOf(StubOrigin.Enum(enumDef)),
                properties = listOf(valueProperty)
        )
    }

    private fun EnumConstant.isMoreCanonicalThan(other: EnumConstant): Boolean = with(other.name.toLowerCase()) {
        contains("min") || contains("max") ||
                contains("first") || contains("last") ||
                contains("begin") || contains("end")
    }

    /**
     * Produces to [out] the Kotlin definitions for given enum which shouldn't be represented as Kotlin enum.
     */
    private fun generateEnumAsConstants(enumDef: EnumDef): List<StubIrElement> {
        // TODO: if this enum defines e.g. a type of struct field, then it should be generated inside the struct class
        //  to prevent name clashing

        val entries = mutableListOf<PropertyStub>()
        val typealiases = mutableListOf<TypealiasStub>()

        val constants = enumDef.constants.filter {
            // Macro "overrides" the original enum constant.
            it.name !in context.macroConstantsByName
        }

        val kotlinType: KotlinType

        val baseKotlinType = context.mirror(enumDef.baseType).argType
        val meta = if (enumDef.isAnonymous) {
            kotlinType = baseKotlinType
            StubContainerMeta(textAtStart = if (constants.isNotEmpty()) "// ${enumDef.spelling}:" else "")
        } else {
            val typeMirror = context.mirror(EnumType(enumDef))
            if (typeMirror !is TypeMirror.ByValue) {
                error("unexpected enum type mirror: $typeMirror")
            }

            val varTypeName = typeMirror.info.constructPointedType(typeMirror.valueType)
            val varTypeClassifier = typeMirror.pointedType.classifier
            val valueTypeClassifier = typeMirror.valueType.classifier
            val origin = StubOrigin.Enum(enumDef)
            typealiases += TypealiasStub(varTypeClassifier, varTypeName.toStubIrType(), StubOrigin.VarOf(origin))
            typealiases += TypealiasStub(valueTypeClassifier, baseKotlinType.toStubIrType(), origin)

            kotlinType = typeMirror.valueType
            StubContainerMeta()
        }

        for (constant in constants) {
            val literal = context.tryCreateIntegralStub(enumDef.baseType, constant.value) ?: continue
            val kind = when (context.generationMode) {
                GenerationMode.SOURCE_CODE -> {
                    val getter = PropertyAccessor.Getter.SimpleGetter(constant = literal)
                    PropertyStub.Kind.Val(getter)
                }
                GenerationMode.METADATA -> {
                    PropertyStub.Kind.Constant(literal)
                }
            }
            entries += PropertyStub(
                    constant.name,
                    kotlinType.toStubIrType(),
                    kind,
                    MemberStubModality.FINAL,
                    null,
                    origin = StubOrigin.EnumEntry(constant)
            )
        }
        val container = SimpleStubContainer(
                meta,
                properties = entries.toList(),
                typealiases = typealiases.toList()
        )
        return listOf(container)
    }
}

internal class FunctionStubBuilder(
        override val context: StubsBuildingContext,
        private val func: FunctionDecl,
        private val skipOverloads: Boolean = false
) : StubElementBuilder {

    override fun build(): List<StubIrElement> {
        val platform = context.platform
        val parameters = mutableListOf<FunctionParameterStub>()

        var hasStableParameterNames = true
        func.parameters.forEachIndexed { index, parameter ->
            val parameterName = parameter.name.let {
                if (it == null || it.isEmpty()) {
                    hasStableParameterNames = false
                    "arg$index"
                } else {
                    it
                }
            }

            val representAsValuesRef = representCFunctionParameterAsValuesRef(parameter.type)
            parameters += when {
                representCFunctionParameterAsString(func, parameter.type) -> {
                    val annotations = when (platform) {
                        KotlinPlatform.JVM -> emptyList()
                        KotlinPlatform.NATIVE -> listOf(AnnotationStub.CCall.CString)
                    }
                    val type = KotlinTypes.string.makeNullable().toStubIrType()
                    val functionParameterStub = FunctionParameterStub(parameterName, type, annotations)
                    context.bridgeComponentsBuilder.cStringParameters += functionParameterStub
                    functionParameterStub
                }
                representCFunctionParameterAsWString(func, parameter.type) -> {
                    val annotations = when (platform) {
                        KotlinPlatform.JVM -> emptyList()
                        KotlinPlatform.NATIVE -> listOf(AnnotationStub.CCall.WCString)
                    }
                    val type = KotlinTypes.string.makeNullable().toStubIrType()
                    val functionParameterStub = FunctionParameterStub(parameterName, type, annotations)
                    context.bridgeComponentsBuilder.wCStringParameters += functionParameterStub
                    functionParameterStub
                }
                representAsValuesRef != null -> {
                    FunctionParameterStub(parameterName, representAsValuesRef.toStubIrType())
                }
                else -> {
                    val mirror = context.mirror(parameter.type)
                    val type = mirror.argType.toStubIrType()
                    FunctionParameterStub(parameterName, type)
                }
            }
        }

        val returnType = if (func.returnsVoid()) {
            KotlinTypes.unit
        } else {
            context.mirror(func.returnType).argType
        }.toStubIrType()

        if (skipOverloads && context.isOverloading(func))
            return emptyList()

        val annotations: List<AnnotationStub>
        val mustBeExternal: Boolean
        if (platform == KotlinPlatform.JVM) {
            annotations = emptyList()
            mustBeExternal = false
        } else {
            if (func.isVararg) {
                val type = KotlinTypes.any.makeNullable().toStubIrType()
                parameters += FunctionParameterStub("variadicArguments", type, isVararg = true)
            }
            annotations = listOf(AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${func.name}"))
            mustBeExternal = true
        }
        val functionStub = FunctionStub(
                func.name,
                returnType,
                parameters.toList(),
                StubOrigin.Function(func),
                annotations,
                mustBeExternal,
                null,
                MemberStubModality.FINAL,
                hasStableParameterNames = hasStableParameterNames
        )
        return listOf(functionStub)
    }


    private fun FunctionDecl.returnsVoid(): Boolean = this.returnType.unwrapTypedefs() is VoidType

    private fun representCFunctionParameterAsValuesRef(type: Type): KotlinType? {
        val pointeeType = when (type) {
            is PointerType -> type.pointeeType
            is ArrayType -> type.elemType
            else -> return null
        }

        val unwrappedPointeeType = pointeeType.unwrapTypedefs()

        if (unwrappedPointeeType is VoidType) {
            // Represent `void*` as `CValuesRef<*>?`:
            return KotlinTypes.cValuesRef.typeWith(StarProjection).makeNullable()
        }

        if (unwrappedPointeeType is FunctionType) {
            // Don't represent function pointer as `CValuesRef<T>?` currently:
            return null
        }

        if (unwrappedPointeeType is ArrayType) {
            return representCFunctionParameterAsValuesRef(pointeeType)
        }


        return KotlinTypes.cValuesRef.typeWith(context.mirror(pointeeType).pointedType).makeNullable()
    }


    private val platformWStringTypes = setOf("LPCWSTR")

    private val noStringConversion: Set<String>
        get() = context.configuration.noStringConversion

    private fun Type.isAliasOf(names: Set<String>): Boolean {
        var type = this
        while (type is Typedef) {
            if (names.contains(type.def.name)) return true
            type = type.def.aliased
        }
        return false
    }

    private fun representCFunctionParameterAsString(function: FunctionDecl, type: Type): Boolean {
        val unwrappedType = type.unwrapTypedefs()
        return unwrappedType is PointerType && unwrappedType.pointeeIsConst &&
                unwrappedType.pointeeType.unwrapTypedefs() == CharType &&
                !noStringConversion.contains(function.name)
    }

    // We take this approach as generic 'const short*' shall not be used as String.
    private fun representCFunctionParameterAsWString(function: FunctionDecl, type: Type) = type.isAliasOf(platformWStringTypes)
            && !noStringConversion.contains(function.name)
}

internal class GlobalStubBuilder(
        override val context: StubsBuildingContext,
        private val global: GlobalDecl
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val mirror = context.mirror(global.type)
        val unwrappedType = global.type.unwrapTypedefs()
        val origin = StubOrigin.Global(global)

        val kotlinType: KotlinType
        val kind: PropertyStub.Kind
        if (unwrappedType is ArrayType) {
            kotlinType = (mirror as TypeMirror.ByValue).valueType
            val getter = when (context.platform) {
                KotlinPlatform.JVM -> {
                    PropertyAccessor.Getter.SimpleGetter().also {
                        val extra = BridgeGenerationInfo(global.name, mirror.info)
                        context.bridgeComponentsBuilder.arrayGetterBridgeInfo[it] = extra
                    }
                }
                KotlinPlatform.NATIVE -> {
                    val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_getter")
                    PropertyAccessor.Getter.ExternalGetter(listOf(cCallAnnotation)).also {
                        context.wrapperComponentsBuilder.getterToWrapperInfo[it] = WrapperGenerationInfo(global)
                    }
                }
            }
            kind = PropertyStub.Kind.Val(getter)
        } else {
            when (mirror) {
                is TypeMirror.ByValue -> {
                    kotlinType = mirror.argType
                    val getter = when (context.platform) {
                        KotlinPlatform.JVM -> {
                            PropertyAccessor.Getter.SimpleGetter().also {
                                val getterExtra = BridgeGenerationInfo(global.name, mirror.info)
                                context.bridgeComponentsBuilder.getterToBridgeInfo[it] = getterExtra
                            }
                        }
                        KotlinPlatform.NATIVE -> {
                            val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_getter")
                            PropertyAccessor.Getter.ExternalGetter(listOf(cCallAnnotation)).also {
                                context.wrapperComponentsBuilder.getterToWrapperInfo[it] = WrapperGenerationInfo(global)
                            }
                        }
                    }
                    kind = if (global.isConst) {
                        PropertyStub.Kind.Val(getter)
                    } else {
                        val setter = when (context.platform) {
                            KotlinPlatform.JVM -> {
                                PropertyAccessor.Setter.SimpleSetter().also {
                                    val setterExtra = BridgeGenerationInfo(global.name, mirror.info)
                                    context.bridgeComponentsBuilder.setterToBridgeInfo[it] = setterExtra
                                }
                            }
                            KotlinPlatform.NATIVE -> {
                                val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_setter")
                                PropertyAccessor.Setter.ExternalSetter(listOf(cCallAnnotation)).also {
                                    context.wrapperComponentsBuilder.setterToWrapperInfo[it] = WrapperGenerationInfo(global)
                                }
                            }
                        }
                        PropertyStub.Kind.Var(getter, setter)
                    }
                }
                is TypeMirror.ByRef -> {
                    kotlinType = mirror.pointedType
                    val getter = when (context.generationMode) {
                        GenerationMode.SOURCE_CODE -> {
                            PropertyAccessor.Getter.InterpretPointed(global.name, kotlinType.toStubIrType())
                        }
                        GenerationMode.METADATA -> {
                            val cCallAnnotation = AnnotationStub.CCall.Symbol("${context.generateNextUniqueId("knifunptr_")}_${global.name}_getter")
                            PropertyAccessor.Getter.ExternalGetter(listOf(cCallAnnotation)).also {
                                context.wrapperComponentsBuilder.getterToWrapperInfo[it] = WrapperGenerationInfo(global, passViaPointer = true)
                            }
                        }
                    }
                    kind = PropertyStub.Kind.Val(getter)
                }
            }
        }
        return listOf(PropertyStub(global.name, kotlinType.toStubIrType(), kind, origin = origin))
    }
}

internal class TypedefStubBuilder(
        override val context: StubsBuildingContext,
        private val typedefDef: TypedefDef
) : StubElementBuilder {
    override fun build(): List<StubIrElement> {
        val mirror = context.mirror(Typedef(typedefDef))
        val baseMirror = context.mirror(typedefDef.aliased)
        val varType = mirror.pointedType.classifier
        val origin = StubOrigin.TypeDef(typedefDef)
        return when (baseMirror) {
            is TypeMirror.ByValue -> {
                val valueType = (mirror as TypeMirror.ByValue).valueType
                val varTypeAliasee = mirror.info.constructPointedType(valueType)
                val valueTypeAliasee = baseMirror.valueType
                listOf(
                        TypealiasStub(varType, varTypeAliasee.toStubIrType(), StubOrigin.VarOf(origin)),
                        TypealiasStub(valueType.classifier, valueTypeAliasee.toStubIrType(), origin)
                )
            }
            is TypeMirror.ByRef -> {
                val varTypeAliasee = baseMirror.pointedType
                listOf(TypealiasStub(varType, varTypeAliasee.toStubIrType(), origin))
            }
        }
    }
}
