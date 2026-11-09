/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import llvm.LLVMABIAlignmentOfType
import org.jetbrains.kotlin.backend.konan.llvm.alignTo
import llvm.LLVMABISizeOfType
import llvm.LLVMStoreSizeOfType
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.llvm.CodegenLlvmHelpers
import org.jetbrains.kotlin.backend.konan.llvm.globalHierarchyClassIdInterval
import org.jetbrains.kotlin.backend.konan.llvm.backupInterfaceId
import org.jetbrains.kotlin.backend.konan.llvm.hierarchyWillBeLaidOutByFinalLink
import org.jetbrains.kotlin.backend.konan.llvm.globalHierarchyInterfaceId
import org.jetbrains.kotlin.backend.konan.llvm.computeFunctionName
import org.jetbrains.kotlin.backend.konan.llvm.needsCacheEntryPointForFinalFakeOverride
import org.jetbrains.kotlin.backend.konan.llvm.requireOwnsInterfaceNumbering
import org.jetbrains.kotlin.backend.konan.llvm.toLLVMType
import org.jetbrains.kotlin.backend.konan.lower.bridgeTarget
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsDeserializer
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.objcinterop.canObjCClassMethodBeCalledVirtually
import org.jetbrains.kotlin.ir.objcinterop.isKotlinObjCClass
import org.jetbrains.kotlin.ir.objcinterop.isObjCClassMethod
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import kotlin.math.min

