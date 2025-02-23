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
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.ClassLayoutBuilder
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getPublicSignature
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.encodings.WobblyTF8
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.library.impl.IrMemoryArrayWriter
import org.jetbrains.kotlin.library.impl.IrMemoryStringWriter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.collections.set
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrField as ProtoField
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoParameter

private const val INVALID_INDEX = -1

internal class InlineFunctionSerializer(private val deserializer: KonanPartialModuleDeserializer) {
    fun buildInlineFunctionReference(irFunction: IrFunction): SerializedInlineFunctionReference {
        val signature = irFunction.symbol.signature
                ?: error("No signature for ${irFunction.render()}")
        val topLevelSignature = signature.topLevelSignature()
        val fileDeserializationState: FileDeserializationState = deserializer.getFileDeserializationState(topLevelSignature)
        val declarationIndex = fileDeserializationState.fileDeserializer.reversedSignatureIndex[topLevelSignature]
                ?: error("No declaration for ${topLevelSignature.render()}")
        val fileReader = fileDeserializationState.fileReader
        val symbolDeserializer = fileDeserializationState.fileDeserializer.symbolDeserializer
        val protoDeclaration = fileReader.declaration(declarationIndex)

        val outerClasses: List<IrClass> = (irFunction.parent as? IrClass)?.getOuterClasses(takeOnlyInner = false) ?: emptyList()
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

        fun serializeValueParameter(parameter: ProtoParameter): Int =
                BinarySymbolData.decode(parameter.base.symbol).signatureId

        val dispatchReceiverSig = if (protoFunction.base.hasDispatchReceiver()) {
            serializeValueParameter(protoFunction.base.extensionReceiver)
        } else INVALID_INDEX
        val contextParameterSigs = protoFunction.base.contextParameterList.map { param ->
            serializeValueParameter(param)
        }
        val extensionReceiverSig = if (protoFunction.base.hasExtensionReceiver()) {
            serializeValueParameter(protoFunction.base.extensionReceiver)
        } else INVALID_INDEX
        val regularParameterSigs = protoFunction.base.regularParameterList.map { param ->
            serializeValueParameter(param)
        }
        val defaultValues = protoFunction.base.regularParameterList.map { param ->
            if (param.hasDefaultValue()) param.defaultValue else INVALID_INDEX
        }

        return SerializedInlineFunctionReference(
                SerializedFileReference(fileDeserializationState.file),
                functionSignature, protoFunction.base.body, irFunction.startOffset, irFunction.endOffset,
                extensionReceiverSig, dispatchReceiverSig, outerReceiverSigs.toIntArray(),
                contextParameterSigs.toIntArray(), regularParameterSigs.toIntArray(),
                typeParameterSigs.toIntArray(), defaultValues.toIntArray()
        )
    }
}

