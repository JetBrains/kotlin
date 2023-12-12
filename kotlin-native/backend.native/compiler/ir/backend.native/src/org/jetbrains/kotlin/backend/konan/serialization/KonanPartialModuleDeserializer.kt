/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinaryNameAndType
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.InlineFunctionOriginInfo
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getPublicSignature
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class KonanPartialModuleDeserializer(
        konanIrLinker: KonanIrLinker,
        moduleDescriptor: ModuleDescriptor,
        override val klib: KotlinLibrary,
        private val stubGenerator: DeclarationStubGenerator,
        private val cachedLibraries: CachedLibraries,
        private val inlineFunctionFiles: MutableMap<IrExternalPackageFragment, IrFile>,
        strategyResolver: (String) -> DeserializationStrategy,
        private val cacheDeserializationStrategy: CacheDeserializationStrategy,
        containsErrorCode: Boolean = false
) : BasicIrModuleDeserializer(konanIrLinker, moduleDescriptor, klib,
        { fileName ->
            if (cacheDeserializationStrategy.contains(fileName))
                strategyResolver(fileName)
            else DeserializationStrategy.ON_DEMAND
        }, klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT, containsErrorCode
) {
    companion object {
        private const val INVALID_INDEX = -1
    }

    private val descriptorSignatures = mutableMapOf<DeclarationDescriptor, IdSignature>()

    val files by lazy { fileDeserializationStates.map { it.file } }

    private val idSignatureToFile by lazy {
        buildMap {
            fileDeserializationStates.forEach { fileDeserializationState ->
                fileDeserializationState.fileDeserializer.reversedSignatureIndex.keys.forEach { idSig ->
                    put(idSig, fileDeserializationState.file)
                }
            }
        }
    }

    private val fileReferenceToFileDeserializationState by lazy {
        fileDeserializationStates.associateBy { SerializedFileReference(it.file.packageFqName.asString(), it.file.path) }
    }

    private val SerializedFileReference.deserializationState
        get() = fileReferenceToFileDeserializationState[this] ?: error("Unknown file $this")

    private tailrec fun IdSignature.fileSignature(): IdSignature.FileSignature? = when (this) {
        is IdSignature.FileSignature -> this
        is IdSignature.CompositeSignature -> this.container.fileSignature()
        else -> null
    }

    private val IrClass.firstNonClassParent: IrDeclarationParent
        get() {
            var parent = parent
            while (parent is IrClass) parent = parent.parent
            return parent
        }

    fun getFileNameOf(declaration: IrDeclaration): String {
        fun IrDeclaration.getSignature() = symbol.signature ?: descriptorSignatures[descriptor]

        val idSig = declaration.getSignature()
                ?: (declaration.parent as? IrDeclaration)?.getSignature()
                ?: ((declaration as? IrAttributeContainer)?.attributeOwnerId as? IrDeclaration)?.getSignature()
                ?: error("Can't find signature of ${declaration.render()}")
        val topLevelIdSig = idSig.topLevelSignature()
        return topLevelIdSig.fileSignature()?.fileName
                ?: idSignatureToFile[topLevelIdSig]?.path
                ?: error("No file for $idSig")
    }

    fun getKlibFileIndexOf(irFile: IrFile) = fileDeserializationStates.first { it.file == irFile }.fileIndex

    fun buildInlineFunctionReference(irFunction: IrFunction): SerializedInlineFunctionReference {
        val signature = irFunction.symbol.signature
                ?: error("No signature for ${irFunction.render()}")
        val topLevelSignature = signature.topLevelSignature()
        val fileDeserializationState = moduleReversedFileIndex[topLevelSignature]
                ?: error("No file deserializer for ${topLevelSignature.render()}")
        val declarationIndex = fileDeserializationState.fileDeserializer.reversedSignatureIndex[topLevelSignature]
                ?: error("No declaration for ${topLevelSignature.render()}")
        val fileReader = fileDeserializationState.fileReader
        val symbolDeserializer = fileDeserializationState.fileDeserializer.symbolDeserializer
        val protoDeclaration = fileReader.declaration(declarationIndex)

        val outerClasses = (irFunction.parent as? IrClass)?.getOuterClasses(takeOnlyInner = false) ?: emptyList()
        require((outerClasses.getOrNull(0)?.parent ?: irFunction.parent) is IrFile) {
            "Local inline functions are not supported: ${irFunction.render()}"
        }

        val typeParameterSigs = mutableListOf<Int>()
        val outerReceiverSigs = mutableListOf<Int>()
        val protoFunction = if (outerClasses.isEmpty()) {
            val irProperty = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner
            if (irProperty == null)
                protoDeclaration.irFunction
            else protoDeclaration.irProperty.findAccessor(irProperty, irFunction)
        } else {
            val firstNotInnerClassIndex = outerClasses.indexOfLast { !it.isInner }
            var protoClass = protoDeclaration.irClass
            outerClasses.indices.forEach { classIndex ->
                if (classIndex >= firstNotInnerClassIndex /* owner's type parameters are always accessible */) {
                    (0 until protoClass.typeParameterCount).mapTo(typeParameterSigs) {
                        BinarySymbolData.decode(protoClass.getTypeParameter(it).base.symbol).signatureId
                    }
                }
                if (classIndex < outerClasses.size - 1) {
                    if (classIndex >= firstNotInnerClassIndex)
                        outerReceiverSigs.add(BinarySymbolData.decode(protoClass.thisReceiver.base.symbol).signatureId)
                    protoClass = protoClass.findClass(outerClasses[classIndex + 1], fileReader, symbolDeserializer)
                }
            }
            protoClass.findInlineFunction(irFunction, fileReader, symbolDeserializer)
        }

        val functionSignature = BinarySymbolData.decode(protoFunction.base.base.symbol).signatureId
        (0 until protoFunction.base.typeParameterCount).mapTo(typeParameterSigs) {
            BinarySymbolData.decode(protoFunction.base.getTypeParameter(it).base.symbol).signatureId
        }
        val defaultValues = mutableListOf<Int>()
        val valueParameterSigs = (0 until protoFunction.base.valueParameterCount).map {
            val valueParameter = protoFunction.base.getValueParameter(it)
            defaultValues.add(if (valueParameter.hasDefaultValue()) valueParameter.defaultValue else INVALID_INDEX)
            BinarySymbolData.decode(valueParameter.base.symbol).signatureId
        }
        val extensionReceiverSig = irFunction.extensionReceiverParameter?.let {
            BinarySymbolData.decode(protoFunction.base.extensionReceiver.base.symbol).signatureId
        } ?: INVALID_INDEX
        val dispatchReceiverSig = irFunction.dispatchReceiverParameter?.let {
            BinarySymbolData.decode(protoFunction.base.dispatchReceiver.base.symbol).signatureId
        } ?: INVALID_INDEX

        return SerializedInlineFunctionReference(
                SerializedFileReference(fileDeserializationState.file),
                functionSignature, protoFunction.base.body, irFunction.startOffset, irFunction.endOffset,
                extensionReceiverSig, dispatchReceiverSig, outerReceiverSigs.toIntArray(),
                valueParameterSigs.toIntArray(), typeParameterSigs.toIntArray(), defaultValues.toIntArray()
        )
    }

    fun buildClassFields(irClass: IrClass, fields: List<ClassLayoutBuilder.FieldInfo>): SerializedClassFields {
        val signature = irClass.symbol.signature
                ?: error("No signature for ${irClass.render()}")
        val topLevelSignature = signature.topLevelSignature()
        val fileDeserializationState = moduleReversedFileIndex[topLevelSignature]
                ?: error("No file deserializer for ${topLevelSignature.render()}")
        val fileDeserializer = fileDeserializationState.fileDeserializer
        val declarationIndex = fileDeserializer.reversedSignatureIndex[topLevelSignature]
                ?: error("No declaration for ${topLevelSignature.render()}")
        val fileReader = fileDeserializationState.fileReader
        val symbolDeserializer = fileDeserializer.symbolDeserializer
        val protoDeclaration = fileReader.declaration(declarationIndex)

        val outerClasses = irClass.getOuterClasses(takeOnlyInner = false)
        require(outerClasses.first().parent is IrFile) { "Local classes are not supported: ${irClass.render()}" }

        val typeParameterSigs = mutableListOf<Int>()
        var protoClass = protoDeclaration.irClass
        val protoClasses = mutableListOf(protoClass)
        val firstNotInnerClassIndex = outerClasses.indexOfLast { !it.isInner }
        for (classIndex in outerClasses.indices) {
            if (classIndex >= firstNotInnerClassIndex /* owner's type parameters are always accessible */) {
                (0 until protoClass.typeParameterCount).mapTo(typeParameterSigs) {
                    BinarySymbolData.decode(protoClass.getTypeParameter(it).base.symbol).signatureId
                }
            }
            if (classIndex < outerClasses.size - 1) {
                protoClass = protoClass.findClass(outerClasses[classIndex + 1], fileReader, symbolDeserializer)
                protoClasses += protoClass
            }
        }

        val protoFields = mutableListOf<ProtoField>()
        for (i in 0 until protoClass.declarationCount) {
            val declaration = protoClass.getDeclaration(i)
            if (declaration.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_FIELD)
                protoFields.add(declaration.irField)
            else if (declaration.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_PROPERTY) {
                val protoProperty = declaration.irProperty
                if (protoProperty.hasBackingField())
                    protoFields.add(protoProperty.backingField)
            }
        }
        val protoFieldsMap = mutableMapOf<String, ProtoField>()
        protoFields.forEach {
            val nameAndType = BinaryNameAndType.decode(it.nameType)
            val name = fileReader.string(nameAndType.nameIndex)
            val prev = protoFieldsMap[name]
            if (prev != null)
                error("Class ${irClass.render()} has two fields with same name '$name'")
            protoFieldsMap[name] = it
        }

        val outerThisIndex = fields.indexOfFirst { it.irField?.origin == IrDeclarationOrigin.FIELD_FOR_OUTER_THIS }
        val compatibleMode = CompatibilityMode(libraryAbiVersion).oldSignatures
        return SerializedClassFields(
                SerializedFileReference(fileDeserializationState.file),
                signature,
                typeParameterSigs.toIntArray(),
                outerThisIndex,
                Array(fields.size) {
                    val field = fields[it]
                    if (it == outerThisIndex) {
                        require(irClass.isInner) { "Expected an inner class: ${irClass.render()}" }
                        require(protoClasses.size > 1) { "An inner class must have at least one outer class" }
                        val outerProtoClass = protoClasses[protoClasses.size - 2]
                        val nameAndType = BinaryNameAndType.decode(outerProtoClass.thisReceiver.nameType)

                        SerializedClassFieldInfo(
                                name = INVALID_INDEX,
                                binaryType = INVALID_INDEX,
                                nameAndType.typeIndex,
                                flags = 0,
                                field.alignment
                        )
                    } else {
                        val protoField = protoFieldsMap[field.name] ?: error("No proto for ${field.name}")
                        val nameAndType = BinaryNameAndType.decode(protoField.nameType)
                        var flags = 0
                        if (field.isConst)
                            flags = flags or SerializedClassFieldInfo.FLAG_IS_CONST
                        val classifier = field.type.classifierOrNull
                                ?: error("Fields of type ${field.type.render()} are not supported")
                        val primitiveBinaryType = field.type.computePrimitiveBinaryTypeOrNull()

                        SerializedClassFieldInfo(
                                nameAndType.nameIndex,
                                primitiveBinaryType?.ordinal ?: INVALID_INDEX,
                                if (with(KonanManglerIr) { (classifier as? IrClassSymbol)?.owner?.isExported(compatibleMode) } == false)
                                    INVALID_INDEX
                                else nameAndType.typeIndex,
                                flags,
                                field.alignment
                        )
                    }
                })
    }

    fun buildEagerInitializedFile(irFile: IrFile) =
            SerializedEagerInitializedFile(SerializedFileReference(irFile))

    private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
            moduleDescriptor, KonanManglerDesc,
            DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY
    )

    private val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

    // Need to notify the deserializing machinery that some symbols have already been created by stub generator
    // (like type parameters and receiver parameters) and there's no need to create new symbols for them.
    private fun referenceIrSymbol(symbolDeserializer: IrSymbolDeserializer, sigIndex: Int, symbol: IrSymbol) {
        val idSig = symbolDeserializer.deserializeIdSignature(sigIndex)
        symbolDeserializer.referenceLocalIrSymbol(symbol, idSig)
        if (idSig.isPubliclyVisible) {
            deserializedSymbols[idSig]?.let {
                require(it == symbol) { "Two different symbols for the same signature ${idSig.render()}" }
            }
            // Sometimes the linker would want to create a new symbol, so save actual symbol here
            // and use it in [contains] and [tryDeserializeSymbol].
            deserializedSymbols[idSig] = symbol
        }
    }

    override fun contains(idSig: IdSignature): Boolean =
            super.contains(idSig) || deserializedSymbols.containsKey(idSig) ||
                    cacheDeserializationStrategy != CacheDeserializationStrategy.WholeModule
                    && idSig.isPubliclyVisible && descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        super.tryDeserializeIrSymbol(idSig, symbolKind)?.let { return it }

        deserializedSymbols[idSig]?.let { return it }

        val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: return null

        descriptorSignatures[descriptor] = idSig

        return (stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner).symbol
    }

    private val inlineFunctionReferences by lazy {
        val cache = cachedLibraries.getLibraryCache(klib)!! // ?: error("No cache for ${klib.libraryName}") // KT-54668
        cache.serializedInlineFunctionBodies.associateBy {
            it.file.deserializationState.declarationDeserializer.symbolDeserializer.deserializeIdSignature(it.functionSignature)
        }
    }

    private val deserializedInlineFunctions = mutableMapOf<IrFunction, InlineFunctionOriginInfo>()

    fun deserializeInlineFunction(function: IrFunction): DeserializedInlineFunction {
        deserializedInlineFunctions[function]?.let { return DeserializedInlineFunction(firstAccess = false, it) }
        val result = deserializeInlineFunctionInternal(function)
        deserializedInlineFunctions[function] = result
        return DeserializedInlineFunction(firstAccess = true, result)
    }

    private fun deserializeInlineFunctionInternal(function: IrFunction): InlineFunctionOriginInfo {
        val packageFragment = function.getPackageFragment()
        if (function.parents.any { (it as? IrFunction)?.isInline == true }) {
            // Already deserialized by the top-most inline function.
            return InlineFunctionOriginInfo(
                    function,
                    packageFragment as? IrFile
                            ?: inlineFunctionFiles[packageFragment as IrExternalPackageFragment]
                            ?: error("${function.render()} should've been deserialized along with its parent"),
                    function.startOffset, function.endOffset
            )
        }

        val signature = function.symbol.signature
                ?: descriptorSignatures[function.descriptor]
                ?: error("No signature for ${function.render()}")
        val inlineFunctionReference = inlineFunctionReferences[signature]
                ?: error("No inline function reference for ${function.render()}, sig = ${signature.render()}")
        val fileDeserializationState = inlineFunctionReference.file.deserializationState
        val declarationDeserializer = fileDeserializationState.declarationDeserializer

        if (packageFragment is IrExternalPackageFragment) {
            val symbolDeserializer = declarationDeserializer.symbolDeserializer

            inlineFunctionFiles[packageFragment]?.let {
                require(it == fileDeserializationState.file) {
                    "Different files ${it.fileEntry.name} and ${fileDeserializationState.file.fileEntry.name} have the same $packageFragment"
                }
            }
            inlineFunctionFiles[packageFragment] = fileDeserializationState.file

            val outerClasses = (function.parent as? IrClass)?.getOuterClasses(takeOnlyInner = true) ?: emptyList()
            require((outerClasses.getOrNull(0)?.firstNonClassParent ?: function.parent) is IrPackageFragment) {
                "Local inline functions are not supported: ${function.render()}"
            }

            var endToEndTypeParameterIndex = 0
            outerClasses.forEach { outerClass ->
                outerClass.typeParameters.forEach { parameter ->
                    val sigIndex = inlineFunctionReference.typeParameterSigs[endToEndTypeParameterIndex++]
                    referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
                }
            }
            function.typeParameters.forEach { parameter ->
                val sigIndex = inlineFunctionReference.typeParameterSigs[endToEndTypeParameterIndex++]
                referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }
            function.valueParameters.forEachIndexed { index, parameter ->
                val sigIndex = inlineFunctionReference.valueParameterSigs[index]
                referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }
            function.extensionReceiverParameter?.let { parameter ->
                val sigIndex = inlineFunctionReference.extensionReceiverSig
                require(sigIndex != INVALID_INDEX) { "Expected a valid sig reference to the extension receiver for ${function.render()}" }
                referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }
            function.dispatchReceiverParameter?.let { parameter ->
                val sigIndex = inlineFunctionReference.dispatchReceiverSig
                require(sigIndex != INVALID_INDEX) { "Expected a valid sig reference to the dispatch receiver for ${function.render()}" }
                referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }
            for (index in 0 until outerClasses.size - 1) {
                val sigIndex = inlineFunctionReference.outerReceiverSigs[index]
                referenceIrSymbol(symbolDeserializer, sigIndex, outerClasses[index].thisReceiver!!.symbol)
            }
        }

        with(declarationDeserializer) {
            function.withDeserializeBodies {
                body = (deserializeStatementBody(inlineFunctionReference.body) as IrBody)
                valueParameters.forEachIndexed { index, parameter ->
                    val defaultValueIndex = inlineFunctionReference.defaultValues[index]
                    if (defaultValueIndex != INVALID_INDEX)
                        parameter.defaultValue = deserializeExpressionBody(defaultValueIndex)
                }
            }
        }
        if (packageFragment is IrFile)
            linker.deserializeAllReachableTopLevels()

        linker.partialLinkageSupport.exploreClassifiers(linker.fakeOverrideBuilder)
        linker.partialLinkageSupport.exploreClassifiersInInlineLazyIrFunction(function)

        linker.fakeOverrideBuilder.provideFakeOverrides()

        linker.partialLinkageSupport.generateStubsAndPatchUsages(linker.symbolTable, function)

        linker.checkNoUnboundSymbols(
                linker.symbolTable,
                "after deserializing lazy-IR function ${function.name.asString()} in inline functions lowering"
        )

        return InlineFunctionOriginInfo(
                function,
                fileDeserializationState.file,
                inlineFunctionReference.startOffset,
                inlineFunctionReference.endOffset
        )
    }

    private val classesFields by lazy {
        val cache = cachedLibraries.getLibraryCache(klib)!! // ?: error("No cache for ${klib.libraryName}") // KT-54668
        cache.serializedClassFields.associateBy {
            it.classSignature
        }
    }

    private val lock = Any()

    fun deserializeClassFields(irClass: IrClass, outerThisFieldInfo: ClassLayoutBuilder.FieldInfo?): List<ClassLayoutBuilder.FieldInfo> = synchronized(lock) {
        val signature = irClass.symbol.signature
                ?: error("No signature for ${irClass.render()}")
        val serializedClassFields = classesFields[signature]
                ?: error("No class fields for ${irClass.render()}, sig = ${signature.render()}")
        val fileDeserializationState = serializedClassFields.file.deserializationState
        val declarationDeserializer = fileDeserializationState.declarationDeserializer
        val symbolDeserializer = declarationDeserializer.symbolDeserializer

        if (irClass.getPackageFragment() is IrExternalPackageFragment) {
            val outerClasses = irClass.getOuterClasses(takeOnlyInner = true)
            require(outerClasses.first().firstNonClassParent is IrExternalPackageFragment) {
                "Local classes are not supported: ${irClass.render()}"
            }

            var endToEndTypeParameterIndex = 0
            outerClasses.forEach { outerClass ->
                outerClass.typeParameters.forEach { parameter ->
                    val sigIndex = serializedClassFields.typeParameterSigs[endToEndTypeParameterIndex++]
                    referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
                }
            }
            require(endToEndTypeParameterIndex == serializedClassFields.typeParameterSigs.size) {
                "Not all type parameters have been referenced"
            }
        }

        fun getByClassId(classId: ClassId): IrClassSymbol {
            val classIdSig = getPublicSignature(classId.packageFqName, classId.relativeClassName.asString())
            return symbolDeserializer.deserializePublicSymbol(classIdSig, BinarySymbolData.SymbolKind.CLASS_SYMBOL) as IrClassSymbol
        }

        return serializedClassFields.fields.mapIndexed { index, field ->
            val builtIns = linker.builtIns
            if (index == serializedClassFields.outerThisIndex) {
                require(irClass.isInner) { "Expected an inner class: ${irClass.render()}" }
                require(outerThisFieldInfo != null) { "For an inner class ${irClass.render()} there should be <outer this> field" }
                outerThisFieldInfo.also {
                    require(it.alignment == field.alignment) { "Mismatched align information for outer this" }
                }
            } else {
                val name = fileDeserializationState.fileReader.string(field.name)
                val type = when {
                    field.type != INVALID_INDEX -> declarationDeserializer.deserializeIrType(field.type)
                    field.binaryType == INVALID_INDEX -> builtIns.anyNType
                    else -> when (PrimitiveBinaryType.values().getOrNull(field.binaryType)) {
                        PrimitiveBinaryType.BOOLEAN -> builtIns.booleanType
                        PrimitiveBinaryType.BYTE -> builtIns.byteType
                        PrimitiveBinaryType.SHORT -> builtIns.shortType
                        PrimitiveBinaryType.INT -> builtIns.intType
                        PrimitiveBinaryType.LONG -> builtIns.longType
                        PrimitiveBinaryType.FLOAT -> builtIns.floatType
                        PrimitiveBinaryType.DOUBLE -> builtIns.doubleType
                        PrimitiveBinaryType.POINTER -> getByClassId(KonanPrimitiveType.NON_NULL_NATIVE_PTR.classId).defaultType
                        PrimitiveBinaryType.VECTOR128 -> getByClassId(KonanPrimitiveType.VECTOR128.classId).defaultType
                        else -> error("Bad binary type of field $name of ${irClass.render()}")
                    }
                }
                ClassLayoutBuilder.FieldInfo(
                        name, type,
                        isConst = (field.flags and SerializedClassFieldInfo.FLAG_IS_CONST) != 0,
                        irFieldSymbol = IrFieldSymbolImpl(),
                        alignment = field.alignment,
                )
            }
        }
    }

    private fun IrClass.getOuterClasses(takeOnlyInner: Boolean): List<IrClass> {
        var outerClass = this
        val outerClasses = mutableListOf(outerClass)
        while (outerClass.isInner || !takeOnlyInner) {
            outerClass = outerClass.parent as? IrClass ?: break
            outerClasses.add(outerClass)
        }
        outerClasses.reverse()
        return outerClasses
    }

    val eagerInitializedFiles by lazy {
        val cache = cachedLibraries.getLibraryCache(klib)!! // ?: error("No cache for ${klib.libraryName}") // KT-54668
        cache.serializedEagerInitializedFiles
                .map { it.file.deserializationState.file }
                .distinct()
    }

    val sortedFileIds by lazy {
        fileDeserializationStates
                .sortedBy { it.file.fileEntry.name }
                .map { CacheSupport.cacheFileId(it.file.packageFqName.asString(), it.file.fileEntry.name) }
    }
}