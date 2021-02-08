/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun createLlvmDeclarations(context: Context): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(context)
    context.ir.irModule.acceptChildrenVoid(generator)
    return LlvmDeclarations(generator.uniques)
}

// Please note, that llvmName is part of the ABI, and cannot be liberally changed.
enum class UniqueKind(val llvmName: String) {
    UNIT("theUnitInstance"),
    EMPTY_ARRAY("theEmptyArray")
}

internal class LlvmDeclarations(private val unique: Map<UniqueKind, UniqueLlvmDeclarations>) {
    fun forFunction(function: IrFunction) = forFunctionOrNull(function) ?: with(function){error("$name in $file/${parent.fqNameForIrSerialization}")}
    fun forFunctionOrNull(function: IrFunction) = (function.metadata as? CodegenFunctionMetadata)?.llvm

    fun forClass(irClass: IrClass) = (irClass.metadata as? CodegenClassMetadata)?.llvm ?:
            error(irClass.descriptor.toString())

    fun forField(field: IrField) = (field.metadata as? CodegenInstanceFieldMetadata)?.llvm ?:
            error(field.descriptor.toString())

    fun forStaticField(field: IrField) = (field.metadata as? CodegenStaticFieldMetadata)?.llvm ?:
            error(field.descriptor.toString())

    fun forSingleton(irClass: IrClass) = forClass(irClass).singletonDeclarations ?:
            error(irClass.descriptor.toString())

    fun forUnique(kind: UniqueKind) = unique[kind] ?: error("No unique $kind")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val typeInfoGlobal: StaticData.Global,
        val writableTypeInfoGlobal: StaticData.Global?,
        val typeInfo: ConstPointer,
        val singletonDeclarations: SingletonLlvmDeclarations?,
        val objCDeclarations: KotlinObjCClassLlvmDeclarations?)

internal class SingletonLlvmDeclarations(val instanceStorage: AddressAccess)

internal class KotlinObjCClassLlvmDeclarations(
        val classInfoGlobal: StaticData.Global,
        val bodyOffsetGlobal: StaticData.Global
)

internal class FunctionLlvmDeclarations(val llvmFunction: LLVMValueRef)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef)

internal class StaticFieldLlvmDeclarations(val storageAddressAccess: AddressAccess)

internal class UniqueLlvmDeclarations(val pointer: ConstPointer)

private fun ContextUtils.createClassBodyType(name: String, fields: List<IrField>): LLVMTypeRef {
    val fieldTypes = listOf(runtime.objHeaderType) + fields.map { getLLVMType(it.type) }
    // TODO: consider adding synthetic ObjHeader field to Any.

    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!

    // LLVMStructSetBody expects the struct to be properly aligned and will insert padding accordingly. In our case
    // `allocInstance` returns 16x + 8 address, i.e. always misaligned for vector types. Workaround is to use packed struct.
    val hasBigAlignment = fields.any { LLVMABIAlignmentOfType(context.llvm.runtime.targetData, getLLVMType(it.type)) > 8 }
    val packed = if (hasBigAlignment) 1 else 0
    LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, packed)

    return classType
}

