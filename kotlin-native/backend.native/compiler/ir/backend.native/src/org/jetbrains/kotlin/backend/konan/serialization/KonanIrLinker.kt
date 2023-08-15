/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinaryNameAndType
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.encodings.FunctionFlags
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.isFromInteropLibraryByDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.fir.lazy.Fir2IrLazyClass
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import sun.misc.Unsafe
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunction as ProtoFunction
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty

private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
    isAccessible = true
    return@with this.get(null) as Unsafe
}

private val byteArrayBaseOffset = unsafe.arrayBaseOffset(ByteArray::class.java).toLong()
private val charArrayBaseOffset = unsafe.arrayBaseOffset(CharArray::class.java).toLong()
private val intArrayBaseOffset = unsafe.arrayBaseOffset(IntArray::class.java).toLong()

internal class ByteArrayStream(val buf: ByteArray) {
    private var offset = 0

    fun hasData() = offset < buf.size

    fun readInt(): Int {
        checkSize(offset + Int.SIZE_BYTES) { "Can't read an int at $offset, size = ${buf.size}" }
        return unsafe.getInt(buf, byteArrayBaseOffset + offset).also { offset += Int.SIZE_BYTES }
    }

    fun writeInt(value: Int) {
        checkSize(offset + Int.SIZE_BYTES) { "Can't write an int at $offset, size = ${buf.size}" }
        unsafe.putInt(buf, byteArrayBaseOffset + offset, value).also { offset += Int.SIZE_BYTES }
    }

    fun readString(length: Int): String {
        checkSize(offset + Char.SIZE_BYTES * length) {
            "Can't read a string of length $length at $offset, size = ${buf.size}"
        }
        val chars = CharArray(length)
        unsafe.copyMemory(buf, byteArrayBaseOffset + offset, chars, charArrayBaseOffset, length * Char.SIZE_BYTES.toLong())
        offset += length * Char.SIZE_BYTES
        return String(chars)
    }

    fun writeString(string: String) {
        checkSize(offset + Char.SIZE_BYTES * string.length) {
            "Can't write a string of length ${string.length} at $offset, size = ${buf.size}"
        }
        unsafe.copyMemory(string.toCharArray(), charArrayBaseOffset, buf, byteArrayBaseOffset + offset, string.length * Char.SIZE_BYTES.toLong())
        offset += string.length * Char.SIZE_BYTES
    }

    fun readIntArray(): IntArray {
        val size = readInt()
        checkSize(offset + Int.SIZE_BYTES * size) {
            "Can't read an int array of size $size at $offset, size = ${buf.size}"
        }
        val array = IntArray(size)
        unsafe.copyMemory(buf, byteArrayBaseOffset + offset, array, intArrayBaseOffset, size * Int.SIZE_BYTES.toLong())
        offset += size * Int.SIZE_BYTES
        return array
    }

    fun writeIntArray(array: IntArray) {
        checkSize(offset + Int.SIZE_BYTES + Int.SIZE_BYTES * array.size) {
            "Can't write an int array of size ${array.size} at $offset, size = ${buf.size}"
        }
        unsafe.putInt(buf, byteArrayBaseOffset + offset, array.size).also { offset += Int.SIZE_BYTES }
        unsafe.copyMemory(array, intArrayBaseOffset, buf, byteArrayBaseOffset + offset, array.size * Int.SIZE_BYTES.toLong())
        offset += array.size * Int.SIZE_BYTES
    }

    private fun checkSize(at: Int, messageBuilder: () -> String) {
        if (at > buf.size) error(messageBuilder())
    }
}

data class SerializedFileReference(val fqName: String, val path: String) {
    constructor(irFile: IrFile) : this(irFile.packageFqName.asString(), irFile.path)
}

private class StringTableBuilder {
    private val indices = mutableMapOf<String, Int>()
    private var index = 0

    operator fun String.unaryPlus() {
        this@StringTableBuilder.indices.getOrPut(this) { index++ }
    }

    fun build() = StringTable(indices)
}

private inline fun buildStringTable(block: StringTableBuilder.() -> Unit): StringTable {
    val builder = StringTableBuilder()
    builder.block()
    return builder.build()
}

