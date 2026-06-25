/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.compileTimeValue
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLazilyBoundAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import kotlin.metadata.*
import kotlin.metadata.ClassKind as KmClassKind
import kotlin.metadata.Modality as KmModality
import kotlin.metadata.Visibility as KmVisibility

/**
 * An isolated component responsible for converting kotlinx-metadata entities representing
 * the metadata from an C-interop library into IR.
 */
class CInteropKlibMetadata2IRTransformer(
    private val symbolTable: SymbolTable,
    private val symbols: ExternalSymbols,
    private val declarationTracker: DeclarationTracker,
    private val getNestedKmClass: (ClassId) -> KmClass?,
    private val getOrCreateContainingPackageFragment: (kmDeclaration: Any) -> IrPackageFragment,
    private val getReferencedDeclarationSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
    private val irProviderForLazyAnnotations: IrProvider,
) {
    private val signatureComputer = PublicIdSignatureComputer(KonanManglerIr, markAllAsCInterop = true)

    fun transformTopLevelClass(kmClass: KmClass): IrClass {
        val irPackage = getOrCreateContainingPackageFragment(kmClass)
        val irClass = deserializeClass(kmClass, irPackage)
        irPackage.addChild(irClass)
        return irClass
    }

    fun transformTopLevelFunction(kmFunction: KmFunction): IrSimpleFunction {
        val irPackage = getOrCreateContainingPackageFragment(kmFunction)
        val irFunction = deserializeFunction(kmFunction, irPackage)
        irPackage.addChild(irFunction)
        computeSignatureAndRegisterInSymbolTable(irFunction)
        return irFunction
    }

    fun transformTopLevelProperty(kmProperty: KmProperty): IrProperty {
        val irPackage = getOrCreateContainingPackageFragment(kmProperty)
        val irProperty = deserializeProperty(kmProperty, irPackage)
        irPackage.addChild(irProperty)
        computeSignatureAndRegisterInSymbolTable(irProperty)
        return irProperty
    }

    private fun deserializeClass(kmClass: KmClass, parent: IrDeclarationParent): IrClass {
        require(kmClass.typeParameters.isEmpty()) { "Classes inside C-interop Klibs are not expected to have type parameters." }
        require(!kmClass.name.isLocalClassName()) { "Local/anonymous classes are not supported: ${kmClass.name}." }

        val classId = ClassId.fromString(kmClass.name)
        val signature = classId.toCInteropSignature(isCInterop = true)

        val clazz = symbolTable.declareClass(signature, { IrClassSymbolImpl(signature = signature) }) { symbol ->
            IrFactoryImpl.createClass(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                symbol = symbol,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = classId.shortClassName,
                visibility = kmClass.visibility.toDescriptorVisibility(),
                modality = kmClass.modality.toDescriptorModality(),
                kind = kmClass.kind.toDescriptorClassKind(),
                isCompanion = kmClass.kind == KmClassKind.COMPANION_OBJECT,
                isInner = kmClass.isInner,
                isExpect = kmClass.isExpect,
                isExternal = kmClass.isExternal,
                isValue = kmClass.isValue,
                isData = kmClass.isData,
                isFun = kmClass.isFunInterface,
                hasEnumEntries = kmClass.hasEnumEntries,
            ).also(declarationTracker::onNewClass)
        }

        clazz.annotations = kmClass.annotations.map { deserializeAnnotation(it) }
        clazz.superTypes = if (kmClass.supertypes.isNotEmpty()) {
            kmClass.supertypes.map { it.toIrType() }
        } else {
            listOf(symbols.anyType)
        }
        clazz.createThisReceiverParameter()
        clazz.parent = parent

        for (kmConstructor in kmClass.constructors) {
            clazz.declarations += deserializeConstructor(kmConstructor, clazz)
        }
        for (kmProperty in kmClass.properties) {
            clazz.declarations += deserializeProperty(kmProperty, clazz)
        }
        for (kmFunction in kmClass.functions) {
            clazz.declarations += deserializeFunction(kmFunction, clazz)
        }
        for (enumEntry in kmClass.kmEnumEntries) {
            clazz.declarations += deserializeEnumEntry(enumEntry, clazz, signature)
        }
        for (nestedClassName in kmClass.nestedClasses) {
            val nestedClassId = classId.createNestedClassId(Name.identifier(nestedClassName))
            val nestedKmClass = getNestedKmClass(nestedClassId) ?: continue
            clazz.declarations += deserializeClass(nestedKmClass, clazz)
        }

        if (clazz.inheritsFromCEnum()) {
            val members = generateSpecialEnumMembers(clazz)
            clazz.declarations += members
            for (member in members) {
                member.patchDeclarationParents(clazz)
            }
        }

        // Computing a signature sometimes depends on sibling members in the class, so it has to be done after all the members are created.
        for (member in clazz.declarations) {
            computeSignatureAndRegisterInSymbolTable(member as IrDeclarationWithName)
        }

        return clazz
    }

    private fun deserializeConstructor(kmConstructor: KmConstructor, parent: IrClass): IrConstructor {
        val constructor = IrFactoryImpl.createConstructorWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = SpecialNames.INIT,
            visibility = kmConstructor.visibility.toDescriptorVisibility(),
            returnType = parent.defaultType,
            isExpect = false,
            isExternal = false,
            isInline = false,
            isPrimary = !kmConstructor.isSecondary,
        )
        constructor.parameters = kmConstructor.valueParameters.map { deserializeRegularParameter(it, constructor, emptyMap()) }
        constructor.parameters.forEach { it.parent = constructor }

        constructor.annotations = kmConstructor.annotations.map { deserializeAnnotation(it) }

        constructor.parent = parent
        return constructor
    }

    private fun deserializeFunction(kmFunction: KmFunction, parent: IrDeclarationParent): IrSimpleFunction {
        val typeParametersById = deserializeTypeParameters(kmFunction.typeParameters)
        val function = IrFactoryImpl.createFunctionWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(kmFunction.name),
            visibility = kmFunction.visibility.toDescriptorVisibility(),
            modality = kmFunction.modality.toDescriptorModality(),
            returnType = kmFunction.returnType.toIrType(typeParametersById),
            isExpect = kmFunction.isExpect,
            isInfix = kmFunction.isInfix,
            isExternal = kmFunction.isExternal,
            isInline = kmFunction.isInline,
            isTailrec = kmFunction.isTailrec,
            isSuspend = kmFunction.isSuspend,
            isOperator = kmFunction.isOperator,
        )
        function.parameters = buildList {
            if (parent is IrClass) {
                addIfNotNull(parent.thisReceiver?.copyTo(function))
            }
            kmFunction.receiverParameterType?.let {
                this += createExtensionReceiverParameter(
                    type = it.toIrType(typeParametersById),
                    kmAnnotations = kmFunction.extensionReceiverParameterAnnotations,
                    parent = function
                )
            }
            kmFunction.valueParameters.mapTo(this) { deserializeRegularParameter(it, function, typeParametersById) }

            @OptIn(ExperimentalContextParameters::class)
            require(kmFunction.contextParameters.isEmpty()) { "Context parameters are not expected" }
        }
        function.parameters.forEach { it.parent = function }

        function.typeParameters = typeParametersById.values.sortedBy { it.index }
        function.typeParameters.forEach { it.parent = function }

        function.annotations = kmFunction.annotations.map { deserializeAnnotation(it) }

        function.parent = parent
        return function
    }

    private fun deserializeProperty(kmProperty: KmProperty, parent: IrDeclarationParent): IrProperty {
        require(kmProperty.typeParameters.isEmpty()) { "Properties inside C-interop Klibs are not expected to have type parameters." }
        val property = IrFactoryImpl.createPropertyWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = Name.identifier(kmProperty.name),
            visibility = kmProperty.visibility.toDescriptorVisibility(),
            modality = kmProperty.modality.toDescriptorModality(),
            isExpect = kmProperty.isExpect,
            isExternal = kmProperty.isExternal,
            isVar = kmProperty.isVar,
            isConst = kmProperty.isConst,
            isLateinit = kmProperty.isLateinit,
            isDelegated = kmProperty.isDelegated,
        )
        property.getter = deserializeAccessor(kmProperty.getter, false, kmProperty, parent)
        property.getter?.parent = parent
        property.setter = kmProperty.setter?.let { deserializeAccessor(it, true, kmProperty, parent) }
        property.setter?.parent = parent

        kmProperty.compileTimeValue?.let { kmValue ->
            val field = IrFactoryImpl.createField(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = Name.identifier(kmProperty.name),
                visibility = kmProperty.visibility.toDescriptorVisibility(),
                symbol = IrFieldSymbolImpl(),
                type = kmProperty.returnType.toIrType(),
                isFinal = !kmProperty.isVar,
                isStatic = property.getter?.isStatic == true,
                isExternal = kmProperty.isExternal
            ).apply {
                val irValue = deserializeAnnotationArgument(kmValue)
                initializer = IrFactoryImpl.createExpressionBody(irValue)
            }
            property.backingField = field

            property.getter?.let { getter ->
                val getField = IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, field.type)
                getter.body = IrFactoryImpl.createExpressionBody(getField)
            }
        }

        property.annotations = kmProperty.annotations.map { deserializeAnnotation(it) }

        property.parent = parent
        return property
    }

    private fun deserializeAccessor(
        kmAccessor: KmPropertyAccessorAttributes,
        isSetter: Boolean,
        kmProperty: KmProperty,
        parent: IrDeclarationParent,
    ): IrSimpleFunction {
        val propertyType = kmProperty.returnType.toIrType()
        val accessor = IrFactoryImpl.createFunctionWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = if (isSetter) Name.special("<set-${kmProperty.name}>") else Name.special("<get-${kmProperty.name}>"),
            visibility = kmAccessor.visibility.toDescriptorVisibility(),
            modality = kmAccessor.modality.toDescriptorModality(),
            returnType = if (isSetter) symbols.unitType else propertyType,
            isExpect = kmProperty.isExpect,
            isInfix = false,
            isExternal = kmAccessor.isExternal,
            isInline = kmAccessor.isInline,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
        )
        accessor.parameters = buildList {
            if (parent is IrClass) {
                addIfNotNull(parent.thisReceiver?.copyTo(accessor))
            }
            kmProperty.receiverParameterType?.let {
                this += createExtensionReceiverParameter(
                    type = it.toIrType(),
                    kmAnnotations = kmProperty.extensionReceiverParameterAnnotations, parent = accessor
                )
            }
            if (isSetter) {
                this += IrFactoryImpl.createValueParameter(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    symbol = IrValueParameterSymbolImpl(),
                    name = Name.identifier("value"),
                    kind = IrParameterKind.Regular,
                    type = propertyType,
                    varargElementType = null,
                    isAssignable = false,
                    isCrossinline = false,
                    isNoinline = false,
                    isHidden = false,
                )
            }
        }
        accessor.parameters.forEach { it.parent = accessor }

        accessor.annotations = kmAccessor.annotations.map { deserializeAnnotation(it) }

        accessor.parent = parent
        return accessor
    }

    private fun deserializeRegularParameter(
        kmParameter: KmValueParameter,
        parent: IrFunction,
        typeParametersInScope: Map<Int, IrTypeParameter>,
    ): IrValueParameter {
        val parameter = IrFactoryImpl.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            symbol = IrValueParameterSymbolImpl(),
            name = Name.identifier(kmParameter.name),
            kind = IrParameterKind.Regular,
            type = kmParameter.type.toIrType(typeParametersInScope),
            varargElementType = kmParameter.varargElementType?.toIrType(typeParametersInScope),
            isAssignable = false,
            isCrossinline = kmParameter.isCrossinline,
            isNoinline = kmParameter.isNoinline,
            isHidden = false,
        )
        if (kmParameter.declaresDefaultValue) {
            parameter.defaultValue = parameter.createStubDefaultValue()
        }
        parameter.annotations = kmParameter.annotations.map { deserializeAnnotation(it) }

        parameter.parent = parent
        return parameter
    }

    private fun createExtensionReceiverParameter(type: IrType, kmAnnotations: List<KmAnnotation>, parent: IrFunction): IrValueParameter {
        val parameter = IrFactoryImpl.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
            name = SpecialNames.RECEIVER,
            kind = IrParameterKind.ExtensionReceiver,
            type = type,
            symbol = IrValueParameterSymbolImpl(),
            isAssignable = false,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )
        parameter.annotations = kmAnnotations.map { deserializeAnnotation(it) }

        parameter.parent = parent
        return parameter
    }

    private fun deserializeEnumEntry(kmEnumEntry: KmEnumEntry, parent: IrClass, parentSignature: IdSignature.CommonSignature): IrEnumEntry {
        val signature = IdSignature.CommonSignature(
            parentSignature.packageFqName,
            parentSignature.declarationFqName + "." + kmEnumEntry.name,
            null, parentSignature.mask, null
        )
        val enumEntry = symbolTable.declareEnumEntry(signature, { IrEnumEntrySymbolImpl(signature = signature) }) { symbol ->
            IrFactoryImpl.createEnumEntry(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = Name.identifier(kmEnumEntry.name),
                symbol = symbol,
            ).also(declarationTracker::onNewEnumEntry)
        }

        enumEntry.annotations = kmEnumEntry.annotations.map { deserializeAnnotation(it) }

        enumEntry.parent = parent
        return enumEntry
    }

    private fun generateSpecialEnumMembers(enumClass: IrClass): List<IrDeclarationWithName> = buildList {
        this += IrFactoryImpl.createFunctionWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER,
            name = StandardNames.ENUM_VALUES,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            returnType = symbols.arrayClass.typeWith(enumClass.defaultType),
            isExpect = false,
            isInfix = false,
            isExternal = false,
            isInline = false,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
        ).apply {
            body = IrSyntheticBodyImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrSyntheticBodyKind.ENUM_VALUES)
        }

        this += IrFactoryImpl.createFunctionWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER,
            name = StandardNames.ENUM_VALUE_OF,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            returnType = enumClass.defaultType,
            isExpect = false,
            isInfix = false,
            isExternal = false,
            isInline = false,
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
        ).apply {
            addValueParameter {
                name = Name.identifier("value")
                type = symbols.stringType
            }
            body = IrSyntheticBodyImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrSyntheticBodyKind.ENUM_VALUEOF)
        }

        this += IrFactoryImpl.createPropertyWithLateBinding(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER,
            name = StandardNames.ENUM_ENTRIES,
            visibility = DescriptorVisibilities.PUBLIC,
            modality = Modality.FINAL,
            isExpect = false,
            isExternal = false,
            isConst = false,
            isLateinit = false,
            isVar = false,
            isDelegated = false,
        ).also { property ->
            property.getter = IrFactoryImpl.createFunctionWithLateBinding(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER,
                name = Name.special("<get-${StandardNames.ENUM_ENTRIES}>"),
                visibility = DescriptorVisibilities.PUBLIC,
                modality = Modality.FINAL,
                returnType = symbols.enumEntriesInterfaceClass.typeWith(enumClass.defaultType),
                isExpect = false,
                isInfix = false,
                isExternal = false,
                isInline = false,
                isTailrec = false,
                isSuspend = false,
                isOperator = false,
            ).apply {
                body = IrSyntheticBodyImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrSyntheticBodyKind.ENUM_ENTRIES)
            }
        }
    }

    private fun deserializeTypeParameters(kmParameters: List<KmTypeParameter>): Map<Int, IrTypeParameter> {
        val kmToIrParam = kmParameters.withIndex().associate { [index, kmParameter] ->
            kmParameter to IrFactoryImpl.createTypeParameter(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                symbol = IrTypeParameterSymbolImpl(),
                name = Name.identifier(kmParameter.name),
                isReified = kmParameter.isReified,
                variance = kmParameter.variance.toIrVariance(),
                index = index,
            )
        }

        val typeParamsById = kmToIrParam.mapKeys { it.key.id }
        for ([kmParameter, irParameter] in kmToIrParam) {
            irParameter.superTypes = kmParameter.upperBounds.map { it.toIrType(typeParamsById) }
        }

        return typeParamsById
    }

    private fun KmType.toIrType(typeParametersInScope: Map<Int, IrTypeParameter> = emptyMap()): IrType {
        require(flexibleTypeUpperBound == null) { "Flexible types are not supported in K/Native." }

        val classifier = findReferencedClassifier(classifier, typeParametersInScope)
        return IrSimpleTypeImpl(
            classifier = classifier,
            nullability = if (isNullable) SimpleTypeNullability.MARKED_NULLABLE else SimpleTypeNullability.DEFINITELY_NOT_NULL,
            arguments = arguments.map { it.toIrTypeArgument(typeParametersInScope) },
            annotations = annotations.map { deserializeAnnotation(it) },
        )
    }

    private fun KmTypeProjection.toIrTypeArgument(typeParametersInScope: Map<Int, IrTypeParameter>): IrTypeArgument = when (this) {
        KmTypeProjection.STAR -> IrStarProjectionImpl
        else -> makeTypeProjection(type!!.toIrType(typeParametersInScope), variance!!.toIrVariance())
    }

    private fun deserializeAnnotation(kmAnnotation: KmAnnotation): IrAnnotation {
        val annotationClassSymbol = findReferencedClass(kmAnnotation.className)
        val irArguments = kmAnnotation.arguments.entries.associate {
            Name.identifier(it.key) to deserializeAnnotationArgument(it.value)
        }
        // Note: Stdlib does not define any annotation class with type parameters, so type arguments may be left empty.
        return IrLazilyBoundAnnotationImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            origin = null,
            source = SourceElement.NO_SOURCE,
            type = annotationClassSymbol.defaultTypeWithoutArguments,
            constructorTypeArgumentsCount = 0,
            classSymbol = annotationClassSymbol,
            argumentMapping = irArguments,
            linker = irProviderForLazyAnnotations,
        )
    }

    private fun findReferencedClass(className: ClassName): IrClassSymbol {
        require(!className.isLocalClassName()) { "Local/anonymous classes are not supported: $className" }

        val classId = ClassId.fromString(className)

        // A C-interop Klib may only reference classes from the Kotlin stdlib, itself, or other C-interop Klibs.
        // Additionally, interop Klibs may reference special "forward declared" classes, which are not physically present in
        // any Klib. We also know that:
        // - The classes from Stdlib that could be referenced are in `kotlin` and `kotlinx.cinterop` packages.
        // - Forward declared classes are designated by one of the predefined packages (`cnames` and `objcnames`).
        // - Other Klibs are not expected to define any of those packages (see also KT-85765, KT-86193).
        // We can use all of that to infer whether a referenced class comes from Kolin code (the Stdlib), forward declarations,
        // or otherwise, from C-interop Klib. This information is necessary to construct a proper IdSignature.
        val isInteropClass = !classId.packageFqName.isDefinedInStdlib() && !classId.packageFqName.isPackageOfForwardDeclaration()
        val classSignature = classId.toCInteropSignature(isCInterop = isInteropClass)

        return getReferencedDeclarationSymbol(classSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL) as IrClassSymbol
    }

    private fun deserializeAnnotationArgument(kmArgument: KmAnnotationArgument): IrExpression {
        return when (kmArgument) {
            is KmAnnotationArgument.ByteValue -> IrConstImpl.byte(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.byteType, kmArgument.value)
            is KmAnnotationArgument.ShortValue -> IrConstImpl.short(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.shortType, kmArgument.value)
            is KmAnnotationArgument.IntValue -> IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.intType, kmArgument.value)
            is KmAnnotationArgument.LongValue -> IrConstImpl.long(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.longType, kmArgument.value)
            is KmAnnotationArgument.UByteValue -> IrConstImpl.byte(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.ubyteType, kmArgument.value.toByte())
            is KmAnnotationArgument.UShortValue -> IrConstImpl.short(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.ushortType, kmArgument.value.toShort())
            is KmAnnotationArgument.UIntValue -> IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.uintType, kmArgument.value.toInt())
            is KmAnnotationArgument.ULongValue -> IrConstImpl.long(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.ulongType, kmArgument.value.toLong())
            is KmAnnotationArgument.FloatValue -> IrConstImpl.float(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.floatType, kmArgument.value)
            is KmAnnotationArgument.DoubleValue -> IrConstImpl.double(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.doubleType, kmArgument.value)
            is KmAnnotationArgument.CharValue -> IrConstImpl.char(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.charType, kmArgument.value)
            is KmAnnotationArgument.BooleanValue -> IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.booleanType, kmArgument.value)
            is KmAnnotationArgument.StringValue -> IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.stringType, kmArgument.value)
            is KmAnnotationArgument.AnnotationValue -> deserializeAnnotation(kmArgument.annotation)
            is KmAnnotationArgument.KClassValue -> {
                val classSymbol = findReferencedClass(kmArgument.className)
                IrClassReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbols.kClassClass.starProjectedType, classSymbol, classSymbol.defaultTypeWithoutArguments)
            }
            is KmAnnotationArgument.ArrayKClassValue -> TODO("Unsupported annotation argument kind used inside C-interop Klib: Array class reference")
            is KmAnnotationArgument.EnumValue -> {
                val enumClassId = ClassId.fromString(kmArgument.enumClassName)
                val enumEntryId = enumClassId.createNestedClassId(Name.identifier(kmArgument.enumEntryName))
                val enumEntrySig = enumEntryId.toIdSignature()
                val enumEntrySymbol = getReferencedDeclarationSymbol(enumEntrySig, BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL) as IrEnumEntrySymbol
                val enumClassSymbol = findReferencedClass(kmArgument.enumClassName)
                val irType = enumClassSymbol.defaultTypeWithoutArguments
                IrGetEnumValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irType, enumEntrySymbol)
            }
            is KmAnnotationArgument.ArrayValue -> {
                val elements = kmArgument.elements.map { deserializeAnnotationArgument(it) }
                val varargElementType = elements.mapToSetOrEmpty { it.type }.singleOrNull() ?: symbols.anyType
                val arrayType = if (varargElementType.isPrimitiveType()) {
                    val classId = ClassId.topLevel(varargElementType.classifierOrFail.fqNameWhenAvailable!!)
                    val arrayClassId = StandardClassIds.primitiveArrayTypeByElementType[classId]!!
                    symbolTable.referenceClass(arrayClassId.toIdSignature()).defaultTypeWithoutArguments
                } else symbols.arrayClass.typeWith(varargElementType)
                IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, varargElementType, elements)
            }
        }
    }

    private fun computeSignatureAndRegisterInSymbolTable(declaration: IrDeclarationWithName) {
        if (declaration is IrClass || declaration is IrEnumEntry) {
            // Classes and enum entries have simple signatures, which may be computed right upon their creation.
            // Having the signature, they are also declared in a symbol table right away. So nothing to do here.
            return
        }

        val signature = signatureComputer.computeSignature(declaration)
        when (declaration) {
            is IrFunctionWithLateBinding -> symbolTable.declareSimpleFunction(
                signature = signature,
                symbolFactory = { IrSimpleFunctionSymbolImpl(signature = signature) },
                functionFactory = { declaration.acquireSymbol(it).also(declarationTracker::onNewFunction) }
            )
            is IrConstructorWithLateBinding -> symbolTable.declareConstructor(
                signature = signature,
                symbolFactory = { IrConstructorSymbolImpl(signature = signature) },
                constructorFactory = { declaration.acquireSymbol(it).also(declarationTracker::onNewClassConstructor) }
            )
            is IrPropertyWithLateBinding -> {
                symbolTable.declareProperty(
                    signature = signature,
                    symbolFactory = { IrPropertySymbolImpl(signature = signature) },
                    propertyFactory = { declaration.acquireSymbol(it).also(declarationTracker::onNewProperty) }
                )

                declaration.getter?.let(::computeSignatureAndRegisterInSymbolTable)
                declaration.setter?.let(::computeSignatureAndRegisterInSymbolTable)
            }
            else -> error("Unexpected declaration kind: ${declaration::class.simpleName}")
        }
    }

    private fun findReferencedClassifier(
        classifier: KmClassifier,
        typeParametersInScope: Map<Int, IrTypeParameter> = emptyMap(),
    ): IrClassifierSymbol {
        return when (classifier) {
            is KmClassifier.TypeParameter -> typeParametersInScope[classifier.id]?.symbol
                ?: error("No type parameter with id ${classifier.id} found in the current scope.")
            is KmClassifier.Class -> findReferencedClass(classifier.name)
            is KmClassifier.TypeAlias -> error(
                "Unexpected type alias reference.\n" +
                        "All types in metadata are expected to be expanded, except in KmType.abbreviatedType, " +
                        "however, it should be ignored, as IR does not use type abbreviations."
            )
        }
    }

    class ExternalSymbols(symbolTable: ReferenceSymbolTable) {
        private val anyClass = symbolTable.referenceClass(StandardClassIds.Any.toIdSignature())
        private val unitClass = symbolTable.referenceClass(StandardClassIds.Unit.toIdSignature())
        private val booleanClass = symbolTable.referenceClass(StandardClassIds.Boolean.toIdSignature())
        private val charClass = symbolTable.referenceClass(StandardClassIds.Char.toIdSignature())
        private val byteClass = symbolTable.referenceClass(StandardClassIds.Byte.toIdSignature())
        private val shortClass = symbolTable.referenceClass(StandardClassIds.Short.toIdSignature())
        private val intClass = symbolTable.referenceClass(StandardClassIds.Int.toIdSignature())
        private val longClass = symbolTable.referenceClass(StandardClassIds.Long.toIdSignature())
        private val ubyteClass = symbolTable.referenceClass(StandardClassIds.UByte.toIdSignature())
        private val ushortClass = symbolTable.referenceClass(StandardClassIds.UShort.toIdSignature())
        private val uintClass = symbolTable.referenceClass(StandardClassIds.UInt.toIdSignature())
        private val ulongClass = symbolTable.referenceClass(StandardClassIds.ULong.toIdSignature())
        private val floatClass = symbolTable.referenceClass(StandardClassIds.Float.toIdSignature())
        private val doubleClass = symbolTable.referenceClass(StandardClassIds.Double.toIdSignature())
        private val stringClass = symbolTable.referenceClass(StandardClassIds.String.toIdSignature())

        val arrayClass = symbolTable.referenceClass(StandardClassIds.Array.toIdSignature())
        val kClassClass = symbolTable.referenceClass(StandardClassIds.KClass.toIdSignature())
        val enumEntriesInterfaceClass = symbolTable.referenceClass(ClassId(FqName("kotlin.enums"), Name.identifier("EnumEntries")).toIdSignature())

        val anyType = anyClass.defaultTypeWithoutArguments
        val unitType = unitClass.defaultTypeWithoutArguments
        val booleanType = booleanClass.defaultTypeWithoutArguments
        val charType = charClass.defaultTypeWithoutArguments
        val byteType = byteClass.defaultTypeWithoutArguments
        val shortType = shortClass.defaultTypeWithoutArguments
        val intType = intClass.defaultTypeWithoutArguments
        val longType = longClass.defaultTypeWithoutArguments
        val ubyteType = ubyteClass.defaultTypeWithoutArguments
        val ushortType = ushortClass.defaultTypeWithoutArguments
        val uintType = uintClass.defaultTypeWithoutArguments
        val ulongType = ulongClass.defaultTypeWithoutArguments
        val floatType = floatClass.defaultTypeWithoutArguments
        val doubleType = doubleClass.defaultTypeWithoutArguments
        val stringType = stringClass.defaultTypeWithoutArguments
    }

    /** Used to track the declarations that were deserialized during conversion from kotlinx-metadata to IR. */
    open class DeclarationTracker {
        /** The cache of the deserialized declarations. */
        val deserializedDeclarations: Map<IdSignature, IrDeclaration>
            field = mutableMapOf<IdSignature, IrDeclaration>()

        open fun onNewClass(clazz: IrClass) {
            deserializedDeclarations[clazz.symbol.signature!!] = clazz
        }

        fun onNewEnumEntry(enumEntry: IrEnumEntry) {
            deserializedDeclarations[enumEntry.symbol.signature!!] = enumEntry
        }

        fun onNewFunction(function: IrSimpleFunction) {
            deserializedDeclarations[function.symbol.signature!!] = function
        }

        fun onNewProperty(property: IrProperty) {
            deserializedDeclarations[property.symbol.signature!!] = property
        }

        fun onNewClassConstructor(constructor: IrConstructor) {
            deserializedDeclarations[constructor.symbol.signature!!] = constructor
        }
    }

    companion object {
        private fun KmVisibility.toDescriptorVisibility(): DescriptorVisibility = when (this) {
            KmVisibility.PUBLIC -> DescriptorVisibilities.PUBLIC
            KmVisibility.INTERNAL -> DescriptorVisibilities.INTERNAL
            KmVisibility.PROTECTED -> DescriptorVisibilities.PROTECTED
            KmVisibility.PRIVATE -> DescriptorVisibilities.PRIVATE
            KmVisibility.PRIVATE_TO_THIS -> DescriptorVisibilities.PRIVATE_TO_THIS
            KmVisibility.LOCAL -> DescriptorVisibilities.LOCAL
        }

        private fun KmModality.toDescriptorModality(): Modality = when (this) {
            KmModality.FINAL -> Modality.FINAL
            KmModality.OPEN -> Modality.OPEN
            KmModality.ABSTRACT -> Modality.ABSTRACT
            KmModality.SEALED -> Modality.SEALED
        }

        private fun KmClassKind.toDescriptorClassKind(): ClassKind = when (this) {
            KmClassKind.CLASS -> ClassKind.CLASS
            KmClassKind.INTERFACE -> ClassKind.INTERFACE
            KmClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
            KmClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KmClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
            KmClassKind.OBJECT -> ClassKind.OBJECT
            KmClassKind.COMPANION_OBJECT -> ClassKind.OBJECT
        }

        private fun KmVariance.toIrVariance(): Variance = when (this) {
            KmVariance.INVARIANT -> Variance.INVARIANT
            KmVariance.IN -> Variance.INVARIANT
            KmVariance.OUT -> Variance.OUT_VARIANCE
        }

        private fun FqName.isDefinedInStdlib(): Boolean =
            isSubpackageOf(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) || isSubpackageOf(NativeStandardInteropNames.cInteropPackage)

        private fun FqName.isPackageOfForwardDeclaration(): Boolean =
            this in NativeForwardDeclarationKind.packageFqNameToKind

        private fun ClassId.toCInteropSignature(isCInterop: Boolean) = IdSignature.CommonSignature(
            packageFqName = packageFqName.asString(),
            declarationFqName = relativeClassName.asString(),
            id = null,
            mask = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(isCInterop),
            description = null,
        )
    }
}