internal class InlineFunctionDeserializer(
        private val deserializer: KonanPartialModuleDeserializer,
        private val cachedLibraries: CachedLibraries,
        private val linker: KonanIrLinker,
) {
    private val inlineFunctionReferences: Map<IdSignature, SerializedInlineFunctionReference> by lazy {
        val cache = cachedLibraries.getLibraryCache(deserializer.klib) ?: error("No cache for ${deserializer.klib.libraryName}")
        cache.serializedInlineFunctionBodies.associateBy {
            with(deserializer) {
                val symbolDeserializer = it.file.deserializationState.declarationDeserializer.symbolDeserializer
                symbolDeserializer.deserializeIdSignature(it.functionSignature)
            }
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun deserializeInlineFunction(function: IrFunction) {
        val packageFragment = function.getPackageFragment()
        if (function.parents.any { (it as? IrFunction)?.isInline == true }) {
            // Already deserialized by the top-most inline function.
            return
        }

        val signature = function.symbol.signature
                ?: deserializer.getIdSignature(function.descriptor)
                ?: error("No signature for ${function.render()}")
        val inlineFunctionReference = inlineFunctionReferences[signature]
                ?: error("No inline function reference for ${function.render()}, sig = ${signature.render()}")
        val fileDeserializationState = with(deserializer) { inlineFunctionReference.file.deserializationState }
        val declarationDeserializer = fileDeserializationState.declarationDeserializer

        if (packageFragment is IrExternalPackageFragment) {
            val symbolDeserializer = declarationDeserializer.symbolDeserializer
            linker.inlineFunctionFilesTracker.add(packageFragment, fileDeserializationState.file)
            val outerClasses = (function.parent as? IrClass)?.getOuterClasses(takeOnlyInner = true) ?: emptyList()
            require((outerClasses.getOrNull(0)?.firstNonClassParent ?: function.parent) is IrPackageFragment) {
                "Local inline functions are not supported: ${function.render()}"
            }

            var endToEndTypeParameterIndex = 0
            outerClasses.forEach { outerClass ->
                outerClass.typeParameters.forEach { parameter ->
                    val sigIndex = inlineFunctionReference.typeParameterSigs[endToEndTypeParameterIndex++]
                    deserializer.referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
                }
            }
            function.typeParameters.forEach { parameter ->
                val sigIndex = inlineFunctionReference.typeParameterSigs[endToEndTypeParameterIndex++]
                deserializer.referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }

            var contextParamIndex = 0
            var regularParamIndex = 0
            function.parameters.forEach { parameter ->
                val sigIndex = when (parameter.kind) {
                    IrParameterKind.DispatchReceiver -> inlineFunctionReference.dispatchReceiverSig
                    IrParameterKind.Context -> inlineFunctionReference.contextParameterSigs[contextParamIndex++]
                    IrParameterKind.ExtensionReceiver -> inlineFunctionReference.extensionReceiverSig
                    IrParameterKind.Regular -> inlineFunctionReference.regularParameterSigs[regularParamIndex++]
                }
                if (parameter.kind != IrParameterKind.Regular) {
                    require(sigIndex != INVALID_INDEX) { "Expected a valid sig reference to ${parameter.kind} parameter for ${function.render()}" }
                }

                deserializer.referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
            }

            for (index in 0 until outerClasses.size - 1) {
                val sigIndex = inlineFunctionReference.outerReceiverSigs[index]
                deserializer.referenceIrSymbol(symbolDeserializer, sigIndex, outerClasses[index].thisReceiver!!.symbol)
            }
        }

        with(declarationDeserializer) {
            function.withDeserializeBodies {
                body = (deserializeStatementBody(inlineFunctionReference.body) as IrBody)
                parameters.filter { it.kind == IrParameterKind.Regular }.forEachIndexed { index, parameter ->
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

        linker.partialLinkageSupport.enqueueDeclaration(function)
        linker.partialLinkageSupport.generateStubsAndPatchUsages(linker.symbolTable)

        linker.checkNoUnboundSymbols(
                linker.symbolTable,
                "after deserializing lazy-IR function ${function.name.asString()} in inline functions lowering"
        )
    }
}

internal class CachedEagerInitializedFiles(
        private val cachedLibraries: CachedLibraries,
        private val klib: KotlinLibrary,
        private val deserializer: KonanPartialModuleDeserializer,
) {
    val eagerInitializedFiles: List<IrFile> by lazy {
        val cache = cachedLibraries.getLibraryCache(klib)
                ?: error("No cache for ${klib.libraryName}") // KT-54668
        cache.serializedEagerInitializedFiles
                .map { with(deserializer) { it.file.deserializationState.file } }
                .distinct()
    }
}

internal fun buildSerializedClassFields(
        irClass: IrClass,
        fields: List<ClassLayoutBuilder.FieldInfo>,
        deserializer: KonanPartialModuleDeserializer
): SerializedClassFields {
    val signature = irClass.symbol.signature
            ?: error("No signature for ${irClass.render()}")
    val topLevelSignature = signature.topLevelSignature()
    val fileDeserializationState = deserializer.getFileDeserializationState(topLevelSignature)
    val fileDeserializer = with(deserializer) { fileDeserializationState.fileDeserializer }
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
                            name = "",
                            binaryType = INVALID_INDEX,
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
                            field.name,
                            primitiveBinaryType?.ordinal ?: INVALID_INDEX,
                            flags,
                            field.alignment
                    )
                }
            })
}

internal class ClassFieldsDeserializer(
        private val cachedLibraries: CachedLibraries,
        private val builtIns: IrBuiltIns,
        private val deserializer: KonanPartialModuleDeserializer,
) {
    private val lock = Any()

    fun deserializeClassFields(irClass: IrClass, outerThisFieldInfo: ClassLayoutBuilder.FieldInfo?): List<ClassLayoutBuilder.FieldInfo> = synchronized(lock) {
        val signature = irClass.symbol.signature
                ?: error("No signature for ${irClass.render()}")
        val serializedClassFields = classesFields[signature]
                ?: error("No class fields for ${irClass.render()}, sig = ${signature.render()}")
        val fileDeserializationState = with(deserializer) { serializedClassFields.file.deserializationState }
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
                    deserializer.referenceIrSymbol(symbolDeserializer, sigIndex, parameter.symbol)
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
            if (index == serializedClassFields.outerThisIndex) {
                require(irClass.isInner) { "Expected an inner class: ${irClass.render()}" }
                require(outerThisFieldInfo != null) { "For an inner class ${irClass.render()} there should be <outer this> field" }
                outerThisFieldInfo.also {
                    require(it.alignment == field.alignment) { "Mismatched align information for outer this" }
                }
            } else {
                val name = field.name
                val type = when {
                    field.binaryType == INVALID_INDEX -> builtIns.anyNType
                    else -> when (PrimitiveBinaryType.entries.getOrNull(field.binaryType)) {
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

    private val classesFields by lazy {
        val cache = cachedLibraries.getLibraryCache(deserializer.klib) ?: error("No cache for ${deserializer.klib.libraryName}")
        cache.serializedClassFields.associateBy {
            it.classSignature
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

private val IrClass.firstNonClassParent: IrDeclarationParent
    get() {
        var parent = parent
        while (parent is IrClass) parent = parent.parent
        return parent
    }

class SerializedInlineFunctionReference(val file: SerializedFileReference, val functionSignature: Int, val body: Int,
                                        val startOffset: Int, val endOffset: Int,
                                        val extensionReceiverSig: Int, val dispatchReceiverSig: Int, val outerReceiverSigs: IntArray,
                                        val contextParameterSigs: IntArray, val regularParameterSigs: IntArray, val typeParameterSigs: IntArray,
                                        val defaultValues: IntArray)

internal object InlineFunctionBodyReferenceSerializer {
    fun serialize(bodies: List<SerializedInlineFunctionReference>): ByteArray {
        val stringTable = buildStringTable {
            bodies.forEach {
                +it.file.fqName
                +it.file.path
            }
        }
        val size = stringTable.sizeBytes + bodies.sumOf {
            Int.SIZE_BYTES * (13 + it.outerReceiverSigs.size + it.contextParameterSigs.size + it.regularParameterSigs.size +
                    it.typeParameterSigs.size + it.defaultValues.size)
        }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        bodies.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
            stream.writeInt(it.functionSignature)
            stream.writeInt(it.body)
            stream.writeInt(it.startOffset)
            stream.writeInt(it.endOffset)
            stream.writeInt(it.extensionReceiverSig)
            stream.writeInt(it.dispatchReceiverSig)
            stream.writeIntArray(it.outerReceiverSigs)
            stream.writeIntArray(it.contextParameterSigs)
            stream.writeIntArray(it.regularParameterSigs)
            stream.writeIntArray(it.typeParameterSigs)
            stream.writeIntArray(it.defaultValues)
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedInlineFunctionReference>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val functionSignature = stream.readInt()
            val body = stream.readInt()
            val startOffset = stream.readInt()
            val endOffset = stream.readInt()
            val extensionReceiverSig = stream.readInt()
            val dispatchReceiverSig = stream.readInt()
            val outerReceiverSigs = stream.readIntArray()
            val contextParameterSigs = stream.readIntArray()
            val regularParameterSigs = stream.readIntArray()
            val typeParameterSigs = stream.readIntArray()
            val defaultValues = stream.readIntArray()
            result.add(SerializedInlineFunctionReference(
                    SerializedFileReference(fileFqName, filePath), functionSignature, body, startOffset, endOffset,
                    extensionReceiverSig, dispatchReceiverSig, outerReceiverSigs, contextParameterSigs, regularParameterSigs,
                    typeParameterSigs, defaultValues)
            )
        }
    }
}
// [binaryType] is needed in case a field is of a primitive type. Otherwise we know it's an object type and
// that is enough information for the backend.
class SerializedClassFieldInfo(val name: String, val binaryType: Int, val flags: Int, val alignment: Int) {
    companion object {
        const val FLAG_IS_CONST = 1
    }
}

class SerializedClassFields(val file: SerializedFileReference, val classSignature: IdSignature, val typeParameterSigs: IntArray,
                            val outerThisIndex: Int, val fields: Array<SerializedClassFieldInfo>)

internal object ClassFieldsSerializer {

    fun serialize(classFields: List<SerializedClassFields>): ByteArray {

        val protoStringMap = hashMapOf<String, Int>()
        val protoStringArray = arrayListOf<String>()
        val protoIdSignatureMap = mutableMapOf<IdSignature, Int>()
        val protoIdSignatureArray = arrayListOf<ProtoIdSignature>()

        fun serializeString(value: String): Int = protoStringMap.getOrPut(value) {
            protoStringArray.add(value)
            protoStringArray.size - 1
        }

        val idSignatureSerializer = IdSignatureSerializer(
                ::serializeString,
                ::serializeString,
                protoIdSignatureMap,
                protoIdSignatureArray
        )

        classFields.forEach {
            idSignatureSerializer.protoIdSignature(it.classSignature)
        }
        val signatures = IrMemoryArrayWriter(protoIdSignatureArray.map { it.toByteArray() }).writeIntoMemory()
        val signatureStrings = IrMemoryStringWriter(protoStringArray).writeIntoMemory()
        val stringTable = buildStringTable {
            classFields.forEach {
                +it.file.fqName
                +it.file.path
                it.fields.forEach { +it.name }
            }
        }
        val size = stringTable.sizeBytes + classFields.sumOf { Int.SIZE_BYTES * (6 + it.typeParameterSigs.size + it.fields.size * 4) }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        classFields.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
            stream.writeInt(protoIdSignatureMap[it.classSignature]!!)
            stream.writeIntArray(it.typeParameterSigs)
            stream.writeInt(it.outerThisIndex)
            stream.writeInt(it.fields.size)
            it.fields.forEach { field ->
                stream.writeInt(stringTable.indices[field.name]!!)
                stream.writeInt(field.binaryType)
                stream.writeInt(field.flags)
                stream.writeInt(field.alignment)
            }
        }
        return IrMemoryArrayWriter(listOf(signatures, signatureStrings, stream.buf)).writeIntoMemory()
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedClassFields>) {
        val reader = IrArrayMemoryReader(data)
        val signatures = IrArrayMemoryReader(reader.tableItemBytes(0))
        val signatureStrings = IrArrayMemoryReader(reader.tableItemBytes(1))
        val libFile: IrLibraryFile = object: IrLibraryFile() {
            override fun declaration(index: Int) = error("Declarations are not needed for IdSignature deserialization")
            override fun type(index: Int) = error("Types are not needed for IdSignature deserialization")
            override fun expressionBody(index: Int) = error("Expression bodies are not needed for IdSignature deserialization")
            override fun statementBody(index: Int) = error("Statement bodies are not needed for IdSignature deserialization")
            override fun fileEntry(index: Int) = error("File entries are not needed for IdSignature deserialization")

            override fun signature(index: Int): ProtoIdSignature {
                val bytes = signatures.tableItemBytes(index)
                return ProtoIdSignature.parseFrom(bytes.codedInputStream)
            }

            private fun deserializeString(index: Int): String = WobblyTF8.decode(signatureStrings.tableItemBytes(index))

            override fun string(index: Int): String = deserializeString(index)
            override fun debugInfo(index: Int): String = deserializeString(index)
        }
        val interner = IrInterningService()
        val stream = ByteArrayStream(reader.tableItemBytes(2))
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val signatureIndex = stream.readInt()
            val typeParameterSigs = stream.readIntArray()
            val outerThisIndex = stream.readInt()
            val fieldsCount = stream.readInt()
            val fields = Array(fieldsCount) {
                val name = stringTable[stream.readInt()]
                val binaryType = stream.readInt()
                val flags = stream.readInt()
                val alignment = stream.readInt()
                SerializedClassFieldInfo(name, binaryType, flags, alignment)
            }
            val fileSignature = IdSignature.FileSignature(
                    id = Any(),
                    fqName = FqName(fileFqName),
                    fileName = filePath
            )
            val idSignatureDeserializer = IdSignatureDeserializer(libFile, fileSignature, interner)
            val classSignature = idSignatureDeserializer.deserializeIdSignature(signatureIndex)
            result.add(SerializedClassFields(
                    SerializedFileReference(fileFqName, filePath), classSignature, typeParameterSigs, outerThisIndex, fields)
            )
        }
    }
}

class SerializedEagerInitializedFile(val file: SerializedFileReference)

internal object EagerInitializedPropertySerializer {
    fun serialize(properties: List<SerializedEagerInitializedFile>): ByteArray {
        val stringTable = buildStringTable {
            properties.forEach {
                +it.file.fqName
                +it.file.path
            }
        }
        val size = stringTable.sizeBytes + properties.sumOf { Int.SIZE_BYTES * 2 }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        properties.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedEagerInitializedFile>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            result.add(SerializedEagerInitializedFile(SerializedFileReference(fileFqName, filePath)))
        }
    }
}