internal class OverriddenFunctionInfo(
        val function: IrSimpleFunction,
        val overriddenFunction: IrSimpleFunction,
        val policy: BridgesPolicy,
) {
    val needBridge: Boolean
        get() = function.target.needBridgeTo(overriddenFunction, policy)

    val bridgeDirections: BridgeDirections
        get() = function.target.bridgeDirectionsTo(overriddenFunction, policy)

    val canBeCalledVirtually: Boolean
        get() {
            if (overriddenFunction.isObjCClassMethod()) {
                return function.canObjCClassMethodBeCalledVirtually(overriddenFunction)
            }

            return overriddenFunction.isOverridable
        }

    val inheritsBridge: Boolean
        get() = !function.isReal
                && function.target.overrides(overriddenFunction)
                && function.bridgeDirectionsTo(overriddenFunction, policy).allNotNeeded()

    fun getImplementation(context: NativeBackendContext): IrSimpleFunction? {
        val target = function.target
        val implementation = if (!needBridge)
            target
        else {
            val bridgeOwner = if (inheritsBridge) {
                target // Bridge is inherited from superclass.
            } else {
                function
            }
            context.bridgesSupport.getBridge(OverriddenFunctionInfo(bridgeOwner, overriddenFunction, policy))
        }
        return if (implementation.modality == Modality.ABSTRACT) null else implementation
    }

    context(config: NativeSecondStageCompilationConfig)
    fun needsBridgeForCacheEntryPoint(): Boolean =
            overriddenFunction == function && function.needsCacheEntryPointForFinalFakeOverride

    override fun toString(): String {
        return "(descriptor=$function, overriddenDescriptor=$overriddenFunction)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverriddenFunctionInfo) return false

        if (function != other.function) return false
        if (overriddenFunction != other.overriddenFunction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = function.hashCode()
        result = 31 * result + overriddenFunction.hashCode()
        return result
    }
}

internal fun IrField.requiredAlignment(llvm: CodegenLlvmHelpers): Int {
    val llvmType = type.toLLVMType(llvm)
    val abiAlignment = if (llvmType == llvm.vector128Type) {
        8 // over-aligned objects are not supported now, and this worked somehow, so let's keep it as it for now
    } else {
        LLVMABIAlignmentOfType(llvm.runtime.targetData, llvmType)
    }
    return if (hasAnnotation(KonanFqNames.volatile)) {
        val size = LLVMABISizeOfType(llvm.runtime.targetData, llvmType).toInt()
        val alignment = maxOf(size, abiAlignment)
        require(alignment % size == 0) { "Bad alignment of field ${render()}: abiAlignment = ${abiAlignment}, size = ${size}"}
        require(alignment % abiAlignment == 0) { "Bad alignment of field ${render()}: abiAlignment = ${abiAlignment}, size = ${size}"}
        alignment
    } else {
        abiAlignment
    }
}


internal class ClassLayoutBuilder(val irClass: IrClass, val context: NativeBackendContext) {
    private val bridgesPolicy = context.config.bridgesPolicy

    private fun IrField.toFieldInfo(llvm: CodegenLlvmHelpers): FieldInfo {
        val isConst = correspondingPropertySymbol?.owner?.isConst ?: false
        require(!isConst || initializer?.expression is IrConst) { "A const val field ${render()} must have constant initializer" }
        return FieldInfo(name.asString(), type, isConst, symbol, requiredAlignment(llvm))
    }

    val vtableEntries: List<OverriddenFunctionInfo> by lazy {
        require(!irClass.isInterface) {
            buildString {
                appendLine("Expected a class, found interface:")
                appendLine("  IR: " + irClass.render())
                appendLine("  FQ name: " + irClass.fqNameForIrSerialization)
                irClass.fileOrNull?.let { file ->
                    appendLine("  Location: " + irClass.getCompilerMessageLocation(file))
                }
            }
        }

        context.logMultiple {
            +""
            +"BUILDING vTable for ${irClass.render()}"
        }

        val superVtableEntries = if (irClass.isSpecialClassWithNoSupertypes()) {
            emptyList()
        } else {
            val superClass = irClass.getSuperClassNotAny() ?: context.irBuiltIns.anyClass.owner
            context.getLayoutBuilder(superClass).vtableEntries
        }

        val methods = overridableOrOverridingMethods
        val newVtableSlots = mutableListOf<OverriddenFunctionInfo>()
        val overridenVtableSlots = mutableMapOf<IrSimpleFunction, OverriddenFunctionInfo>()

        context.logMultiple {
            +""
            +"SUPER vTable:"
            superVtableEntries.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()}" }

            +""
            +"METHODS:"
            methods.forEach { +"    ${it.render()}" }

            +""
            +"BUILDING INHERITED vTable"
        }

        val superVtableMap = superVtableEntries.groupBy { it.function }
        methods.forEach { overridingMethod ->
            overridingMethod.allOverriddenFunctions.forEach {
                val superMethods = superVtableMap[it]
                if (superMethods?.isNotEmpty() == true) {
                    newVtableSlots.add(OverriddenFunctionInfo(overridingMethod, it, bridgesPolicy))
                    superMethods.forEach { superMethod ->
                        overridenVtableSlots[superMethod.overriddenFunction] =
                                OverriddenFunctionInfo(overridingMethod, superMethod.overriddenFunction, bridgesPolicy)
                    }
                }
            }
        }
        val inheritedVtableSlots = superVtableEntries.map { superMethod ->
            overridenVtableSlots[superMethod.overriddenFunction]?.also {
                context.log { "Taking overridden ${superMethod.overriddenFunction.render()} -> ${it.function.render()}" }
            } ?: superMethod.also {
                context.log { "Taking super ${superMethod.overriddenFunction.render()} -> ${superMethod.function.render()}" }
            }
        }

        // Add all possible (descriptor, overriddenDescriptor) edges for now, redundant will be removed later.
        methods.mapTo(newVtableSlots) { OverriddenFunctionInfo(it, it, bridgesPolicy) }

        val inheritedVtableSlotsSet = inheritedVtableSlots.map { it.function to it.bridgeDirections }.toSet()

        val filteredNewVtableSlots = newVtableSlots
            .filterNot { inheritedVtableSlotsSet.contains(it.function to it.bridgeDirections) }
            .distinctBy { it.function to it.bridgeDirections }
            .filter { it.function.isOverridable }

        context.logMultiple {
            +""
            +"INHERITED vTable slots:"
            inheritedVtableSlots.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()}" }

            +""
            +"MY OWN vTable slots:"
            filteredNewVtableSlots.forEach { +"    ${it.overriddenFunction.render()} -> ${it.function.render()} ${it.function}" }
            +"DONE vTable for ${irClass.render()}"
        }

        inheritedVtableSlots + filteredNewVtableSlots.sortedBy { it.overriddenFunction.uniqueName }
    }

    fun vtableIndex(function: IrSimpleFunction): Int {
        val bridgeDirections = function.target.bridgeDirectionsTo(function, bridgesPolicy)
        val index = vtableEntries.indexOfFirst { it.function == function && it.bridgeDirections == bridgeDirections }
        require(index >= 0) { "${function.render()} is not found in vtable of ${irClass.render()}" }
        return index
    }

    fun overridingOf(function: IrSimpleFunction) =
            overridableOrOverridingMethods.firstOrNull { function in it.allOverriddenFunctions }?.let {
                OverriddenFunctionInfo(it, function, bridgesPolicy).getImplementation(context)
            }

    val interfaceVTableEntries: List<IrSimpleFunction> by lazy {
        require(irClass.isInterface)
        irClass.simpleFunctions()
                .map { it.getLoweredVersion() }
                .filter { f ->
                    f.isOverridable && f.bridgeTarget == null
                            && (f.isReal || f.overriddenSymbols.any { f.needBridgeTo(it.owner, bridgesPolicy) })
                }
                .sortedBy { it.uniqueName }
    }

    data class InterfaceTablePlace(val irInterface: IrClass?, val interfaceId: Int, val itableSize: Int, val methodIndex: Int) {
        companion object {
            val INVALID = InterfaceTablePlace(null, 0, -1, -1)
        }
    }

    /**
     * The value to put into `TypeInfo.classId_`: the start of the DFS interval of a class, the id of an
     * interface in the interface numbering, and zero where neither is known here.
     */
    val classId: Int
        get() = when {
            // The type info of such an obj class is built at runtime, by `createTypeInfo` in ObjCExport.mm,
            // whatever value written here never gets used.
            irClass.isKotlinObjCClass() -> 0
            // The numbering will be done by later by the final link. This runtime should not rely on this field
            // and use `TypeInfo.classHierarchyIds_` instead.
            context.hierarchyWillBeLaidOutByFinalLink -> 0
            !context.hasGlobalHierarchyAnalysis() -> {
                // No global hierarchy optimizations - the checks will go through the backup path
                if (irClass.isInterface) irClass.backupInterfaceId else 0
            }
            irClass.isInterface -> context.globalHierarchyInterfaceId(irClass)
            else -> context.globalHierarchyClassIdInterval(irClass).classIdLo
        }

    /**
     * The id of this interface as this compilation knows it: the one the hierarchy analysis assigned, or a hash
     * of the name where there is no analysis to ask.
     * It is a key for the structures of this compilation alone e.g. the devirtualization analysis, which never outlives it.
     * Anything the rest of the program looks an interface up in goes through [emittedInterfaceId] instead.
     */
    val localInterfaceId: Int
        get() {
            require(irClass.isInterface) { "An interface expected but was ${irClass.name}" }
            return if (context.hasGlobalHierarchyAnalysis()) context.globalHierarchyInterfaceId(irClass)
            else irClass.backupInterfaceId
        }

    /**
     * [localInterfaceId], for the emitted data.
     * Only for global optimizations. In the presence of caches, the final link does a different layout
     * and runtime reads the ids from auxiliary structures.
     */
    val emittedInterfaceId: Int
        get() {
            context.requireOwnsInterfaceNumbering { "The id of the interface ${irClass.render()}" }
            return localInterfaceId
        }

    fun itablePlace(function: IrSimpleFunction): InterfaceTablePlace {
        require(irClass.isInterface) { "An interface expected but was ${irClass.name}" }
        val interfaceVTable = interfaceVTableEntries
        val index = interfaceVTable.indexOf(function)
        if (index >= 0)
            return InterfaceTablePlace(irClass, localInterfaceId, interfaceVTable.size, index)
        val superFunction = function.overriddenSymbols.first().owner
        return context.getLayoutBuilder(superFunction.parentAsClass).itablePlace(superFunction)
    }

    class FieldInfo(val name: String, val type: IrType, val isConst: Boolean, val irFieldSymbol: IrFieldSymbol, val alignment: Int) {
        val irField: IrField?
            get() = if (irFieldSymbol.isBound) irFieldSymbol.owner else null
        init {
            require(alignment.countOneBits() == 1) { "Alignment should be power of 2" }
        }
    }

    /**
     * All fields of the class instance.
     * The fields are in the order they end up in the binary (pseudo fields used for padding are missing).
     * The fields of a subclass may be inside the padding bits of the superclass.
     */
    fun getFields(llvm: CodegenLlvmHelpers): List<FieldInfo> = getFieldsInternal(llvm)

    private var fields: List<FieldInfo>? = null

    // Synchronization is needed due to potential deserialization invocation while building fields for the super classes.
    @Synchronized
    private fun getFieldsInternal(llvm: CodegenLlvmHelpers): List<FieldInfo> {
        fields?.let { return it }

        val superClass = irClass.getSuperClassNotAny()
        val superFields = if (superClass != null) context.getLayoutBuilder(superClass).getFieldsInternal(llvm) else emptyList()
        val declaredFields = getDeclaredFields(llvm)
        val packedFields = packFields(llvm, superFields, declaredFields)

        return packedFields.also { fields = it }
    }

    val associatedObjects by lazy {
        val result = mutableMapOf<IrClass, IrClass>()

        irClass.annotations.forEach {
            val irFile = irClass.fileOrNull

            val annotationClass = it.classSymbol.owner

            if (annotationClass.hasAnnotation(RuntimeNames.associatedObjectKey)) {
                val argument = it.argumentMapping.values.singleOrNull()

                val irClassReference = argument as? IrClassReference
                        ?: error(irFile, argument, "unexpected annotation argument")

                val associatedObject = irClassReference.symbol.owner

                if (associatedObject !is IrClass || !associatedObject.isObject) {
                    error(irFile, irClassReference, "argument is not a singleton")
                }

                if (annotationClass in result) {
                    error(
                            irFile,
                            it,
                            "duplicate value for ${annotationClass.name}, previous was ${result[annotationClass]?.name}"
                    )
                }

                result[annotationClass] = associatedObject
            }
        }

        result
    }

    /**
     * Fields declared in the class.
     */
    fun getDeclaredFields(llvm: CodegenLlvmHelpers): List<FieldInfo> {
        val outerThisField = if (irClass.isInner)
            context.innerClassesSupport.getOuterThisField(irClass)
        else null

        val moduleDeserializer = context.moduleDeserializerProvider.getDeserializerOrNull(irClass)
        if (moduleDeserializer != null) {
            val classFieldsDeserializer = ClassFieldsDeserializer(context.config.cachedLibraries, context.irBuiltIns, moduleDeserializer)
            return classFieldsDeserializer.deserializeClassFields(irClass, outerThisField?.toFieldInfo(llvm))
        }
        val declarations = irClass.declarations.toMutableList()
        outerThisField?.let {
            if (!declarations.contains(it))
                declarations += it
        }
        return declarations.mapNotNull {
            when (it) {
                is IrField -> it.takeIf { it.isReal && !it.isStatic }?.toFieldInfo(llvm)
                is IrProperty -> it.takeIf { it.isReal }?.backingField?.takeIf { !it.isStatic }?.toFieldInfo(llvm)
                else -> null
            }
        }
    }

    /**
     * Given fields of the superclass and the declared fields of the current class, produce an optimally packed
     * order of fields.
     */
    private fun packFields(llvm: CodegenLlvmHelpers, superFields: List<FieldInfo>, declaredFields: List<FieldInfo>): List<FieldInfo> {
        // The NoReorderFields annotation is internal, and only occurs on final classes with no inherited fields.
        if (irClass.hasAnnotation(KonanFqNames.noReorderFields)) {
            return superFields + declaredFields
        }
        val fields = superFields + declaredFields.sortedByDescending {
            with(llvm) { LLVMStoreSizeOfType(runtime.targetData, it.type.toLLVMType(this)) }
        }
        // When packFields is disabled, reorder only the fields inside the current class.
        if (!context.config.packFields) {
            return fields
        }

        // offsetN indicates at what offset, if any, an N-byte appropriately aligned block can be packed.
        // If no such block exists that does not overlap with a better aligned free block, offsetN will be -1.
        var offset1 = -1L
        var offset2 = -1L
        var offset4 = -1L
        var offset8 = 0L // == size of allocated fields rounded up to multiple of 8 bytes

        // Allocates a block of the given size. Sizes smaller than 8 bytes will be rounded up to a power of 2 and
        // aligned according to that size. Other sizes will be rounded up to a multiple of 8 and will be 8 byte aligned.
        fun nextOffset(size: Long): Long = when (size) {
            /* Algorithm:
            N -> {
                if we have a saved block of size N, return it and forget about it
                else split a block of size 2N in two, return the first, and save the second for later.
             */
            1L -> {
                if (offset1 != -1L) offset1.also { offset1 = -1 }
                else nextOffset(2).also { offset1 = it + 1 }
            }
            2L -> {
                if (offset2 != -1L) offset2.also { offset2 = -1 }
                else nextOffset(4).also { offset2 = it + 2 }
            }
            4L -> {
                if (offset4 != -1L) offset4.also { offset4 = -1 }
                else nextOffset(8).also { offset4 = it + 4 }
            }
            /* Base case:
            else -> the size is big enough that it needs one or more 8-byte blocks by itself.
             */
            else -> offset8.also { offset8 += alignTo(size, 8) }
        }

        class IndexedField(val offset: Long, val field: FieldInfo)

        val packedFields = mutableListOf<IndexedField>()
        for (field in fields) {
            val size = LLVMStoreSizeOfType(llvm.runtime.targetData, field.type.toLLVMType(llvm))
            check(size == 1L || size == 2L || size == 4L || size % 8 == 0L)
            check(min(size, 8L) % field.alignment == 0L)
            val offset = nextOffset(size)
            packedFields.add(IndexedField(offset, field))
        }
        packedFields.sortBy { it.offset }
        return packedFields.map { it.field }
    }

    /**
     * Normally, function should be already replaced. But if the function come from LazyIr, it can be not replaced.
     */
    fun IrSimpleFunction.getLoweredVersion() = when {
        isSuspend -> this.getOrCreateFunctionWithContinuationStub(context)
        else -> this
    }
    private val overridableOrOverridingMethods: List<IrSimpleFunction>
        get() = irClass.simpleFunctions()
                .map {it.getLoweredVersion() }
                .filter { it.isOverridableOrOverrides && it.bridgeTarget == null }

    private val IrFunction.uniqueName get() = computeFunctionName()
}