private class StringTable(val indices: Map<String, Int>) {
    val sizeBytes: Int get() = Int.SIZE_BYTES + indices.keys.sumOf { Int.SIZE_BYTES + it.length * Char.SIZE_BYTES }

    fun serialize(stream: ByteArrayStream) {
        val lengths = IntArray(indices.size)
        val strings = Array(indices.size) { "" }
        indices.forEach { (string, index) ->
            lengths[index] = string.length
            strings[index] = string
        }
        stream.writeIntArray(lengths)
        strings.forEach { stream.writeString(it) }
    }

    companion object {
        fun deserialize(stream: ByteArrayStream): Array<String> {
            val lengths = stream.readIntArray()
            return Array(lengths.size) { stream.readString(lengths[it]) }
        }
    }
}

class SerializedInlineFunctionReference(val file: SerializedFileReference, val functionSignature: Int, val body: Int,
                                        val startOffset: Int, val endOffset: Int,
                                        val extensionReceiverSig: Int, val dispatchReceiverSig: Int, val outerReceiverSigs: IntArray,
                                        val valueParameterSigs: IntArray, val typeParameterSigs: IntArray,
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
            Int.SIZE_BYTES * (12 + it.outerReceiverSigs.size + it.valueParameterSigs.size + it.typeParameterSigs.size + it.defaultValues.size)
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
            stream.writeIntArray(it.valueParameterSigs)
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
            val valueParameterSigs = stream.readIntArray()
            val typeParameterSigs = stream.readIntArray()
            val defaultValues = stream.readIntArray()
            result.add(SerializedInlineFunctionReference(
                    SerializedFileReference(fileFqName, filePath), functionSignature, body, startOffset, endOffset,
                    extensionReceiverSig, dispatchReceiverSig, outerReceiverSigs, valueParameterSigs,
                    typeParameterSigs, defaultValues)
            )
        }
    }
}

// [binaryType] is needed in case a field is of a private inline class type (which can't be deserialized).
// But it is safe to just set the field's type to the primitive type the inline class will be erased to.
class SerializedClassFieldInfo(val name: Int, val binaryType: Int, val type: Int, val flags: Int, val alignment: Int) {
    companion object {
        const val FLAG_IS_CONST = 1
    }
}

class SerializedClassFields(val file: SerializedFileReference, val classSignature: Int, val typeParameterSigs: IntArray,
                            val outerThisIndex: Int, val fields: Array<SerializedClassFieldInfo>)

internal object ClassFieldsSerializer {
    fun serialize(classFields: List<SerializedClassFields>): ByteArray {
        val stringTable = buildStringTable {
            classFields.forEach {
                +it.file.fqName
                +it.file.path
            }
        }
        val size = stringTable.sizeBytes + classFields.sumOf { Int.SIZE_BYTES * (6 + it.typeParameterSigs.size + it.fields.size * 5) }
        val stream = ByteArrayStream(ByteArray(size))
        stringTable.serialize(stream)
        classFields.forEach {
            stream.writeInt(stringTable.indices[it.file.fqName]!!)
            stream.writeInt(stringTable.indices[it.file.path]!!)
            stream.writeInt(it.classSignature)
            stream.writeIntArray(it.typeParameterSigs)
            stream.writeInt(it.outerThisIndex)
            stream.writeInt(it.fields.size)
            it.fields.forEach { field ->
                stream.writeInt(field.name)
                stream.writeInt(field.binaryType)
                stream.writeInt(field.type)
                stream.writeInt(field.flags)
                stream.writeInt(field.alignment)
            }
        }
        return stream.buf
    }

