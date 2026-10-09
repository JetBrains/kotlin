/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.ir.isArray
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.classHierarchyIdsSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.interfaceTableRecordsSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.interfaceTableSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface.interfaceVTableSymbolName
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.WritableTypeInfoPointer
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.generateWritableTypeInfoForSyntheticInterface
import org.jetbrains.kotlin.backend.konan.llvm.runtime.RuntimeModule
import org.jetbrains.kotlin.backend.konan.lower.hasSyntheticNameToBeHiddenInReflection
import org.jetbrains.kotlin.backend.konan.lower.getObjectClassInstanceFunction
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.util.*

internal class RTTIGenerator(
        override val generationState: NativeGenerationState,
        private val referencedFunctions: Set<IrSimpleFunction>?,
) : ContextUtils {

    private val acyclicCache = mutableMapOf<IrType, Boolean>()
    private val safeAcyclicFieldTypes = setOf(
            context.irBuiltIns.stringClass,
            context.irBuiltIns.booleanClass, context.irBuiltIns.charClass,
            context.irBuiltIns.byteClass, context.irBuiltIns.shortClass, context.irBuiltIns.intClass,
            context.irBuiltIns.longClass,
            context.irBuiltIns.floatClass, context.irBuiltIns.doubleClass) +
            context.irBuiltIns.primitiveTypesToPrimitiveArrays.values +
            context.irBuiltIns.unsignedTypesToUnsignedArrays.values

    // TODO: extend logic here by taking into account final acyclic classes.
    private fun checkAcyclicFieldType(type: IrType): Boolean = acyclicCache.getOrPut(type) {
        when {
            type.isInterface() -> false
            type.computePrimitiveBinaryTypeOrNull() != null -> true
            else -> {
                val classifier = type.classifierOrNull
                (classifier != null && classifier in safeAcyclicFieldTypes)
            }
        }
    }

    private fun checkAcyclicClass(irClass: IrClass): Boolean = when {
        irClass.symbol == context.irBuiltIns.arrayClass -> false
        irClass.isArray -> true
        context.getLayoutBuilder(irClass).getFields(llvm).all { checkAcyclicFieldType(it.type) } -> true
        else -> false
    }

    private fun flagsFromClass(irClass: IrClass): Int {
        var result = 0
        // TODO: maybe perform deeper analysis to find surely acyclic types.
        if (!irClass.isInterface && !irClass.isAbstract() && !irClass.isAnnotationClass) {
            if (checkAcyclicClass(irClass)) {
                result = result or TF_ACYCLIC
            }
        }
        if (irClass.isInterface)
            result = result or TF_INTERFACE

        if (irClass.defaultType.isSuspendFunction()) {
            result = result or TF_SUSPEND_FUNCTION
        }

        if (irClass.hasFinalizer) {
            result = result or TF_HAS_FINALIZER
        }

        return result
    }

    inner class InterfaceTableRecord(id: ConstInt32, vtableSize: ConstInt32, vtable: ConstPointer?) :
            Struct(runtime.interfaceTableRecordType, id, vtableSize, vtable)

    private inner class TypeInfo(
            selfPtr: ConstPointer,
            extendedInfo: ConstPointer,
            size: Int,
            superType: ConstValue,
            objOffsets: ConstValue,
            objOffsetsCount: Int,
            interfaces: ConstValue,
            interfacesCount: Int,
            interfaceTable: ConstValue,
            packageName: String?,
            relativeName: String?,
            flags: Int,
            classId: Int,
            classHierarchyIds: ConstPointer,
            writableTypeInfo: WritableTypeInfoPointer?,
            associatedObjects: ConstPointer?,
            processObjectInMark: ConstPointer?,
            requiredAlignment: Int,
    ) : Struct(
                    runtime.typeInfoType,

                    selfPtr,

                    extendedInfo,

                    // TODO: it used to be a single int32 ABI version,
                    // but klib abi version is not an int anymore.
                    // So now this field is just reserved to preserve the layout.
                    llvm.constInt32(0),

                    llvm.constInt32(size),

                    superType,

                    objOffsets,
                    llvm.constInt32(objOffsetsCount),

                    interfaces,
                    llvm.constInt32(interfacesCount),

                    interfaceTable,

                    kotlinStringLiteral(packageName),
                    kotlinStringLiteral(relativeName),

                    llvm.constInt32(flags),

                    llvm.constInt32(classId),

                    classHierarchyIds,

                    *listOfNotNull(writableTypeInfo).toTypedArray(),

                    associatedObjects,

                    processObjectInMark,
                    llvm.constInt32(requiredAlignment),
    )

    private fun kotlinStringLiteral(string: String?): ConstPointer = if (string == null) {
        llvm.nullPointer
    } else {
        staticData.kotlinStringLiteral(string)
    }

    private fun exportTypeInfoIfRequired(irClass: IrClass, typeInfoGlobal: LLVMValueRef?) {
        val annotation = irClass.annotations.findAnnotation(RuntimeNames.exportTypeInfoAnnotation)
        if (annotation != null) {
            val name = annotation.getConstArgument<String>("name")!!
            // TODO: use LLVMAddAlias.
            val global = addGlobal(name, llvm.pointerType, isExported = true)
            LLVMSetInitializer(global, typeInfoGlobal)
        }
    }

    private val arrayClasses = mapOf(
            IdSignatureValues.array to llvm.pointerType,
            primitiveArrayTypesSignatures[PrimitiveType.BYTE] to llvm.int8Type,
            primitiveArrayTypesSignatures[PrimitiveType.CHAR] to llvm.int16Type,
            primitiveArrayTypesSignatures[PrimitiveType.SHORT] to llvm.int16Type,
            primitiveArrayTypesSignatures[PrimitiveType.INT] to llvm.int32Type,
            primitiveArrayTypesSignatures[PrimitiveType.LONG] to llvm.int64Type,
            primitiveArrayTypesSignatures[PrimitiveType.FLOAT] to llvm.floatType,
            primitiveArrayTypesSignatures[PrimitiveType.DOUBLE] to llvm.doubleType,
            primitiveArrayTypesSignatures[PrimitiveType.BOOLEAN] to llvm.int8Type,
            IdSignatureValues.string to llvm.int16Type,
            getPublicSignature(KonanFqNames.packageName, "ImmutableBlob") to llvm.int8Type,
            getPublicSignature(KonanFqNames.internalPackageName, "NativePtrArray") to llvm.pointerType
    )

    // Keep in sync with Konan_RuntimeType.
    private val RT_OBJECT = 1
    private val primitiveRuntimeTypeMap = mapOf(
            llvm.int8Type to 2,
            llvm.int16Type to 3,
            llvm.int32Type to 4,
            llvm.int64Type to 5,
            llvm.floatType to 6,
            llvm.doubleType to 7,
            llvm.pointerType to 8,
            llvm.int1Type to 9,
            llvm.vector128Type to 10
    )

    private fun getElementType(irClass: IrClass): LLVMTypeRef? {
        val signature = irClass.symbol.signature as? IdSignature.CommonSignature?
        return signature?.let { arrayClasses[it] }
    }

    private fun getInstanceSize(classType: LLVMTypeRef?, irClass: IrClass) : Int {
        val elementType = getElementType(irClass)
        // Check if it is an array.
        if (elementType != null) return -LLVMABISizeOfType(llvmTargetData, elementType).toInt()
        return LLVMStoreSizeOfType(llvmTargetData, classType).toInt()
    }

    fun generate(irClass: IrClass) {

        val className = irClass.fqNameForIrSerialization

        val llvmDeclarations = generationState.llvmDeclarations.forClass(irClass)

        val bodyType = llvmDeclarations.bodyType.llvmBodyType

        val instanceSize = getInstanceSize(bodyType, irClass)

        val superType = when {
            irClass.isAny() -> llvm.nullPointer
            irClass.isKotlinObjCClass() -> context.irBuiltIns.anyClass.owner.typeInfoPtr
            else -> {
                val superTypeOrAny = irClass.getSuperClassNotAny() ?: context.irBuiltIns.anyClass.owner
                superTypeOrAny.typeInfoPtr
            }
        }

        val implementedInterfaces = irClass.implementedInterfaces.filter { it.requiresRtti() }

        val interfaces = implementedInterfaces.map { it.typeInfoPtr }
        val interfacesPtr = staticData.placeGlobalConstArray("kintf:$className",
                llvm.pointerType, interfaces)

        val objOffsets = getObjOffsets(llvmDeclarations.bodyType)

        val objOffsetsPtr = staticData.placeGlobalConstArray("krefs:$className", llvm.int32Type, objOffsets)

        val objOffsetsCount = if (irClass.symbol == context.irBuiltIns.arrayClass) {
            1 // To mark it as non-leaf.
        } else {
            objOffsets.size
        }

        val interfaceTablePtr = interfaceLookupTable(irClass)

        val reflectionInfo = getReflectionInfo(irClass)
        val typeInfoGlobal = llvmDeclarations.typeInfoGlobal
        // Only meaningful when another compilation lays the intervals out; otherwise classId_ carries this
        // class's own interval, and without the optimizations there are no intervals at all.
        val classHierarchyIds =
                if (irClass.isInterface || !context.hierarchyWillBeLaidOutByFinalLink) llvm.nullPointer
                else constPointer(importGlobal(irClass.classHierarchyIdsSymbolName, runtime.classHierarchyIdsType))
        val typeInfo = TypeInfo(
                irClass.typeInfoPtr,
                makeExtendedInfo(irClass),
                instanceSize,
                superType,
                objOffsetsPtr, objOffsetsCount,
                interfacesPtr, interfaces.size,
                interfaceTablePtr,
                reflectionInfo.packageName,
                reflectionInfo.relativeName,
                flagsFromClass(irClass) or reflectionInfo.reflectionFlags,
                context.getLayoutBuilder(irClass).classId,
                classHierarchyIds,
                llvmDeclarations.writableTypeInfoGlobal,
                associatedObjects = genAssociatedObjects(irClass),
                processObjectInMark = when {
                    irClass.symbol == context.irBuiltIns.arrayClass -> llvm.Kotlin_processArrayInMark.toConstPointer()
                    else -> genProcessObjectInMark(llvmDeclarations.bodyType)
                },
                requiredAlignment = llvmDeclarations.alignment
        )

        val typeInfoGlobalValue = if (!irClass.typeInfoHasVtableAttached) {
            typeInfo
        } else {
            val vtable = vtable(irClass)
            llvm.struct(typeInfo, vtable)
        }

        typeInfoGlobal.setInitializer(typeInfoGlobalValue)
        typeInfoGlobal.setConstant(true)

        exportTypeInfoIfRequired(irClass, irClass.llvmTypeInfoPtr)
    }

    private fun getObjOffsets(bodyType: ObjectBodyType): List<ConstInt32> =
            bodyType.sortedIndicesOfObjectFields.map { index ->
                llvm.constInt32(LLVMOffsetOfElement(llvmTargetData, bodyType.llvmBodyType, index).toInt())
            }

    fun vtable(irClass: IrClass): ConstArray {
        // TODO: compile-time resolution limits binary compatibility.
        val vtableEntries = context.getLayoutBuilder(irClass).vtableEntries.map {
            val implementation = it.implementation
            if (implementation == null || implementation.isExternalObjCClassMethod() || referencedFunctions?.contains(implementation) == false) {
                llvm.nullPointer
            } else {
                implementation.entryPointAddress
            }
        }
        return ConstArray(llvm.pointerType, vtableEntries)
    }

    private fun interfaceLookupTable(irClass: IrClass): ConstValue {
        if (!irClass.needsInterfaceLookupTable) return llvm.nullPointer

        irClass.implementedInterfaces.forEach { iface ->
            emitInterfaceVTable(irClass, iface)
        }

        return if (context.hierarchyWillBeLaidOutByFinalLink) {
            constPointer(importGlobal(irClass.interfaceTableSymbolName, runtime.interfaceTableType))
        } else {
            val entries = irClass.implementedInterfaces.map { iface ->
                InterfaceTableEntry(
                        interfaceName = iface.crossModuleName(),
                        interfaceId = context.getLayoutBuilder(iface).classId,
                        vtableSize = context.getLayoutBuilder(iface).interfaceVTableEntries.size,
                )
            }
            createInterfaceLookupTable(irClass.crossModuleName(), entries)
        }
    }

    /**
     * The interface lookup table records of [irClass] for its ObjC type adapter: an abstract class owns no table
     * of its own, but the ObjC runtime builds the tables of its subclasses out of these records.
     */
    fun interfaceTableRecordsForObjCTypeAdapter(irClass: IrClass): Pair<List<InterfaceTableRecord>, Int> {
        val layouts = irClass.implementedInterfaces.map { context.getLayoutBuilder(it) }
        val [slots, size] = layOutInterfaceLookupTable(layouts) { it.classId }
        val records = slots.map { iface ->
            if (iface == null)
                InterfaceTableRecord(llvm.constInt32(0), llvm.constInt32(0), null)
            else
                InterfaceTableRecord(
                        llvm.constInt32(iface.classId),
                        llvm.constInt32(iface.interfaceVTableEntries.size),
                        emitInterfaceVTable(irClass, iface.irClass),
                )
        }
        return Pair(records, size)
    }

    /**
     * The interval of the classes that are not part of the recorded hierarchy. Any's own interval starts here,
     * so `is Any` accepts them and no other class type check does.
     */
    private val ANY_INTERVAL_START = -1

    private fun syntheticClassHierarchyIds(): ConstPointer =
            if (!context.hierarchyWillBeLaidOutByFinalLink) llvm.nullPointer
            else staticData.placeGlobal("", Struct(runtime.classHierarchyIdsType,
                    llvm.constInt32(ANY_INTERVAL_START), llvm.constInt32(ANY_INTERVAL_START)))
                    .also { it.setConstant(true) }.pointer

    /** The vtable of the methods of [iface] as implemented by [irClass]. */
    private fun emitInterfaceVTable(irClass: IrClass, iface: IrClass): ConstPointer {
        val layoutBuilder = context.getLayoutBuilder(irClass)
        val vtableEntries = context.getLayoutBuilder(iface).interfaceVTableEntries.map { ifaceFunction ->
            val impl = layoutBuilder.overridingOf(ifaceFunction)
            if (impl == null || referencedFunctions?.contains(impl) == false)
                llvm.nullPointer
            else impl.entryPointAddress
        }
        return staticData.placeGlobalConstArray(
                interfaceVTableSymbolName(irClass.crossModuleName(), iface.crossModuleName()),
                llvm.pointerType, vtableEntries,
                // The lookup table this vtable goes into is laid out by the compilation producing the final binary.
                isExported = context.hierarchyWillBeLaidOutByFinalLink,
        )
    }

    private fun mapRuntimeType(type: LLVMTypeRef, isObjectType: Boolean): Int {
        if (isObjectType) {
            require(type == llvm.pointerType) { "Expected object type, got ${type.toTypeString()}" }
            return RT_OBJECT
        }

        return primitiveRuntimeTypeMap[type] ?: throw Error("Unmapped type: ${type.toTypeString()}")
    }

    private val debugRuntimeOrNull: LLVMModuleRef? by lazy {
        if (generationState.runtimeModulesConfig.containsDebuggingRuntime) {
            val path = generationState.runtimeModulesConfig.absolutePathFor(RuntimeModule.DEBUG)
            parseBitcodeFile(context, context.diagnosticReporter, llvm.llvmContext, path)
        } else {
            null
        }
    }

    private val debugOperations: ConstValue by lazy {
        if (debugRuntimeOrNull == null) {
            llvm.nullPointer
        } else {
            val external = LLVMGetNamedGlobal(debugRuntimeOrNull, "Konan_debugOperationsList")!!
            val local = LLVMAddGlobal(llvm.module, LLVMGlobalGetValueType(external),"Konan_debugOperationsList")!!
            constPointer(local)
        }
    }

    val debugOperationsSize: ConstValue by lazy {
        if (debugRuntimeOrNull != null) {
            val external = LLVMGetNamedGlobal(debugRuntimeOrNull, "Konan_debugOperationsList")!!
            llvm.constInt32(LLVMGetArrayLength(LLVMGlobalGetValueType(external)))
        } else
            llvm.constInt32(0)
    }

    private fun makeExtendedInfo(irClass: IrClass): ConstPointer {
        // TODO: shall we actually do that?
        if (context.shouldOptimize())
            return llvm.nullPointer

        val className = irClass.fqNameForIrSerialization.toString()
        val llvmDeclarations = generationState.llvmDeclarations.forClass(irClass)
        val bodyType = llvmDeclarations.bodyType.llvmBodyType
        val elementType = getElementType(irClass)

        val value = if (elementType != null) {
            // An array type.
            val isElementTypeObject = irClass.isKotlinArray()
            val runtimeElementType = mapRuntimeType(elementType, isElementTypeObject)
            Struct(runtime.extendedTypeInfoType,
                    llvm.constInt32(-runtimeElementType),
                    llvm.nullPointer, llvm.nullPointer, llvm.nullPointer,
                    debugOperationsSize, debugOperations)
        } else {
            class FieldRecord(val offset: Int, val type: Int, val name: String)

            val objectFieldIndices = llvmDeclarations.bodyType.sortedIndicesOfObjectFields.toSet()

            val fields = context.getLayoutBuilder(irClass).getFields(llvm).map {
                val index = llvmDeclarations.fieldIndices[it.irFieldSymbol]!!
                val isObjectType = index in objectFieldIndices
                FieldRecord(
                        LLVMOffsetOfElement(llvmTargetData, bodyType, index).toInt(),
                        mapRuntimeType(LLVMStructGetTypeAtIndex(bodyType, index)!!, isObjectType),
                        it.name)
            }
            val offsetsPtr = staticData.placeGlobalConstArray("kextoff:$className", llvm.int32Type,
                    fields.map { llvm.constInt32(it.offset) })
            val typesPtr = staticData.placeGlobalConstArray("kexttype:$className", llvm.int8Type,
                    fields.map { llvm.constInt8(it.type.toByte()) })
            val namesPtr = staticData.placeGlobalConstArray("kextname:$className", llvm.pointerType,
                    fields.map { staticData.placeCStringLiteral(it.name) })

            Struct(runtime.extendedTypeInfoType, llvm.constInt32(fields.size), offsetsPtr, typesPtr, namesPtr,
                    debugOperationsSize, debugOperations)
        }

        val result = staticData.placeGlobal("", value)
        result.setConstant(true)
        return result.pointer
    }

    private fun genAssociatedObjects(irClass: IrClass): ConstPointer? {
        val associatedObjects = context.getLayoutBuilder(irClass).associatedObjects
        if (associatedObjects.isEmpty()) {
            return null
        }

        val associatedObjectTableRecords = associatedObjects.map { [key, value] ->
            val function = context.getObjectClassInstanceFunction(value)

            Struct(runtime.associatedObjectTableRecordType, key.typeInfoPtr, function.llvmFunction.toConstPointer())
        }

        return staticData.placeGlobalConstArray(
                name = "kassociatedobjects:${irClass.fqNameForIrSerialization}",
                elemType = runtime.associatedObjectTableRecordType,
                elements = associatedObjectTableRecords + Struct(runtime.associatedObjectTableRecordType, null, null)
        )
    }

    private fun genProcessObjectInMark(classType: ObjectBodyType): ConstPointer {
        val indicesOfObjectFields = classType.sortedIndicesOfObjectFields
        return when {
            indicesOfObjectFields.isEmpty() -> {
                // TODO: Try to generate it here instead of importing from the runtime.
                llvm.Kotlin_processEmptyObjectInMark.toConstPointer()
            }
            else -> {
                // TODO: specialize for "small" objects
                llvm.Kotlin_processObjectInMark.toConstPointer()
            }
        }
    }

    // TODO: extract more code common with generate().
    fun generateSyntheticInterfaceImpl(
            irClass: IrClass,
            methodImpls: Map<IrSimpleFunction, ConstPointer>,
            bodyType: ObjectBodyType,
            immutable: Boolean = false
    ): ConstPointer {
        assert(irClass.isInterface)

        val size = LLVMStoreSizeOfType(llvmTargetData, bodyType.llvmBodyType).toInt()

        val superClass = context.irBuiltIns.anyClass.owner

        assert(superClass.implementedInterfaces.isEmpty())
        val interfaces = (listOf(irClass) + irClass.implementedInterfaces)
        val interfacesPtr = staticData.placeGlobalConstArray("",
                llvm.pointerType, interfaces.map { it.typeInfoPtr })

        assert(superClass.declarations.all { it !is IrProperty && it !is IrField })

        val objOffsets = getObjOffsets(bodyType)
        val objOffsetsPtr = staticData.placeGlobalConstArray("", llvm.int32Type, objOffsets)
        val objOffsetsCount = objOffsets.size

        val writableTypeInfo = generateWritableTypeInfoForSyntheticInterface(irClass)
        val vtable = vtable(superClass)
        val typeInfoWithVtableType = llvm.structType(runtime.typeInfoType, vtable.llvmType)
        val typeInfoWithVtableGlobal = staticData.createGlobal(typeInfoWithVtableType, "", isExported = false)
        val result = typeInfoWithVtableGlobal.pointer.getElementPtr(llvm, typeInfoWithVtableType, 0)
        // TODO: interfaces (e.g. FunctionN and Function) should have different colors.
        val [interfaceTableSkeleton, interfaceTableSize] =
                layOutInterfaceLookupTable(interfaces.map { context.getLayoutBuilder(it) }) { it.classId }

        val interfaceTable = interfaceTableSkeleton.map { layoutBuilder ->
            if (layoutBuilder == null) {
                InterfaceTableRecord(llvm.constInt32(0), llvm.constInt32(0), null)
            } else {
                val vtableEntries = layoutBuilder.interfaceVTableEntries.map { methodImpls[it]!! }
                val interfaceVTable = staticData.placeGlobalArray("", llvm.pointerType, vtableEntries)
                val interfaceVTableType = LLVMArrayType(llvm.pointerType, vtableEntries.size)!!
                InterfaceTableRecord(
                        llvm.constInt32(layoutBuilder.classId),
                        llvm.constInt32(layoutBuilder.interfaceVTableEntries.size),
                        interfaceVTable.pointer.getElementPtr(llvm, interfaceVTableType, 0)
                )
            }
        }
        val interfaceTableRecordsPtr = staticData.placeGlobalConstArray("", runtime.interfaceTableRecordType, interfaceTable)
        val interfaceTablePtr = staticData.placeGlobal("",
                Struct(runtime.interfaceTableType, llvm.constInt32(interfaceTableSize), interfaceTableRecordsPtr)).pointer

        val typeInfoWithVtable = llvm.struct(TypeInfo(
                selfPtr = result,
                extendedInfo = llvm.nullPointer,
                size = size,
                superType = superClass.typeInfoPtr,
                objOffsets = objOffsetsPtr, objOffsetsCount = objOffsetsCount,
                interfaces = interfacesPtr, interfacesCount = interfaces.size,
                interfaceTable = interfaceTablePtr,
                packageName = ReflectionInfo.EMPTY.packageName,
                relativeName = ReflectionInfo.EMPTY.relativeName,
                flags = flagsFromClass(irClass) or (if (immutable) TF_IMMUTABLE else 0),
                // These classes are not part of the recorded hierarchy; they derive from Any and nothing else,
                // so they take Any's own interval start and no class type check but `is Any` accepts them.
                classId = ANY_INTERVAL_START,
                classHierarchyIds = syntheticClassHierarchyIds(),
                writableTypeInfo = writableTypeInfo,
                associatedObjects = null,
                processObjectInMark = genProcessObjectInMark(bodyType),
                requiredAlignment = runtime.objectAlignment
        ), vtable)

        typeInfoWithVtableGlobal.setInitializer(typeInfoWithVtable)
        typeInfoWithVtableGlobal.setConstant(true)

        return result
    }

    private val OverriddenFunctionInfo.implementation get() = getImplementation(context)

    data class ReflectionInfo(val packageName: String?, val relativeName: String?, val reflectionFlags: Int) {
        companion object {
            val EMPTY = ReflectionInfo(null, null, 0)
        }
    }

    private fun getReflectionInfo(irClass: IrClass): ReflectionInfo {
        val packageFragment = irClass.getPackageFragment()
        // `@kotlin.internal.ReflectionPackageName` is used by test infrastructure.
        val reflectionPackageName = (packageFragment as? IrFile)?.reflectionPackageName
        val packageName: String = reflectionPackageName ?: packageFragment.packageFqName.asString() // Compute and store package name in TypeInfo anyways.
        val relativeName: String?
        val flags: Int

        when {
            irClass.hasSyntheticNameToBeHiddenInReflection -> {
                relativeName = irClass.name.asString()
                flags = 0 // Forbid to use package and relative names in KClass.[simpleName|qualifiedName].
            }
            irClass.isOriginallyLocal -> {
                relativeName = irClass.name.asString()
                flags = TF_REFLECTION_SHOW_REL_NAME // Only allow relative name to be used in KClass.simpleName.
            }
            else -> {
                relativeName = generateDefaultRelativeName(irClass)
                flags = TF_REFLECTION_SHOW_PKG_NAME or TF_REFLECTION_SHOW_REL_NAME // Allow both package and relative names to be used in
                // KClass.[simpleName|qualifiedName].
            }
        }

        return ReflectionInfo(packageName, relativeName, flags)
    }

    private fun generateDefaultRelativeName(irClass: IrClass) =
            generateSequence(irClass) { it.parent as? IrClass }
                    .toList().reversed()
                    .joinToString(".") { it.name.asString() }

    fun dispose() {
        debugRuntimeOrNull?.let { LLVMDisposeModule(it) }
    }
}

