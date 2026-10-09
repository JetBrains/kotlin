/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import llvm.LLVMABIAlignmentOfType
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
     * The value to put into `TypeInfo.classId_`: the id of an interface in the interface numbering, the start
     * of the DFS interval of a class, and zero where neither is known here.
     */
    val classId: Int
        get() {
            // The type info of such an obj class is built at runtime, by `createTypeInfo` in ObjCExport.mm,
            // whatever value written here never gets used.
            if (irClass.isKotlinObjCClass()) return 0

            if (context.hasGlobalHierarchyAnalysis()) {
                if (irClass.isInterface) return context.globalHierarchyInterfaceId(irClass)
                if (!context.hierarchyWillBeLaidOutByFinalLink) return context.globalHierarchyClassIdInterval(irClass).classIdLo
            }

            if (irClass.isInterface) return irClass.backupInterfaceId
            return 0
        }

    fun itablePlace(function: IrSimpleFunction): InterfaceTablePlace {
        require(irClass.isInterface) { "An interface expected but was ${irClass.name}" }
        val interfaceVTable = interfaceVTableEntries
        val index = interfaceVTable.indexOf(function)
        if (index >= 0)
            return InterfaceTablePlace(irClass, classId, interfaceVTable.size, index)
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
     * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
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
        val sortedDeclaredFields = if (irClass.hasAnnotation(KonanFqNames.noReorderFields))
            declaredFields
        else
            declaredFields.sortedByDescending {
                with(llvm) { LLVMStoreSizeOfType(runtime.targetData, it.type.toLLVMType(this)) }
            }

        return (superFields + sortedDeclaredFields).also { fields = it }
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
