/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.backend.common.linkage.IrDeserializer.TopLevelSymbolKind
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
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
import org.jetbrains.kotlin.library.KLIB_PROPERTY_PACKAGE
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.library.metadataVersion
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.utils.putToMultiMap
import java.lang.ref.SoftReference
import kotlin.metadata.*
import kotlin.metadata.ClassKind as KmClassKind
import kotlin.metadata.Modality as KmModality
import kotlin.metadata.Visibility as KmVisibility

/**
 * IR deserializer for C-interop Klibs.
 *
 * Note that interop Klibs do not contain IR, only metadata. But because both are structurally quite similar, this deserializer is able to
 * read metadata and directly convert it to IR, without reaching out help of many other compiler subsystems like frontend or descriptors.
 *
 * It supports only those metadata constructs which are expected to be present in C-interop Klibs. However, in practice, it's almost all of
 * them (some notable exceptions are context parameters and classes with type parameters). So in theory, it is not far away from a
 * general-purpose metadata-to-IR deserializer/converter.
 *
 * It returns regular (non-lazy), body-less IR, with a top-level-class grauallity (i.e. even if one class member is referenced, it
 * deserializes the entire top-level class along with its nested classes).
 */
internal class KonanInteropModuleDeserializer(
        private val deserializationConfiguration: DeserializationConfiguration,
        moduleDescriptor: ModuleDescriptor,
        override val klib: KotlinLibrary,
        private val isLibraryCached: Boolean,
        private val symbols: BackendNativeSymbols,
        private val linker: KonanIrLinker,
) : IrModuleDeserializer(moduleDescriptor, klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT) {
    init {
        require(klib.isCInteropLibrary())
    }

    private val symbolTable = linker.symbolTable
    private val builtIns = linker.builtIns
    private val signatureComputer = PublicIdSignatureComputer(KonanManglerIr, markAllAsCInterop = true)
    private val metadataReader = KlibMetadataReader(klib)

    // Interop Klibs may declare only one package, and its FQ name is declared in the manifest.
    private val definedPackageFqName: FqName = klib.manifestProperties.getProperty(KLIB_PROPERTY_PACKAGE)?.let(::FqName)
            ?: error("Interop klib ${klib.location} does not contain an expected manifest property: $KLIB_PROPERTY_PACKAGE")
    private val moduleHeaderProto: KlibMetadataProtoBuf.Header by lazy { parseModuleHeader(klib.metadata.moduleHeaderData) }

    override val kind get() = IrModuleDeserializerKind.DESERIALIZED
    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)
    private var externalIrPackageFragment: IrExternalPackageFragment? = null
    private var typeDefinitionsIrFile: IrFile? = null

    private fun IdSignature.isInteropSignature() = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

    override fun contains(idSig: IdSignature): Boolean {
        if (!idSig.isInteropSignature()) {
            return false
        }

        val commonSignature = ((idSig as? IdSignature.AccessorSignature)?.propertySignature ?: idSig)
                as? IdSignature.CommonSignature ?: return false
        val packageFqName = FqName(commonSignature.packageFqName)
        if (packageFqName != definedPackageFqName) {
            return false
        }

        // First, check for the presence of a top-level class. We assume that if it exists, all its members should also exist.
        // Note: Along classes, C-interop Klibs also define type aliases. However, all types in IR and metadata already provide their
        // expanded representation, and type aliases are not otherwise useful in IR, so there is no need to deserialize them.
        val topLevelName = FqName(commonSignature.declarationFqName.substringBefore('.'))
        val topLevelClassId = MetadataDeclarationId(TopLevelSymbolKind.CLASS_SYMBOL, packageFqName, topLevelName)
        if (topLevelClassId in metadataReader.getDeclaredDeclarationIds()) {
            return true
        }

        if ('.' !in commonSignature.declarationFqName) {
            // If no top-level class is found for a given FQ name, there may also exist such a top-level function or property.
            // Unfortunately, for them, there is no quick way to tell if an interop Klib contains one with a given signature, because
            // metadata does not store IdSignatures. It's necessary to invoke the actual deserialization, which will compute the signatures
            // on the fly, then match against the requested one.
            return tryDeserializeIrSymbol(idSig, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)?.isBound == true ||
                    tryDeserializeIrSymbol(idSig, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL)?.isBound == true
        }
        return false
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        val symbol = symbolTable.referenceSymbolByKind(idSig, symbolKind) ?: return null
        if (symbol.isBound) {
            return symbol
        }

        var searchForSymbolKind = symbolKind
        var commonSig = idSig
        if (commonSig is IdSignature.AccessorSignature) {
            // When looking for a property's accessor, try to deserialize the property instead.
            // Doing so will, in turn, deserialize its accessors.
            commonSig = commonSig.propertySignature
            searchForSymbolKind = BinarySymbolData.SymbolKind.PROPERTY_SYMBOL
        }
        commonSig = commonSig as? IdSignature.CommonSignature ?: return null

        if ('.' in commonSig.declarationFqName) {
            // When looking for a class member, try to deserialize the (top-most) containing class instead.
            // Doing so will, in turn, deserialize everything declared inside that class (including nested classes, recursively).
            // If the sought declaration is indeed defined somewhere inside this class, it will be linked to `symbol` in the process.
            val topLevelClassSig = commonSig.topLevelSignature()
            tryDeserializeIrSymbol(topLevelClassSig, BinarySymbolData.SymbolKind.CLASS_SYMBOL)
        } else {
            val packageFqName = FqName(commonSig.packageFqName)
            val relativeDeclarationName = FqName(commonSig.declarationFqName)
            val declarationKind = when (searchForSymbolKind) {
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> TopLevelSymbolKind.CLASS_SYMBOL
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> TopLevelSymbolKind.FUNCTION_SYMBOL
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> TopLevelSymbolKind.PROPERTY_SYMBOL
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL,
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> error("This declaration cannot be top-level: $searchForSymbolKind")
                else -> error("Symbol kind is unsupported by C-interop Klib: $searchForSymbolKind")
            }
            val id = MetadataDeclarationId(declarationKind, packageFqName, relativeDeclarationName)

            val kmDeclarations = metadataReader.retrieveDeclarationsById(id)
            if (kmDeclarations != null) {
                for (kmDeclaration in kmDeclarations) {
                    val irPackage = getOrCreateContainingPackageFragment(kmDeclaration)
                    val irDeclaration = when (kmDeclaration) {
                        is KmClass -> deserializeClass(kmDeclaration, irPackage)
                        is KmFunction -> deserializeFunction(kmDeclaration, irPackage)
                        is KmProperty -> deserializeProperty(kmDeclaration, irPackage)
                        else -> error(kmDeclaration.javaClass.name)
                    }
                    irPackage.addChild(irDeclaration)
                    computeSignatureAndRegisterInSymbolTable(irDeclaration)
                }
            }
        }

        return symbol
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No C-Interop symbol found for $idSig")

    private fun SymbolTable.referenceSymbolByKind(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        return when (symbolKind) {
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClass(idSig)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructor(idSig)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(idSig)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(idSig)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntry(idSig)
            else -> null
        }
    }

    private fun computeSignatureAndRegisterInSymbolTable(declaration: IrDeclarationWithName) {
        if (declaration is IrClass) {
            // Classes have signatures and are declared right upon creation, nothing to do here.
            return
        }

        val signature = signatureComputer.computeSignature(declaration)
        when (declaration) {
            is IrSimpleFunction -> {
                val newSymbol = symbolTable.referenceSimpleFunction(signature)
                (declaration as IrFunctionWithLateBinding).acquireSymbol(newSymbol)
                symbolTable.declareSimpleFunction(signature, { newSymbol }, { declaration })
            }
            is IrConstructor -> {
                val newSymbol = symbolTable.referenceConstructor(signature)
                (declaration as IrConstructorWithLateBinding).acquireSymbol(newSymbol)
                symbolTable.declareConstructor(signature, { newSymbol }, { declaration })
            }
            is IrProperty -> {
                val newSymbol = symbolTable.referenceProperty(signature)
                (declaration as IrPropertyWithLateBinding).acquireSymbol(newSymbol)
                symbolTable.declareProperty(signature, { newSymbol }, { declaration })
                declaration.getter?.let(::computeSignatureAndRegisterInSymbolTable)
                declaration.setter?.let(::computeSignatureAndRegisterInSymbolTable)
            }
        }
    }

    fun deserializeAllCStructsAndEnums() {
        for (id in metadataReader.getDeclaredDeclarationIds()) {
            if (id.kind != TopLevelSymbolKind.CLASS_SYMBOL) continue

            // All C structs and enums are expected to be top-level classes.
            // Also, nested classes cannot be loaded here directly, as all class members should be loaded only when deserializing
            // their parent class.
            if (!id.relativeDeclarationName.isOneSegmentFQN()) continue

            val kmClass = (metadataReader.retrieveDeclarationsById(id)?.firstOrNull() as KmClass?) ?: continue
            if (kmClass.inheritsFromCStructOrEnum()) {
                val irPackage = getOrCreateContainingPackageFragment(kmClass)
                val irClass = deserializeClass(kmClass, irPackage)
                irPackage.addChild(irClass)
            }
        }
    }

    private fun KmClass.inheritsFromCStructOrEnum(): Boolean = supertypes.any {
        val classFqName = (it.classifier as? KmClassifier.Class)?.name ?: return@any false
        classFqName == "kotlinx/cinterop/CStructVar" || classFqName == "kotlinx/cinterop/CEnum"
    }


    private fun getOrCreateContainingPackageFragment(forKmDeclaration: Any): IrPackageFragment {
        val containerSource = KlibDeserializedContainerSource(klib, moduleHeaderProto, deserializationConfiguration, definedPackageFqName, null)
        val descriptor = DeserializedSecondStageInteropPackageDescriptor(moduleDescriptor, definedPackageFqName, containerSource)
        if (forKmDeclaration is KmClass && forKmDeclaration.inheritsFromCStructOrEnum() && !isLibraryCached) {
            // Most declarations from C-interop Klib are just stubs which shouldn't be lowered, so they are
            // put inside IrExternalPackageFragment, the same way as on the first stage of compilation.
            // But C structs and enums should be (unless already cached), so instead they are put in a special IrFile,
            // as only IrFiles participate in lowering.
            // If the interop Klib is cached, the cache should contain all the implementation already compiled to
            // binary code, so those classes may be treated as regular dependencies.
            return ::typeDefinitionsIrFile.getOrSetIfNull {
                val fileEntry = NaiveSourceBasedFileEntryImpl(NativeStandardInteropNames.cTypeDefinitionsFileName)
                val irFile = IrFileImpl(fileEntry, IrFileSymbolImpl(descriptor), definedPackageFqName, moduleFragment)
                moduleFragment.files += irFile
                irFile
            }
        } else {
            return ::externalIrPackageFragment.getOrSetIfNull {
                IrExternalPackageFragmentImpl(IrExternalPackageFragmentSymbolImpl(descriptor), definedPackageFqName)
            }
        }
    }

    private fun deserializeClass(kmClass: KmClass, parent: IrDeclarationParent): IrClass {
        require(kmClass.typeParameters.isEmpty()) { "Classes inside C-interop Klibs are not expected to have type parameters." }
        require(!kmClass.name.isLocalClassName()) { "Local/anonymous classes are not supported: ${kmClass.name}." }

        val packageFqName = kmClass.name.substringBeforeLast('/').replace("/", ".")
        val classFqName = kmClass.name.substringAfterLast('/')
        val classSimpleName = classFqName.substringAfterLast('.')
        val signatureMask = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(true)
        val signature = IdSignature.CommonSignature(packageFqName, classFqName, null, signatureMask, null)

        val clazz = symbolTable.declareClass(signature, { IrClassSymbolImpl(signature = signature) }) { symbol ->
            IrFactoryImpl.createClass(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    symbol = symbol,
                    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                    name = Name.identifier(classSimpleName),
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
            )
        }

        clazz.annotations = kmClass.annotations.map { deserializeAnnotation(it) }
        clazz.superTypes = if (kmClass.supertypes.isNotEmpty()) {
            kmClass.supertypes.map { it.toIrType() }
        } else {
            listOf(builtIns.anyType)
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
            val nestedClassFqName = FqName(classFqName).child(Name.identifier(nestedClassName))
            val nestedClassId = MetadataDeclarationId(TopLevelSymbolKind.CLASS_SYMBOL, FqName(packageFqName), nestedClassFqName)
            val nestedKmClass = metadataReader.retrieveDeclarationsById(nestedClassId)?.first() as KmClass? ?: continue
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

        linker.fakeOverrideBuilder.enqueueClass(clazz, signature, CompatibilityMode.CURRENT)
        return clazz
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
            )
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
                returnType = builtIns.arrayClass.typeWith(enumClass.defaultType),
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
                type = builtIns.stringType
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
                    returnType = symbols.enumEntriesInterface.typeWith(enumClass.defaultType),
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
                add(createExtensionReceiverParameter(
                        it.toIrType(typeParametersById), kmFunction.extensionReceiverParameterAnnotations, function
                ))
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
            property.getter?.let { getter ->
                val irValue = deserializeAnnotationArgument(kmValue)
                getter.body = IrFactoryImpl.createExpressionBody(irValue)
            }
        }

        property.annotations = kmProperty.annotations.map { deserializeAnnotation(it) }

        property.parent = parent
        return property
    }

    private fun deserializeAccessor(kmAccessor: KmPropertyAccessorAttributes, isSetter: Boolean, kmProperty: KmProperty, parent: IrDeclarationParent): IrSimpleFunction {
        val propertyType = kmProperty.returnType.toIrType()
        val accessor = IrFactoryImpl.createFunctionWithLateBinding(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = if (isSetter) Name.special("<set-${kmProperty.name}>") else Name.special("<get-${kmProperty.name}>"),
                visibility = kmAccessor.visibility.toDescriptorVisibility(),
                modality = kmAccessor.modality.toDescriptorModality(),
                returnType = if (isSetter) builtIns.unitType else propertyType,
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
                add(createExtensionReceiverParameter(it.toIrType(),
                        kmProperty.extensionReceiverParameterAnnotations, accessor))
            }
            if (isSetter) {
                add(
                        IrFactoryImpl.createValueParameter(
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
                )
            }
        }
        accessor.parameters.forEach { it.parent = accessor }

        accessor.annotations = kmAccessor.annotations.map { deserializeAnnotation(it) }

        accessor.parent = parent
        return accessor
    }

    private fun deserializeRegularParameter(kmParameter: KmValueParameter, parent: IrFunction, typeParametersInScope: Map<Int, IrTypeParameter>): IrValueParameter {
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

    private fun deserializeTypeParameters(kmParameters: List<KmTypeParameter>): Map<Int, IrTypeParameter> {
        val kmToIrParam = kmParameters.withIndex().associate { (index, kmParameter) ->
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
        for ((kmParameter, irParameter) in kmToIrParam) {
            irParameter.superTypes = kmParameter.upperBounds.map { it.toIrType(typeParamsById) }
        }

        return typeParamsById
    }

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
                annotationClassSymbol = annotationClassSymbol,
                argumentMapping = irArguments,
                linker = linker,
        )
    }

    private fun deserializeAnnotationArgument(kmArgument: KmAnnotationArgument): IrExpression {
        return when (kmArgument) {
            is KmAnnotationArgument.ByteValue -> IrConstImpl.byte(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.byteType, kmArgument.value)
            is KmAnnotationArgument.ShortValue -> IrConstImpl.short(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.shortType, kmArgument.value)
            is KmAnnotationArgument.IntValue -> IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.intType, kmArgument.value)
            is KmAnnotationArgument.LongValue -> IrConstImpl.long(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.longType, kmArgument.value)
            is KmAnnotationArgument.UByteValue -> IrConstImpl.byte(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.ubyteType, kmArgument.value.toByte())
            is KmAnnotationArgument.UShortValue -> IrConstImpl.short(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.ushortType, kmArgument.value.toShort())
            is KmAnnotationArgument.UIntValue -> IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.uintType, kmArgument.value.toInt())
            is KmAnnotationArgument.ULongValue -> IrConstImpl.long(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.ulongType, kmArgument.value.toLong())
            is KmAnnotationArgument.FloatValue -> IrConstImpl.float(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.floatType, kmArgument.value)
            is KmAnnotationArgument.DoubleValue -> IrConstImpl.double(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.doubleType, kmArgument.value)
            is KmAnnotationArgument.CharValue -> IrConstImpl.char(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.charType, kmArgument.value)
            is KmAnnotationArgument.BooleanValue -> IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.booleanType, kmArgument.value)
            is KmAnnotationArgument.StringValue -> IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.stringType, kmArgument.value)
            is KmAnnotationArgument.AnnotationValue -> deserializeAnnotation(kmArgument.annotation)
            is KmAnnotationArgument.KClassValue -> {
                val classSymbol = findReferencedClass(kmArgument.className)
                IrClassReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, builtIns.kClassClass.starProjectedType, classSymbol, classSymbol.defaultTypeWithoutArguments)
            }
            is KmAnnotationArgument.ArrayKClassValue -> TODO("Unsupported annotation argument kind used inside C-interop Klib: Array class reference")
            is KmAnnotationArgument.EnumValue -> {
                val pkgFqName = kmArgument.enumClassName.substringBeforeLast('/').replace("/", ".")
                val enumEntryFqName = kmArgument.enumClassName.substringAfterLast('/') + "." + kmArgument.enumEntryName
                val enumEntrySig = IdSignature.CommonSignature(pkgFqName, enumEntryFqName, null, 0, null)
                val enumEntrySymbol = linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(
                        enumEntrySig, BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL, this) as IrEnumEntrySymbol
                val enumClassSymbol = findReferencedClass(kmArgument.enumClassName)
                val irType = enumClassSymbol.defaultTypeWithoutArguments
                IrGetEnumValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irType, enumEntrySymbol)
            }
            is KmAnnotationArgument.ArrayValue -> {
                val elements = kmArgument.elements.map { deserializeAnnotationArgument(it) }
                val varargElementType = elements.mapToSetOrEmpty { it.type }.singleOrNull() ?: builtIns.anyType
                val arrayType = builtIns.primitiveArrayForType[varargElementType]?.defaultTypeWithoutArguments
                        ?: builtIns.arrayClass.typeWith(varargElementType)
                IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, arrayType, varargElementType, elements)
            }
        }
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

    private fun KmVariance.toIrVariance(): Variance = when (this) {
        KmVariance.INVARIANT -> Variance.INVARIANT
        KmVariance.IN -> Variance.INVARIANT
        KmVariance.OUT -> Variance.OUT_VARIANCE
    }

    private fun findReferencedClassifier(classifier: KmClassifier, typeParametersInScope: Map<Int, IrTypeParameter> = emptyMap()): IrClassifierSymbol {
        return when (classifier) {
            is KmClassifier.TypeParameter -> typeParametersInScope[classifier.id]?.symbol
                    ?: error("No type parameter with id ${classifier.id} found in the current scope.")
            is KmClassifier.Class -> findReferencedClass(classifier.name)
            is KmClassifier.TypeAlias -> error("Unexpected type alias reference.\n" +
                    "All types in metadata are expected to be expanded, except in KmType.abbreviatedType, " +
                    "however, it should be ignored, as IR does not use type abbreviations.")
        }
    }

    private fun findReferencedClass(className: ClassName): IrClassSymbol {
        require(!className.isLocalClassName()) { "Local/anonymous classes are not supported: $className" }
        val pkgFqName = FqName(className.substringBeforeLast('/').replace("/", "."))
        val classFqName = className.substringAfterLast('/')

        // A C-interop Klib may only reference classes from the Kotlin stdlib, itself, or other C-interop Klibs.
        // Additionally, interop Klibs may reference special "forward declared" classes, which are not physically present in
        // any Klib. We also know that:
        // - The classes from Stdlib that could be referenced are in `kotlin` and `kotlinx.cinterop` packages.
        // - Forward declared classes are designated by one of the predefined packages (`cnames` and `objcnames`).
        // - Other Klibs are not expected to define any of those packages (see also KT-85765, KT-86193).
        // We can use all of that to infer whether a referenced class comes from Kolin code (the Stdlib), forward declarations,
        // or otherwise, from C-interop Klib. This information is necessary to construct a proper IdSignature.
        val isFromStdlib = pkgFqName.isSubpackageOf(FqName("kotlin")) || pkgFqName.isSubpackageOf(FqName("kotlinx.cinterop"))
        val isForwardDeclaration = pkgFqName in NativeForwardDeclarationKind.packageFqNameToKind
        val cinteropFlag = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.encode(!isFromStdlib && !isForwardDeclaration)
        val classSignature = IdSignature.CommonSignature(pkgFqName.asString(), classFqName, null, cinteropFlag, null)

        return linker.deserializeOrReturnUnboundIrSymbolIfPartialLinkageEnabled(classSignature, BinarySymbolData.SymbolKind.CLASS_SYMBOL,
                this@KonanInteropModuleDeserializer) as IrClassSymbol
    }


    private class KlibMetadataReader(
            private val klib: KotlinLibrary,
    ) {
        private var areDeclarationIdsLoaded = false

        // A cache of all package-level metadata declarations defined in a Klib. Note that this also includes nested classes,
        // because in metadata, they are serialized at package level. (In other words, this map contains both `Map` and `Map.Entry`, but not
        // their methods.)
        // The value is of type Any, because there is no common type between e.g., KmClass and KmFunction.
        // It is wrapped in a List, because there may be multiple functions and properites (although not classes) with the same FQ name.
        // It is wrapped in a SoftReference, because keeping all metadata from all referenced interop libraries may have a significant
        // pressure on memory. If the requested declaration is GC'ed, the entire Klib is loaded again.
        // Also, when a given metadata declaration is converted into an IR declaration, the entry is replaced with a `null` value, as its
        // metadata representation is no longer needed. The entry is not removed completely, so that the map still has keys for all the
        // defined declarations.
        val allMetadataDeclarations: MutableMap<MetadataDeclarationId, SoftReference<List<Any>>?> = hashMapOf()

        private fun ensureDeclarationIdsLoaded() {
            // Metadata has to be loaded at least once to populate the IDs of available declarations.
            if (!areDeclarationIdsLoaded) {
                loadAndCacheMetadata()
                areDeclarationIdsLoaded = true
            }
        }

        private fun loadAndCacheMetadata(): Map<MetadataDeclarationId, List<Any>> {
            val metadataComponent = klib.metadata
            val provider = object : KlibModuleMetadata.MetadataLibraryProvider {
                override val moduleHeaderData get() = metadataComponent.moduleHeaderData
                override val metadataVersion = KlibMetadataVersion((klib.metadataVersion?.toArray()
                        ?: error("No metadata version specified in ${klib.location}")))

                override fun packageMetadata(fqName: String, partName: String) = metadataComponent.getPackageFragment(fqName, partName)
                override fun packageMetadataParts(fqName: String) = metadataComponent.getPackageFragmentNames(fqName)
            }
            val metadataModule = KlibModuleMetadata.readStrict(provider)

            val deserializedDeclarations = mutableMapOf<MetadataDeclarationId, MutableList<Any>>()
            for (packageFragment in metadataModule.fragments) {
                val packageFqName = FqName(packageFragment.fqName ?: continue)
                for (clazz in packageFragment.classes) {
                    val classFqName = FqName(clazz.name.substringAfterLast('/'))
                    val id = MetadataDeclarationId(TopLevelSymbolKind.CLASS_SYMBOL, packageFqName, classFqName)
                    deserializedDeclarations.putToMultiMap(id, clazz)
                }

                val pkg = packageFragment.pkg ?: continue
                for (function in pkg.functions) {
                    val id = MetadataDeclarationId(TopLevelSymbolKind.FUNCTION_SYMBOL, packageFqName, FqName(function.name))
                    deserializedDeclarations.putToMultiMap(id, function)
                }
                for (property in pkg.properties) {
                    val id = MetadataDeclarationId(TopLevelSymbolKind.PROPERTY_SYMBOL, packageFqName, FqName(property.name))
                    deserializedDeclarations.putToMultiMap(id, property)
                }
            }

            for ((id, declarations) in deserializedDeclarations) {
                if (id !in allMetadataDeclarations || allMetadataDeclarations[id]?.get() == null) {
                    allMetadataDeclarations[id] = SoftReference(declarations)
                }
            }

            return deserializedDeclarations
        }

        fun getDeclaredDeclarationIds(): Set<MetadataDeclarationId> {
            ensureDeclarationIdsLoaded()
            return allMetadataDeclarations.keys
        }

        // Note: calling this function a second time for the same id will return `null`.
        fun retrieveDeclarationsById(id: MetadataDeclarationId): List<Any>? {
            ensureDeclarationIdsLoaded()
            val ref = allMetadataDeclarations.replace(id, null) ?: return null
            ref.get()?.let {
                return it
            }

            val allDeclarations = loadAndCacheMetadata()
            return allDeclarations[id]
        }
    }

    private data class MetadataDeclarationId(
            val kind: TopLevelSymbolKind,
            val packageFqName: FqName,
            val relativeDeclarationName: FqName,
    )
}

class DeserializedSecondStageInteropPackageDescriptor(
        module: ModuleDescriptor,
        fqName: FqName,
        private val containerSource: KlibDeserializedContainerSource,
) : PackageFragmentDescriptorImpl(module, fqName) {
    override fun getMemberScope(): MemberScope = error("K1-specific functionality is not supported.")
    override fun getSource(): SourceElement = containerSource
}