internal fun ContextUtils.createInterfaceLookupTable(className: String, entries: List<InterfaceTableEntry>): ConstPointer {
    val [slots, size] = layOutInterfaceLookupTable(entries) { it.interfaceId }
    val records = slots.map { entry ->
        // kInvalidInterfaceId is zero, and the ids are numerated from one, so an empty slot holds zeroes.
        Struct(runtime.interfaceTableRecordType,
                llvm.constInt32(entry?.interfaceId ?: 0),
                llvm.constInt32(entry?.vtableSize ?: 0),
                if (entry == null) llvm.nullPointer else interfaceVTable(className, entry),
        )
    }
    val recordsPtr = staticData.placeGlobalConstArray(interfaceTableRecordsSymbolName(className),
            runtime.interfaceTableRecordType, records)
    val initializer = Struct(runtime.interfaceTableType, llvm.constInt32(size), recordsPtr)
    val symbolName = interfaceTableSymbolName(className)

    if (!context.hierarchyWillBeLaidOutByFinalLink) {
        // Nothing outside this module refers to the table, so let it be internalized and stripped as usual.
        return staticData.placeGlobal(symbolName, initializer, isExported = false)
                .also { it.setConstant(true) }.pointer
    }

    val table = staticData.getOrCreateGlobal(runtime.interfaceTableType, symbolName, isExported = true)
    table.setInitializer(initializer)
    table.setConstant(true)
    llvm.usedGlobals += table.llvmGlobal
    return table.pointer
}

