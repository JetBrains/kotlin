/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.ClassLayoutBuilder
import org.jetbrains.kotlin.backend.konan.descriptors.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.descriptors.requiredAlignment
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.set

internal fun createLlvmDeclarations(generationState: NativeGenerationState, irModule: IrModuleFragment): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(generationState)
    irModule.acceptChildrenVoid(generator)
    return LlvmDeclarations(generator.uniques)
}

// Please note, that llvmName is part of the ABI, and cannot be liberally changed.
enum class UniqueKind(val llvmName: String) {
    UNIT("theUnitInstance"),
    EMPTY_ARRAY("theEmptyArray")
}

internal class LlvmDeclarations(private val unique: Map<UniqueKind, UniqueLlvmDeclarations>) {
    fun forFunction(function: IrFunction): LlvmCallable =
            forFunctionOrNull(function) ?: with(function) {
                error("$name in $file/${parent.fqNameForIrSerialization}")
            }

    fun forFunctionOrNull(function: IrFunction): LlvmCallable? =
            (function.metadata as? KonanMetadata.Function)?.llvm

    fun forClass(irClass: IrClass) =
            (irClass.metadata as? KonanMetadata.Class)?.llvm ?: error(irClass.render())

    fun forField(field: IrField) =
            (field.metadata as? KonanMetadata.InstanceField)?.llvm ?: error(field.render())

    fun forStaticField(field: IrField) =
            (field.metadata as? KonanMetadata.StaticField)?.llvm ?: error(field.render())

    fun forUnique(kind: UniqueKind) = unique[kind] ?: error("No unique $kind")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val typeInfoGlobal: StaticData.Global,
        val writableTypeInfoGlobal: StaticData.Global?,
        val typeInfo: ConstPointer,
        val objCDeclarations: KotlinObjCClassLlvmDeclarations?,
        val alignment: Int,
        val fieldIndices: Map<IrFieldSymbol, Int>
)

internal class KotlinObjCClassLlvmDeclarations(
        val classInfoGlobal: StaticData.Global,
        val bodyOffsetGlobal: StaticData.Global
)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef, val alignment: Int)

internal class StaticFieldLlvmDeclarations(val storageAddressAccess: AddressAccess, val alignment: Int)

internal class UniqueLlvmDeclarations(val pointer: ConstPointer)

internal data class ClassBodyAndAlignmentInfo(
        val body: LLVMTypeRef,
        val alignment: Int,
        val fieldsIndices: Map<IrFieldSymbol, Int>
)

private fun ContextUtils.createClassBody(name: String, fields: List<ClassLayoutBuilder.FieldInfo>): ClassBodyAndAlignmentInfo {
    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(llvm.module), name)!!
    val packed = fields.any { LLVMABIAlignmentOfType(runtime.targetData, it.type.toLLVMType(llvm)) != it.alignment }
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
            +"  $i: ${llvmtype2string(fieldTypes[i])} at offset ${LLVMOffsetOfElement(runtime.targetData, classType, i)}"
        }
        +"  Overall llvm alignment is ${LLVMABIAlignmentOfType(runtime.targetData, classType)}"
        +"  Overall required alignment is ${alignment}"
        +"  Overall size is ${LLVMABISizeOfType(runtime.targetData, classType)}"
        +"  Resulting type is ${llvmtype2string(classType)}"
    }

    return ClassBodyAndAlignmentInfo(classType, alignment, indices)
}

