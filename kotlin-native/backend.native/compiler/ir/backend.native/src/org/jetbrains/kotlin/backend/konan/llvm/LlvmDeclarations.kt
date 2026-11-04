/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*
import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cgen.isCFunctionOrGlobalAccessor
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfo
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.generateWritableTypeInfoForClass
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

private tailrec fun gcd(a: Long, b: Long) : Long = if (b == 0L) a else gcd(b, a % b)

internal fun createLlvmDeclarations(generationState: NativeGenerationState, irModule: IrModuleFragment) {
    val uniques = mutableMapOf<UniqueKind, UniqueLlvmDeclarations>()
    // Make sure, that `llvmDeclarations` are accessible, if `DeclarationsGeneratorVisitor` needs to recurse into it.
    generationState.llvmDeclarations = LlvmDeclarations(generationState, uniques)
    val generator = DeclarationsGeneratorVisitor(generationState, uniques)
    irModule.acceptChildrenVoid(generator)
}

// Please note, that llvmName is part of the ABI, and cannot be liberally changed.
enum class UniqueKind(val llvmName: String) {
    UNIT("theUnitInstance"),
    EMPTY_ARRAY("theEmptyArray")
}

internal class LlvmDeclarations(
        override val generationState: NativeGenerationState,
        private val uniquesInCurrentModule: Map<UniqueKind, UniqueLlvmDeclarations>,
) : ContextUtils {

    private val externalFunctions: MutableMap<IrSimpleFunction, LlvmFunction> = HashMap()
    private val externalClasses: MutableMap<IrClass, ExternalClassLlvmDeclarations> = HashMap()

    fun forFunctionOrNull(function: IrSimpleFunction): LlvmFunction? {
        assert(function.isReal) {
            function.computeFullName()
        }
        return if (isExternal(function)) {
            externalFunctions.getOrPut(function) {
                val symbolName = function.computeSymbolName(context, forImplementation = true)
                val proto = LlvmFunctionProto(function, symbolName, this, LLVMLinkage.LLVMExternalLinkage)
                llvm.externalFunction(proto)
            }
        } else {
            (function.metadata as? KonanMetadata.Function)?.llvm
        }
    }

    fun forClass(irClass: IrClass): ClassLlvmDeclarations {
        return if (isExternal(irClass)) {
            externalClasses.getOrPut(irClass) {
                generationState.dependenciesTracker.add(irClass)

                val body = createClassBody(irClass.computePrivateClassBodyTypeName(irClass.file.path), irClass)
                val writableTypeInfo = generateWritableTypeInfoForClass(irClass)
                val typeInfoSymbolName = if (KonanBinaryInterface.isExported(irClass)) {
                    irClass.computeTypeInfoSymbolName()
                } else {
                    irClass.computePrivateTypeInfoSymbolName(irClass.file.path)
                }
                val typeInfo = constPointer(importGlobal(typeInfoSymbolName, runtime.typeInfoType))
                val objCDeclarations = generateKotlinObjCClassLlvmDeclarations(irClass)
                ExternalClassLlvmDeclarations(
                        body,
                        writableTypeInfo,
                        typeInfo,
                        objCDeclarations,
                )
            }
        } else {
            (irClass.metadata as? KonanMetadata.Class)?.llvm ?: error(irClass.render())
        }
    }

    fun forField(field: IrField): FieldLlvmDeclarations {
        require(!field.isStatic)
        val containingClass = field.parent
        require(containingClass is IrClass)
        return if (isExternal(containingClass)) {
            generateInstanceFieldDeclaration(forClass(containingClass), field)
        } else {
            (field.metadata as? KonanMetadata.InstanceField)?.llvm ?: error(field.render())
        }
    }

    fun forStaticField(field: IrField) =
            (field.metadata as? KonanMetadata.StaticField)?.llvm ?: error(field.render())

    fun forUnique(kind: UniqueKind): UniqueLlvmDeclarations {
        val descriptor = when (kind) {
            UniqueKind.UNIT -> context.irBuiltIns.unitClass.owner
            UniqueKind.EMPTY_ARRAY -> context.irBuiltIns.arrayClass.owner
        }
        return if (isExternal(descriptor)) {
            generationState.dependenciesTracker.add(descriptor)
            val pointer = constPointer(importGlobal(kind.llvmName, runtime.objHeaderType))
            UniqueLlvmDeclarations(pointer)
        } else {
            uniquesInCurrentModule[kind] ?: error("No unique $kind")
        }
    }
}