    fun deserializeTo(data: ByteArray, result: MutableList<SerializedClassFields>) {
        val stream = ByteArrayStream(data)
        val stringTable = StringTable.deserialize(stream)
        while (stream.hasData()) {
            val fileFqName = stringTable[stream.readInt()]
            val filePath = stringTable[stream.readInt()]
            val classSignature = stream.readInt()
            val typeParameterSigs = stream.readIntArray()
            val outerThisIndex = stream.readInt()
            val fieldsCount = stream.readInt()
            val fields = Array(fieldsCount) {
                val name = stream.readInt()
                val binaryType = stream.readInt()
                val type = stream.readInt()
                val flags = stream.readInt()
                val alignment = stream.readInt()
                SerializedClassFieldInfo(name, binaryType, type, flags, alignment)
            }
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

internal fun ProtoClass.findClass(irClass: IrClass, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoClass {
    val signature = irClass.symbol.signature ?: error("No signature for ${irClass.render()}")
    var result: ProtoClass? = null

    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        val childClass = when {
            child.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_CLASS -> child.irClass
            child.declaratorCase == ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY
                    && child.irEnumEntry.hasCorrespondingClass() -> child.irEnumEntry.correspondingClass
            else -> continue
        }

        val name = fileReader.string(childClass.name)
        if (name == irClass.name.asString()) {
            if (result == null)
                result = childClass
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childClass
            }
        }
    }
    return result ?: error("Class ${irClass.render()} is not found")
}

internal fun ProtoClass.findProperty(irProperty: IrProperty, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoProperty {
    val signature = irProperty.symbol.signature ?: error("No signature for ${irProperty.render()}")
    var result: ProtoProperty? = null

    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        if (child.declaratorCase != ProtoDeclaration.DeclaratorCase.IR_PROPERTY) continue
        val childProperty = child.irProperty

        val name = fileReader.string(child.irProperty.name)
        if (name == irProperty.name.asString()) {
            if (result == null)
                result = childProperty
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childProperty
            }
        }
    }
    return result ?: error("Property ${irProperty.render()} is not found")
}

internal fun ProtoProperty.findAccessor(irProperty: IrProperty, irFunction: IrSimpleFunction): ProtoFunction {
    if (irFunction == irProperty.getter)
        return getter
    require(irFunction == irProperty.setter) { "Accessor should be either a getter or a setter. ${irFunction.render()}" }
    return setter
}

internal fun ProtoClass.findInlineFunction(irFunction: IrFunction, fileReader: IrLibraryFile, symbolDeserializer: IrSymbolDeserializer): ProtoFunction {
    (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.let { irProperty ->
        return findProperty(irProperty, fileReader, symbolDeserializer).findAccessor(irProperty, irFunction)
    }

    val signature = irFunction.symbol.signature ?: error("No signature for ${irFunction.render()}")
    var result: ProtoFunction? = null
    for (i in 0 until this.declarationCount) {
        val child = this.getDeclaration(i)
        if (child.declaratorCase != ProtoDeclaration.DeclaratorCase.IR_FUNCTION) continue
        val childFunction = child.irFunction
        if (childFunction.base.valueParameterCount != irFunction.valueParameters.size) continue
        if (childFunction.base.hasExtensionReceiver() xor (irFunction.extensionReceiverParameter != null)) continue
        if (childFunction.base.hasDispatchReceiver() xor (irFunction.dispatchReceiverParameter != null)) continue
        if (!FunctionFlags.decode(childFunction.base.base.flags).isInline) continue

        val nameAndType = BinaryNameAndType.decode(childFunction.base.nameType)
        val name = fileReader.string(nameAndType.nameIndex)
        if (name == irFunction.name.asString()) {
            if (result == null)
                result = childFunction
            else {
                val resultIdSignature = symbolDeserializer.deserializeIdSignature(BinarySymbolData.decode(result.base.base.symbol).signatureId)
                if (resultIdSignature == signature)
                    return result
                result = childFunction
            }
        }
    }
    return result ?: error("Function ${irFunction.render()} is not found")
}

object KonanFakeOverrideClassFilter : FakeOverrideClassFilter {
    private fun IdSignature.isInteropSignature(): Boolean = with(this) {
        IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrClassSymbol.isInterop(): Boolean {
        if (this is IrPublicSymbolBase<*> && this.signature.isInteropSignature()) return true

        // K2 doesn't properly put signatures into such symbols yet, workaround:
        return this.isBound && this.owner is Fir2IrLazyClass && this.owner.isFromInteropLibraryByDescriptor()
    }

    // This is an alternative to .isObjCClass that doesn't need to walk up all the class heirarchy,
    // rather it only looks at immediate super class symbols.
    private fun IrClass.hasInteropSuperClass() = this.superTypes
            .mapNotNull { it.classOrNull }
            .any { it.isInterop() }

    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean {
        return !clazz.hasInteropSuperClass() && clazz !is IrLazyClass
    }
}

internal data class DeserializedInlineFunction(val firstAccess: Boolean, val function: InlineFunctionOriginInfo)

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class KonanIrLinker(
        private val currentModule: ModuleDescriptor,
        override val translationPluginContext: TranslationPluginContext?,
        messageLogger: IrMessageLogger,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        friendModules: Map<String, Collection<String>>,
        private val forwardModuleDescriptor: ModuleDescriptor?,
        private val stubGenerator: DeclarationStubGenerator,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        exportedDependencies: List<ModuleDescriptor>,
        override val partialLinkageSupport: PartialLinkageSupportForLinker,
        private val cachedLibraries: CachedLibraries,
        private val lazyIrForCaches: Boolean,
        private val libraryBeingCached: PartialCacheInfo?,
        override val userVisibleIrModulesSupport: UserVisibleIrModulesSupport
) : KotlinIrLinker(currentModule, messageLogger, builtIns, symbolTable, exportedDependencies) {

    companion object {
        const val INVALID_INDEX = -1
        val FORWARD_DECLARATION_ORIGIN = object : IrDeclarationOriginImpl("FORWARD_DECLARATION_ORIGIN") {}
    }

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isNativeStdlib()

    private val forwardDeclarationDeserializer = forwardModuleDescriptor?.let { KonanForwardDeclarationModuleDeserializer(it) }

    override val fakeOverrideBuilder = FakeOverrideBuilder(
            linker = this,
            symbolTable = symbolTable,
            mangler = KonanManglerIr,
            typeSystem = IrTypeSystemContextImpl(builtIns),
            friendModules = friendModules,
            partialLinkageSupport = partialLinkageSupport,
            platformSpecificClassFilter = KonanFakeOverrideClassFilter
    )

    val moduleDeserializers = mutableMapOf<ModuleDescriptor, KonanPartialModuleDeserializer>()
    val klibToModuleDeserializerMap = mutableMapOf<KotlinLibrary, KonanPartialModuleDeserializer>()

    fun getCachedDeclarationModuleDeserializer(declaration: IrDeclaration): KonanPartialModuleDeserializer? {
        val packageFragment = declaration.getPackageFragment()
        val moduleDescriptor = packageFragment.packageFragmentDescriptor.containingDeclaration
        val klib = packageFragment.konanLibrary
        val declarationBeingCached = packageFragment is IrFile && klib != null && libraryBeingCached?.klib == klib
                && libraryBeingCached.strategy.contains(packageFragment.path)
        return if (klib != null && !moduleDescriptor.isFromInteropLibrary()
                && cachedLibraries.isLibraryCached(klib) && !declarationBeingCached)
            moduleDeserializers[moduleDescriptor] ?: error("No module deserializer for ${declaration.render()}")
        else null
    }

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: KotlinLibrary?, strategyResolver: (String) -> DeserializationStrategy) =
            when {
                moduleDescriptor === forwardModuleDescriptor -> {
                    forwardDeclarationDeserializer ?: error("forward declaration deserializer expected")
                }
                klib == null -> {
                    error("Expecting kotlin library for $moduleDescriptor")
                }
                klib.isInteropLibrary() -> {
                    KonanInteropModuleDeserializer(
                            moduleDescriptor,
                            klib,
                            listOfNotNull(forwardDeclarationDeserializer),
                            cachedLibraries.isLibraryCached(klib),
                            cenumsProvider,
                            stubGenerator,
                            builtIns
                    )
                }
                else -> {
                    val deserializationStrategy = when {
                        klib == libraryBeingCached?.klib -> libraryBeingCached.strategy
                        lazyIrForCaches && cachedLibraries.isLibraryCached(klib) -> CacheDeserializationStrategy.Nothing
                        else -> CacheDeserializationStrategy.WholeModule
                    }
                    KonanPartialModuleDeserializer(
                            this, moduleDescriptor, klib, stubGenerator, cachedLibraries, inlineFunctionFiles, strategyResolver, deserializationStrategy
                    ).also {
                        moduleDeserializers[moduleDescriptor] = it
                        klibToModuleDeserializerMap[klib] = it
                    }
                }
            }

    override fun postProcess(inOrAfterLinkageStep: Boolean) {
        stubGenerator.unboundSymbolGeneration = true
        super.postProcess(inOrAfterLinkageStep)
    }

    private val inlineFunctionFiles = mutableMapOf<IrExternalPackageFragment, IrFile>()

    override fun getFileOf(declaration: IrDeclaration): IrFile {
        val packageFragment = declaration.getPackageFragment()
        return packageFragment as? IrFile
                ?: inlineFunctionFiles[packageFragment as IrExternalPackageFragment]
                ?: error("Unknown external package fragment: ${packageFragment.packageFragmentDescriptor}")
    }

    fun getExternalDeclarationFileName(declaration: IrDeclaration) = when (val packageFragment = declaration.getPackageFragment()) {
        is IrFile -> packageFragment.path

        is IrExternalPackageFragment -> {
            val moduleDescriptor = packageFragment.packageFragmentDescriptor.containingDeclaration
            val moduleDeserializer = moduleDeserializers[moduleDescriptor] ?: error("No module deserializer for $moduleDescriptor")
            moduleDeserializer.getFileNameOf(declaration)
        }

        else -> error("Unknown package fragment kind ${packageFragment::class.java}")
    }

    private inner class KonanForwardDeclarationModuleDeserializer(moduleDescriptor: ModuleDescriptor) : IrModuleDeserializer(moduleDescriptor, KotlinAbiVersion.CURRENT) {
        init {
            require(moduleDescriptor.isForwardDeclarationModule)
        }

        private val declaredDeclaration = mutableMapOf<IdSignature, IrClass>()

        private fun IdSignature.isForwardDeclarationSignature(): Boolean {
            if (isPubliclyVisible) {
                return packageFqName() in NativeForwardDeclarationKind.packageFqNameToKind
            }

            return false
        }

        override fun contains(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

        private fun resolveDescriptor(idSig: IdSignature): ClassDescriptor? =
                with(idSig as IdSignature.CommonSignature) {
                    val classId = ClassId(packageFqName(), FqName(declarationFqName), false)
                    moduleDescriptor.findClassAcrossModuleDependencies(classId)
                }

        private fun buildForwardDeclarationStub(descriptor: ClassDescriptor): IrClass {
            return stubGenerator.generateClassStub(descriptor).also {
                it.origin = FORWARD_DECLARATION_ORIGIN
            }
        }

        override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
            require(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) {
                "Only class could be a Forward declaration $idSig (kind $symbolKind)"
            }
            val descriptor = resolveDescriptor(idSig) ?: return null
            val actualModule = descriptor.module
            if (actualModule !== moduleDescriptor) {
                val moduleDeserializer = resolveModuleDeserializer(actualModule, idSig)
                moduleDeserializer.addModuleReachableTopLevel(idSig)
                return symbolTable.referenceClass(idSig)
            }

            return declaredDeclaration.getOrPut(idSig) { buildForwardDeclarationStub(descriptor) }.symbol
        }

        override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No descriptor found for $idSig")

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns)
        override val moduleDependencies: Collection<IrModuleDeserializer> = emptyList()

        override val kind get() = IrModuleDeserializerKind.SYNTHETIC
    }

    private val String.isForwardDeclarationModuleName: Boolean get() = this == FORWARD_DECLARATIONS_MODULE_NAME.asString()

    val modules: Map<String, IrModuleFragment>
        get() = mutableMapOf<String, IrModuleFragment>().apply {
            deserializersForModules
                    .filter { !it.key.isForwardDeclarationModuleName && it.value.moduleDescriptor !== currentModule }
                    .forEach {
                        val klib = it.value.klib as? KotlinLibrary ?: error("Expected to be KotlinLibrary (${it.key})")
                        this[klib.libraryName] = it.value.moduleFragment
                    }
        }
}