private class DeclarationsGeneratorVisitor(override val generationState: NativeGenerationState)
    : IrElementVisitorVoid, ContextUtils {

    val uniques = mutableMapOf<UniqueKind, UniqueLlvmDeclarations>()

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

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val internalName = qualifyInternalName(declaration)

        val fields = context.getLayoutBuilder(declaration).getFields(llvm)
        val (bodyType, alignment, fieldIndices) = createClassBody("kclassbody:$internalName", fields)

        require(alignment == runtime.objectAlignment) {
            "Over-aligned objects are not supported yet: expected alignment for ${declaration.fqNameWhenAvailable} is $alignment"
        }


        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (declaration.isExported()) {
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
                    LLVMArrayType(llvm.int8PtrType, context.getLayoutBuilder(declaration).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoSymbolName, declaration.isExported())

            // Other LLVM modules might import this global as a TypeInfo global.
            // This works only if there is no gap between the beginning of the global and the TypeInfo part,
            // which should always be the case, since it is the zeroth element.
            // Still, better be safe than sorry, checking this explicitly:
            val typeInfoOffsetInGlobal = LLVMOffsetOfElement(llvmTargetData, typeInfoWithVtableType, 0)
            check(typeInfoOffsetInGlobal == 0L) { "Offset for $typeInfoSymbolName TypeInfo is $typeInfoOffsetInGlobal" }

            if (context.config.producePerFileCache && declaration in generationState.constructedFromExportedInlineFunctions) {
                // This is required because internal inline functions can access private classes.
                // So, in the generated code, the class type info can be accessed outside the file.
                // With per-file caches involved, this can mean accessing from a different object file.
                // Therefore, the class type info has to have external linkage in that case.
                // Check e.g. `nestedInPrivateClass2.kt` test (it should fail if you remove setting linkage to external).
                //
                // This case should be reflected in the `isExported` parameter of `createGlobal` above, instead of
                // patching the linkage ad hoc.
                // The problem is: such globals in fact can have clashing names, which makes createGlobal fail.
                // See https://youtrack.jetbrains.com/issue/KT-61428.
                typeInfoGlobal.setLinkage(LLVMLinkage.LLVMExternalLinkage)
            }

            typeInfoPtr = typeInfoGlobal.pointer.getElementPtr(llvm, 0)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = declaration.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        if (declaration.isUnit() || declaration.isKotlinArray())
            createUniqueDeclarations(declaration, typeInfoPtr, bodyType)

        val objCDeclarations = if (declaration.isKotlinObjCClass()) {
            createKotlinObjCClassDeclarations(declaration)
        } else {
            null
        }

        val writableTypeInfoType = runtime.writableTypeInfoType
        val writableTypeInfoGlobal = if (writableTypeInfoType == null) {
            null
        } else if (declaration.isExported()) {
            val name = declaration.writableTypeInfoSymbolName
            staticData.createGlobal(writableTypeInfoType, name, isExported = true).also {
                it.setLinkage(LLVMLinkage.LLVMCommonLinkage) // Allows to be replaced by other bitcode module.
            }
        } else {
            staticData.createGlobal(writableTypeInfoType, "")
        }.also {
            it.setZeroInitializer()
        }

        return ClassLlvmDeclarations(bodyType, typeInfoGlobal, writableTypeInfoGlobal, typeInfoPtr, objCDeclarations, alignment, fieldIndices)
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

    private fun createKotlinObjCClassDeclarations(irClass: IrClass): KotlinObjCClassLlvmDeclarations {
        val internalName = qualifyInternalName(irClass)

        val isExported = irClass.isExported()
        val classInfoSymbolName = if (isExported) {
            irClass.kotlinObjCClassInfoSymbolName
        } else {
            "kobjcclassinfo:$internalName"
        }
        val classInfoGlobal = staticData.createGlobal(
                runtime.kotlinObjCClassInfo,
                classInfoSymbolName,
                isExported = isExported
        ).apply {
            setConstant(true)
        }

        val bodyOffsetGlobal = staticData.createGlobal(llvm.int32Type, "kobjcbodyoffs:$internalName")

        return KotlinObjCClassLlvmDeclarations(classInfoGlobal, bodyOffsetGlobal)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        // In some cases because of inconsistencies of previous lowerings, default values can be not removed.
        // If they contain class or function, they would not be processed by code generator
        // So we are skipping them here too.
    }

    private tailrec fun gcd(a: Long, b: Long) : Long = if (b == 0L) a else gcd(b, a % b)

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val containingClass = declaration.parent as? IrClass
        if (containingClass != null && !declaration.isStatic) {
            if (!containingClass.requiresRtti()) return
            val classDeclarations = (containingClass.metadata as? KonanMetadata.Class)?.llvm
                    ?: error(containingClass.render())
            val index = classDeclarations.fieldIndices[declaration.symbol]!!
            declaration.metadata = KonanMetadata.InstanceField(
                    declaration,
                    FieldLlvmDeclarations(
                            index,
                            classDeclarations.bodyType,
                            gcd(LLVMOffsetOfElement(llvm.runtime.targetData, classDeclarations.bodyType, index), llvm.runtime.objectAlignment.toLong()).toInt()
                    )
            )
        } else {
            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(declaration)
            val alignmnet = declaration.requiredAlignment(llvm)
            val storage = if (declaration.storageKind(context) == FieldStorageKind.THREAD_LOCAL) {
                addKotlinThreadLocal(name, declaration.type.toLLVMType(llvm), alignmnet)
            } else {
                addKotlinGlobal(name, declaration.type.toLLVMType(llvm), alignmnet, isExported = false)
            }

            declaration.metadata = KonanMetadata.StaticField(declaration, StaticFieldLlvmDeclarations(storage, alignmnet))
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (!declaration.isReal) return

        val llvmFunction = if (declaration.isExternal) {
            if (declaration.isTypedIntrinsic || declaration.isObjCBridgeBased()
                    // All call-sites to external accessors to interop properties
                    // are lowered by InteropLowering.
                    || (declaration.isAccessor && declaration.isFromInteropLibrary())
                    || declaration.annotations.hasAnnotation(RuntimeNames.cCall)) return

            val proto = LlvmFunctionProto(declaration, declaration.computeSymbolName(), this, LLVMLinkage.LLVMExternalLinkage)
            llvm.externalFunction(proto)
        } else {
            if (!declaration.shouldGenerateBody()) {
                return
            }
            val symbolName = if (declaration.isExported()) {
                declaration.computeSymbolName().also {
                    if (declaration.name.asString() != "main") {
                        assert(LLVMGetNamedFunction(llvm.module, it) == null) { it }
                    } else {
                        // As a workaround, allow `main` functions to clash because frontend accepts this.
                        // See [OverloadResolver.isTopLevelMainInDifferentFiles] usage.
                    }
                }
            } else {
                if (!context.config.producePerFileCache)
                    "${KonanBinaryInterface.MANGLE_FUN_PREFIX}:${qualifyInternalName(declaration)}"
                else {
                    val containerName = declaration.parentClassOrNull?.fqNameForIrSerialization?.asString()
                            ?: (generationState.cacheDeserializationStrategy as CacheDeserializationStrategy.SingleFile).filePath
                    declaration.computePrivateSymbolName(containerName)
                }
            }

            val proto = LlvmFunctionProto(declaration, symbolName, this, linkageOf(declaration))
            context.log {
                "Creating llvm function ${symbolName} for ${declaration.render()}"
            }
            proto.createLlvmFunction(context, llvm.module)
        }

        declaration.metadata = KonanMetadata.Function(declaration, llvmFunction)
    }
}

internal sealed class KonanMetadata(override val name: Name?, val konanLibrary: KotlinLibrary?) : MetadataSource {
    sealed class Declaration<T>(declaration: T)
        : KonanMetadata(declaration.metadata?.name, declaration.konanLibrary) where T : IrDeclaration, T : IrMetadataSourceOwner

    class Class(irClass: IrClass, val llvm: ClassLlvmDeclarations, val layoutBuilder: ClassLayoutBuilder) : Declaration<IrClass>(irClass)

    class Function(irFunction: IrFunction, val llvm: LlvmCallable) : Declaration<IrFunction>(irFunction)

    class InstanceField(irField: IrField, val llvm: FieldLlvmDeclarations) : Declaration<IrField>(irField)

    class StaticField(irField: IrField, val llvm: StaticFieldLlvmDeclarations) : Declaration<IrField>(irField)
}