private fun ContextUtils.interfaceVTable(className: String, entry: InterfaceTableEntry): ConstPointer {
    // An interface with no methods of its own has no vtable to point at, and no call can ever reach for one.
    if (entry.vtableSize == 0) return llvm.nullPointer
    val symbolName = interfaceVTableSymbolName(className, entry.interfaceName)
    return staticData.getGlobal(symbolName)?.pointer
            ?: constPointer(importGlobal(
                    symbolName,
                    LLVMArrayType(llvm.pointerType, entry.vtableSize)!!
            ))
}

/**
 * Places [entries] into the slots of an interface lookup table: a perfect hash table keyed by the color bits of
 * the interface ids when they fit into one, and a table sorted by the interface id otherwise.
 * Returns the slots along with the size to record in the table: the mask for the former shape, the negated count for the latter.
 * The details of the scheme are on [GlobalHierarchyAnalysis].
 */
internal fun <T> layOutInterfaceLookupTable(entries: List<T>, interfaceId: (T) -> Int): Pair<List<T?>, Int> {
    // Find the optimal size. It must be a power of 2.
    var size = 1
    val maxSize = 1 shl MAX_BITS_PER_COLOR
    val used = BooleanArray(maxSize)
    while (size <= maxSize) {
        for (i in 0 until size)
            used[i] = false
        // Check for collisions.
        var ok = true
        for (entry in entries) {
            val index = interfaceId(entry) and (size - 1) // This is not an optimization but rather for not to bother with negative numbers.
            if (used[index]) {
                ok = false
                break
            }
            used[index] = true
        }
        if (ok) break
        size *= 2
    }

    if (size <= maxSize) {
        val slots = MutableList<T?>(size) { null }
        for (entry in entries)
            slots[interfaceId(entry) and (size - 1)] = entry
        return Pair(slots, size - 1)
    }

    val sorted = entries.sortedBy(interfaceId)
    for (i in 1 until sorted.size)
        require(interfaceId(sorted[i - 1]) != interfaceId(sorted[i])) {
            "Different interfaces have same interface id: ${interfaceId(sorted[i])}"
        }
    return Pair(sorted, -sorted.size)
}

// Keep in sync with Konan_TypeFlags in TypeInfo.h.
private const val TF_IMMUTABLE = 1
private const val TF_ACYCLIC   = 2
private const val TF_INTERFACE = 4
private const val TF_OBJC_DYNAMIC = 8
private const val TF_LEAK_DETECTOR_CANDIDATE = 16
private const val TF_SUSPEND_FUNCTION = 32
private const val TF_HAS_FINALIZER = 64
private const val TF_HAS_FREEZE_HOOK = 128
private const val TF_REFLECTION_SHOW_PKG_NAME = 256
private const val TF_REFLECTION_SHOW_REL_NAME = 512