internal class ObjectBodyType(val llvmBodyType: LLVMTypeRef, objectFieldIndices: List<Int>) {
    val sortedIndicesOfObjectFields = objectFieldIndices.sorted()
}

internal sealed interface ClassLlvmDeclarations {
    val body: ClassBodyAndAlignmentInfo
    val writableTypeInfo: WritableTypeInfo?
    val typeInfo: ConstPointer
    val objCDeclarations: KotlinObjCClassLlvmDeclarations?
}

internal class DefinedClassLlvmDeclarations(
        override val body: ClassBodyAndAlignmentInfo,
        val typeInfoGlobal: StaticData.Global,
        override val writableTypeInfo: WritableTypeInfo?,
        override val typeInfo: ConstPointer,
        override val objCDeclarations: KotlinObjCClassLlvmDeclarations?,
) : ClassLlvmDeclarations

internal class ExternalClassLlvmDeclarations(
        override val body: ClassBodyAndAlignmentInfo,
        override val writableTypeInfo: WritableTypeInfo?,
        override val typeInfo: ConstPointer,
        override val objCDeclarations: KotlinObjCClassLlvmDeclarations?
) : ClassLlvmDeclarations

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef, val alignment: Int)

internal class StaticFieldLlvmDeclarations(val storageAddressAccess: AddressAccess, val alignment: Int)

internal class UniqueLlvmDeclarations(val pointer: ConstPointer)

internal data class ClassBodyAndAlignmentInfo(
        val objectBody: ObjectBodyType,
        val alignment: Int,
        val fieldsIndices: Map<IrFieldSymbol, Int>
) {
    val llvmBodyType by objectBody::llvmBodyType
}

private fun ContextUtils.createClassBody(name: String, irClass: IrClass): ClassBodyAndAlignmentInfo {
    val fields = context.getLayoutBuilder(irClass).getPackedFields(llvm)
    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(llvm.module), name)!!
    val packed = context.config.packFields ||
        fields.any { LLVMABIAlignmentOfType(runtime.targetData, it.type.toLLVMType(llvm)) != it.alignment }
    val alignment = maxOf(runtime.objectAlignment, fields.maxOfOrNull { it.alignment } ?: 0)
    val indices = mutableMapOf<IrFieldSymbol, Int>()

    val fieldTypes = buildList {
        var currentOffset = 0L
        fun addAndCount(type: LLVMTypeRef) {
            add(type)
            currentOffset += LLVMStoreSizeOfType(runtime.targetData, type)
        }
        addAndCount(runtime.objHeaderType)
        for (field in fields) {
            if (packed) {
                val offset = (currentOffset % field.alignment).toInt()
                if (offset != 0) {
                    val toInsert = field.alignment - offset
                    addAndCount(LLVMArrayType(llvm.int8Type, toInsert)!!)
                }
                require(currentOffset % field.alignment == 0L)
            }
            indices[field.irFieldSymbol] = this.size
            addAndCount(field.type.toLLVMType(llvm))
        }
    }
    LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, if (packed) 1 else 0)

    context.logMultiple {
        +"$name has following fields:"
        for (i in fieldTypes.indices) {
            +"  $i: ${fieldTypes[i].toTypeString()} at offset ${LLVMOffsetOfElement(runtime.targetData, classType, i)}"
        }
        +"  Overall llvm alignment is ${LLVMABIAlignmentOfType(runtime.targetData, classType)}"
        +"  Overall required alignment is ${alignment}"
        +"  Overall size is ${LLVMABISizeOfType(runtime.targetData, classType)}"
        +"  Resulting type is ${classType.toTypeString()}"
    }

    check(alignment == runtime.objectAlignment) {
        "Over-aligned objects are not supported yet: expected alignment for ${irClass.fqNameWhenAvailable} is $alignment"
    }

    val objectFieldIndices = fields.mapNotNull {
        if (it.type.binaryTypeIsReference()) {
            indices.getValue(it.irFieldSymbol)
        } else {
            null
        }
    }

    return ClassBodyAndAlignmentInfo(ObjectBodyType(classType, objectFieldIndices), alignment, indices)
}

private fun ContextUtils.generateInstanceFieldDeclaration(containingClass: ClassLlvmDeclarations, irField: IrField): FieldLlvmDeclarations {
    val index = containingClass.body.fieldsIndices[irField.symbol]!!
    val bodyType = containingClass.body.llvmBodyType
    return FieldLlvmDeclarations(
            index,
            bodyType,
            gcd(LLVMOffsetOfElement(llvm.runtime.targetData, bodyType, index), llvm.runtime.objectAlignment.toLong()).toInt()
    )
}