private class DeclarationsGeneratorVisitor(override val context: Context) :
        IrElementVisitorVoid, ContextUtils {

    val uniques = mutableMapOf<UniqueKind, UniqueLlvmDeclarations>()

    private class Namer(val prefix: String) {
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

    val objectNamer = Namer("object-")

    private fun getLocalName(parent: FqName, declaration: IrDeclaration): Name {
        if (declaration.isAnonymousObject) {
            return objectNamer.getName(parent, declaration)
        }

        return declaration.nameForIrSerialization
    }

    private fun getFqName(declaration: IrDeclaration): FqName {
        val parent = declaration.parent
        val parentFqName = when (parent) {
            is IrPackageFragment -> parent.fqName
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
            val metadata = declaration.metadata as? CodegenClassMetadata
                    ?: CodegenClassMetadata(declaration).also { declaration.metadata = it }
            metadata.llvm = classLlvmDeclarations
        }
        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val internalName = qualifyInternalName(declaration)

        val fields = context.getLayoutBuilder(declaration).fields
        val bodyType = createClassBodyType("kclassbody:$internalName", fields)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (declaration.isExported()) {
            declaration.computeTypeInfoSymbolName()
        } else {
            "ktype:$internalName"
        }

        if (declaration.typeInfoHasVtableAttached) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoGlobalName = "ktypeglobal:$internalName"

            val typeInfoWithVtableType = structType(
                    runtime.typeInfoType,
                    LLVMArrayType(int8TypePtr, context.getLayoutBuilder(declaration).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoGlobalName, isExported = false)

            val llvmTypeInfoPtr = LLVMAddAlias(context.llvmModule,
                    kTypeInfoPtr,
                    typeInfoGlobal.pointer.getElementPtr(0).llvm,
                    typeInfoSymbolName)!!

            if (declaration.isExported()) {
                if (llvmTypeInfoPtr.name != typeInfoSymbolName) {
                    // So alias name has been mangled by LLVM to avoid name clash.
                    throw IllegalArgumentException("Global '$typeInfoSymbolName' already exists")
                }
            } else {
                LLVMSetLinkage(llvmTypeInfoPtr, LLVMLinkage.LLVMInternalLinkage)
            }

            typeInfoPtr = constPointer(llvmTypeInfoPtr)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = declaration.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        if (declaration.isUnit() || declaration.isKotlinArray())
            createUniqueDeclarations(declaration, typeInfoPtr, bodyType)

        val singletonDeclarations = if (declaration.kind.isSingleton) {
            createSingletonDeclarations(declaration)
        } else {
            null
        }

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

        return ClassLlvmDeclarations(bodyType, typeInfoGlobal, writableTypeInfoGlobal, typeInfoPtr,
                singletonDeclarations, objCDeclarations)
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

    private fun createSingletonDeclarations(irClass: IrClass): SingletonLlvmDeclarations? {
        if (irClass.isUnit()) {
            return null
        }

        val storageKind = irClass.storageKind(context)
        val threadLocal = storageKind == ObjectStorageKind.THREAD_LOCAL
        val isExported = irClass.isExported()
        val symbolName = if (isExported) {
            irClass.globalObjectStorageSymbolName
        } else {
            "kobjref:" + qualifyInternalName(irClass)
        }
        val instanceAddress = if (threadLocal) {
            addKotlinThreadLocal(symbolName, getLLVMType(irClass.defaultType))
        } else {
            addKotlinGlobal(symbolName, getLLVMType(irClass.defaultType), isExported)
        }

        return SingletonLlvmDeclarations(instanceAddress)
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
                context.llvm.runtime.kotlinObjCClassInfo,
                classInfoSymbolName,
                isExported = isExported
        ).apply {
            setConstant(true)
        }

        val bodyOffsetGlobal = staticData.createGlobal(int32Type, "kobjcbodyoffs:$internalName")

        return KotlinObjCClassLlvmDeclarations(classInfoGlobal, bodyOffsetGlobal)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val containingClass = declaration.parent as? IrClass
        if (containingClass != null) {
            if (!containingClass.requiresRtti()) return
            val classDeclarations = (containingClass.metadata as? CodegenClassMetadata)?.llvm
                    ?: error(containingClass.descriptor.toString())
            val allFields = context.getLayoutBuilder(containingClass).fields
            declaration.metadata = CodegenInstanceFieldMetadata(
                    declaration.metadata?.name,
                    containingClass.konanLibrary,
                    FieldLlvmDeclarations(
                            allFields.indexOf(declaration) + 1, // First field is ObjHeader.
                            classDeclarations.bodyType
                    )
            )
        } else {
            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(declaration)
            val storage = if (declaration.storageKind == FieldStorageKind.THREAD_LOCAL) {
                addKotlinThreadLocal(name, getLLVMType(declaration.type))
            } else {
                addKotlinGlobal(name, getLLVMType(declaration.type), isExported = false)
            }

            declaration.metadata = CodegenStaticFieldMetadata(
                    declaration.metadata?.name,
                    declaration.konanLibrary,
                    StaticFieldLlvmDeclarations(storage)
            )
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (!declaration.isReal) return

        val llvmFunctionType = getLlvmFunctionType(declaration)

        if ((declaration is IrConstructor && declaration.isObjCConstructor)) {
            return
        }

        val llvmFunction = if (declaration.isExternal) {
            if (declaration.isTypedIntrinsic || declaration.isObjCBridgeBased()
                    // All call-sites to external accessors to interop properties
                    // are lowered by InteropLowering.
                    || (declaration.isAccessor && declaration.isFromInteropLibrary())
                    || declaration.annotations.hasAnnotation(RuntimeNames.cCall)) return

            context.llvm.externalFunction(declaration.computeSymbolName(), llvmFunctionType,
                    // Assume that `external fun` is defined in native libs attached to this module:
                    origin = declaration.llvmSymbolOrigin,
                    independent = declaration.hasAnnotation(RuntimeNames.independent)
            )
        } else {
            val symbolName = if (declaration.isExported()) {
                declaration.computeSymbolName().also {
                    if (declaration.name.asString() != "main") {
                        assert(LLVMGetNamedFunction(context.llvm.llvmModule, it) == null) { it }
                    } else {
                        // As a workaround, allow `main` functions to clash because frontend accepts this.
                        // See [OverloadResolver.isTopLevelMainInDifferentFiles] usage.
                    }
                }
            } else {
                "kfun:" + qualifyInternalName(declaration)
            }

            addLlvmFunctionWithDefaultAttributes(
                    context,
                    context.llvmModule!!,
                    symbolName,
                    llvmFunctionType
            ).also {
                addLlvmAttributesForKotlinFunction(context, declaration, it)
            }
        }

        declaration.metadata = CodegenFunctionMetadata(
                declaration.metadata?.name,
                declaration.konanLibrary,
                FunctionLlvmDeclarations(llvmFunction)
        )
    }
}

internal open class KonanMetadata(override val name: Name?, val konanLibrary: KotlinLibrary?) : MetadataSource

internal class CodegenClassMetadata(irClass: IrClass)
    : KonanMetadata(irClass.metadata?.name, irClass.konanLibrary), MetadataSource.Class {
    var layoutBuilder: ClassLayoutBuilder? = null
    var llvm: ClassLlvmDeclarations? = null
}

private class CodegenFunctionMetadata(
        name: Name?,
        konanLibrary: KotlinLibrary?,
        val llvm: FunctionLlvmDeclarations
) : KonanMetadata(name, konanLibrary), MetadataSource.Function

private class CodegenInstanceFieldMetadata(
        name: Name?,
        konanLibrary: KotlinLibrary?,
        val llvm: FieldLlvmDeclarations
) : KonanMetadata(name, konanLibrary), MetadataSource.Property {
    override val isConst = false
}

private class CodegenStaticFieldMetadata(
        name: Name?,
        konanLibrary: KotlinLibrary?,
        val llvm: StaticFieldLlvmDeclarations
) : KonanMetadata(name, konanLibrary), MetadataSource.Property {
    override val isConst = false
}