private class DeclarationsGeneratorVisitor(
        override val generationState: NativeGenerationState,
        val uniques: MutableMap<UniqueKind, UniqueLlvmDeclarations>,
) : IrVisitorVoid(), ContextUtils {

    class Namer(val prefix: String) {
        private val names = mutableMapOf<IrDeclaration, Name>()
        private val counts = mutableMapOf<FqName, Int>()

        fun getName(parent: FqName, declaration: IrDeclaration): Name {
            return names.getOrPut(declaration) {
                val count = counts.getOrDefault(parent, 0) + 1
                counts[parent] = count
                Name.identifier(prefix + count)
            }
        }
    }

    private val objectNamer = Namer("object-")

    private fun getLocalName(parent: FqName, declaration: IrDeclaration): Name {
        if (declaration.isAnonymousObject) {
            return objectNamer.getName(parent, declaration)
        }

        return declaration.getNameWithAssert()
    }

    private fun getFqName(declaration: IrDeclaration): FqName {
        val parent = declaration.parent
        val parentFqName = when (parent) {
            is IrPackageFragment -> parent.packageFqName
            is IrDeclaration -> getFqName(parent)
            else -> error(parent)
        }

        val localName = getLocalName(parentFqName, declaration)
        return parentFqName.child(localName)
    }

    /**
     * Produces the name to be used for non-exported LLVM declarations corresponding to [declaration].
     *
     * Note: since these declarations are going to be private, the name is only required not to clash with any
     * exported declarations.
     */
    private fun qualifyInternalName(declaration: IrDeclaration): String {
        return getFqName(declaration).asString() + "#internal"
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.requiresRtti()) {
            val classLlvmDeclarations = createClassDeclarations(declaration)
            declaration.metadata = KonanMetadata.Class(declaration, classLlvmDeclarations, context.getLayoutBuilder(declaration))
        }
        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): DefinedClassLlvmDeclarations {
        val internalName = qualifyInternalName(declaration)

        val body = createClassBody("${KonanBinaryInterface.MANGLE_CLASS_BODY_PREFIX}:$internalName", declaration)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (declaration.isExported) {
            declaration.computeTypeInfoSymbolName()
        } else {
            if (!context.config.producePerFileCache)
                "${KonanBinaryInterface.MANGLE_CLASS_PREFIX}:$internalName"
            else {
                val containerName = (generationState.cacheDeserializationStrategy as CacheDeserializationStrategy.SingleFile).filePath
                declaration.computePrivateTypeInfoSymbolName(containerName)
            }
        }

        if (declaration.typeInfoHasVtableAttached) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoWithVtableType = llvm.structType(
                    runtime.typeInfoType,
                    LLVMArrayType(llvm.pointerType, context.getLayoutBuilder(declaration).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(
                    typeInfoWithVtableType, typeInfoSymbolName,
                    declaration.isExported
                            // This is required because internal inline functions can access private classes.
                            // So, in the generated code, the class type info can be accessed outside the file.
                            // With per-file caches involved, this can mean accessing from a different object file.
                            // Therefore, the class type info has to have external linkage in that case.
                            // Hopefully, this can be removed after fixing KT-69666.
                            || context.config.producePerFileCache && declaration.isConstructedFromExportedInlineFunctions
            )

            // Other LLVM modules might import this global as a TypeInfo global.
            // This works only if there is no gap between the beginning of the global and the TypeInfo part,
            // which should always be the case, since it is the zeroth element.
            // Still, better be safe than sorry, checking this explicitly:
            val typeInfoOffsetInGlobal = LLVMOffsetOfElement(llvmTargetData, typeInfoWithVtableType, 0)
            check(typeInfoOffsetInGlobal == 0L) { "Offset for $typeInfoSymbolName TypeInfo is $typeInfoOffsetInGlobal" }

            typeInfoPtr = typeInfoGlobal.pointer.getElementPtr(llvm, typeInfoWithVtableType, 0)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = declaration.isExported)

            typeInfoPtr = typeInfoGlobal.pointer
        }

        if (declaration.isUnit() || declaration.isKotlinArray())
            createUniqueDeclarations(declaration, typeInfoPtr, body.llvmBodyType)

        val objCDeclarations = generateKotlinObjCClassLlvmDeclarations(declaration, ::qualifyInternalName)

        val writableTypeInfo = generateWritableTypeInfoForClass(declaration)

        return DefinedClassLlvmDeclarations(
                body,
                typeInfoGlobal,
                writableTypeInfo,
                typeInfoPtr,
                objCDeclarations,
        )
    }

    private fun createUniqueDeclarations(
            irClass: IrClass, typeInfoPtr: ConstPointer, bodyType: LLVMTypeRef) {
        when {
                irClass.isUnit() -> {
                    uniques[UniqueKind.UNIT] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.UNIT, bodyType, typeInfoPtr))
                }
                irClass.isKotlinArray() -> {
                    uniques[UniqueKind.EMPTY_ARRAY] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.EMPTY_ARRAY, bodyType, typeInfoPtr))
                }
                else -> TODO("Unsupported unique $irClass")
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        // In some cases because of inconsistencies of previous lowerings, default values can be not removed.
        // If they contain class or function, they would not be processed by code generator
        // So we are skipping them here too.
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val containingClass = declaration.parent as? IrClass
        if (containingClass != null && !declaration.isStatic) {
            if (!containingClass.requiresRtti()) return
            val classDeclarations = (containingClass.metadata as? KonanMetadata.Class)?.llvm
                    ?: error(containingClass.render())
            val fieldDeclarations = generateInstanceFieldDeclaration(classDeclarations, declaration)
            declaration.metadata = KonanMetadata.InstanceField(
                    declaration,
                    fieldDeclarations,
            )
        } else {
            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(declaration)
            val alignmnet = declaration.requiredAlignment(llvm)
            val storage = if (declaration.storageKind == FieldStorageKind.THREAD_LOCAL) {
                addKotlinThreadLocal(name, declaration.type.toLLVMType(llvm), alignmnet, declaration.type.binaryTypeIsReference())
            } else {
                addKotlinGlobal(name, declaration.type.toLLVMType(llvm), alignmnet, isExported = false)
            }

            declaration.metadata = KonanMetadata.StaticField(declaration, StaticFieldLlvmDeclarations(storage, alignmnet))
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        super.visitSimpleFunction(declaration)

        if (!declaration.isReal) return

        val llvmFunction = if (declaration.isExternal) {
            if (declaration.isTypedIntrinsic || declaration.isObjCBridgeBased()
                    // All call-sites to external accessors to interop properties
                    // are lowered by InteropLowering.
                    || (declaration.isAccessor && declaration.moduleFragment.kotlinLibrary?.isCInteropLibrary() == true)
                    || declaration.isCFunctionOrGlobalAccessor()) return

            val symbolName = declaration.computeSymbolName(context, forImplementation = true)
            val proto = LlvmFunctionProto(declaration, symbolName, this, LLVMLinkage.LLVMExternalLinkage)
            llvm.externalFunction(proto)
        } else {
            if (!declaration.shouldGenerateBody())
                return

            val symbolName = declaration.computeSymbolName(context, forImplementation = true) {
                runUnless(context.config.producePerFileCache) {
                    "${KonanBinaryInterface.MANGLE_FUN_PREFIX}:${qualifyInternalName(declaration)}"
                }
            }
            if (declaration.isExported) {
                if (declaration.name.asString() != "main") {
                    assert(LLVMGetNamedFunction(llvm.module, symbolName) == null) {
                        "Function `$symbolName` is already defined. New definition is required for ${declaration.render()}"
                    }
                } else {
                    // As a workaround, allow `main` functions to clash because frontend accepts this.
                    // See [OverloadResolver.isTopLevelMainInDifferentFiles] usage.
                }
            }

            val proto = LlvmFunctionProto(declaration, symbolName, this, linkageOf(declaration))
            context.log {
                "Creating llvm function $symbolName for ${declaration.render()}"
            }
            proto.createLlvmFunction(context, llvm.module)
        }

        declaration.metadata = KonanMetadata.Function(declaration, llvmFunction)
    }
}

internal sealed class KonanMetadata(override val name: Name?) : MetadataSource {
    sealed class Declaration<T>(declaration: T)
        : KonanMetadata(declaration.metadata?.name) where T : IrDeclaration, T : IrMetadataSourceOwner

    class Class(irClass: IrClass, val llvm: ClassLlvmDeclarations, val layoutBuilder: ClassLayoutBuilder) : Declaration<IrClass>(irClass)

    class Function(irFunction: IrSimpleFunction, val llvm: LlvmFunction) : Declaration<IrSimpleFunction>(irFunction)

    class InstanceField(irField: IrField, val llvm: FieldLlvmDeclarations) : Declaration<IrField>(irField)

    class StaticField(irField: IrField, val llvm: StaticFieldLlvmDeclarations) : Declaration<IrField>(irField)
}

