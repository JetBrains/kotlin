/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.util.ir2string
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

internal fun IrElement.needDebugInfo(context: Context) = context.shouldContainDebugInfo() || (this is IrVariable && this.isVar)

internal class VariableManager(val functionGenerationContext: FunctionGenerationContext) {
    internal interface Record {
        fun load(resultSlot: LLVMValueRef?) : LLVMValueRef
        fun store(value: LLVMValueRef)
        fun address() : LLVMValueRef
    }

    inner class SlotRecord(val address: LLVMValueRef, val type: LLVMTypeRef, val isVar: Boolean, val isObjectType: Boolean) : Record {
        override fun load(resultSlot: LLVMValueRef?) : LLVMValueRef = functionGenerationContext.loadSlot(type, isObjectType, address, isVar, resultSlot)
        override fun store(value: LLVMValueRef) {
            functionGenerationContext.storeAny(value, address, isObjectType, true)
        }
        override fun address() : LLVMValueRef = this.address
        override fun toString() = (if (isObjectType) "refslot" else "slot") + " for ${address}"
    }

    inner class ParameterRecord(val address: LLVMValueRef, val type: LLVMTypeRef, val isObjectType: Boolean) : Record {
        override fun load(resultSlot: LLVMValueRef?): LLVMValueRef = functionGenerationContext.loadSlot(type, isObjectType, address, false, resultSlot)
        override fun store(value: LLVMValueRef) = functionGenerationContext.store(value, address)
        override fun address() : LLVMValueRef = this.address
        override fun toString() = (if (isObjectType) "refslot" else "slot") + " for ${address}"
    }

    class ValueRecord(val value: LLVMValueRef, val name: Name) : Record {
        override fun load(resultSlot: LLVMValueRef?) : LLVMValueRef = value
        override fun store(value: LLVMValueRef) = throw Error("writing to immutable: ${name}")
        override fun address() : LLVMValueRef = throw Error("no address for: ${name}")
        override fun toString() = "value of ${value} from ${name}"
    }

    val variables: ArrayList<Record> = arrayListOf()
    val contextVariablesToIndex: HashMap<IrValueDeclaration, Int> = hashMapOf()

    // Clears inner state of variable manager.
    fun clear() {
        skipSlots = 0
        variables.clear()
        contextVariablesToIndex.clear()
    }

    fun createVariable(valueDeclaration: IrValueDeclaration, value: LLVMValueRef? = null, variableLocation: VariableDebugLocation?) : Int {
        val isVar = valueDeclaration is IrVariable && valueDeclaration.isVar
        // Note that we always create slot for object references for memory management.
        if (!functionGenerationContext.context.shouldContainDebugInfo() && !isVar && value != null)
            return createImmutable(valueDeclaration, value)
        else
            // Unfortunately, we have to create mutable slots here,
            // as even vals can be assigned on multiple paths. However, we use varness
            // knowledge, as anonymous slots are created only for true vars (for vals
            // their single assigner already have slot).
            return createMutable(valueDeclaration, isVar, value, variableLocation)
    }

    internal fun createMutable(valueDeclaration: IrValueDeclaration,
                               isVar: Boolean, value: LLVMValueRef? = null, variableLocation: VariableDebugLocation?) : Int {
        assert(!contextVariablesToIndex.contains(valueDeclaration)) {
            "Could not find ${valueDeclaration.render()} in contextVariablesToIndex"
        }
        val index = variables.size
        val type = valueDeclaration.type.toLLVMType(functionGenerationContext.llvm)
        val isObjectType = valueDeclaration.type.binaryTypeIsReference()
        val slot = functionGenerationContext.alloca(type, isObjectType, valueDeclaration.name.asString(), variableLocation)
        if (value != null)
            functionGenerationContext.storeAny(value, slot, isObjectType, true)
        variables.add(SlotRecord(slot, type, isVar, isObjectType))
        contextVariablesToIndex[valueDeclaration] = index
        return index
    }

    internal var skipSlots = 0
    internal fun createParameterOnStack(valueDeclaration: IrValueDeclaration, variableLocation: VariableDebugLocation?): Int {
        assert(!contextVariablesToIndex.contains(valueDeclaration))
        val index = variables.size
        val type = valueDeclaration.type.toLLVMType(functionGenerationContext.llvm)
        val isObjectType = valueDeclaration.type.binaryTypeIsReference()
        val slot = functionGenerationContext.alloca(
                type, isObjectType, "p-${valueDeclaration.name.asString()}", variableLocation)
        val isObject = valueDeclaration.type.binaryTypeIsReference()
        variables.add(ParameterRecord(slot, type, isObjectType))
        contextVariablesToIndex[valueDeclaration] = index
        if (isObject)
            skipSlots++
        return index
    }

    internal fun createParameter(valueDeclaration: IrValueDeclaration, value: LLVMValueRef) =
            createImmutable(valueDeclaration, value)

    // Creates anonymous mutable variable.
    // Think of slot reuse.
    fun createAnonymousSlot(isObjectType: Boolean, value: LLVMValueRef? = null) : LLVMValueRef {
        val index = createAnonymousMutable(functionGenerationContext.kObjHeaderPtr, isObjectType, value)
        return addressOf(index)
    }

    private fun createAnonymousMutable(type: LLVMTypeRef, isObjectType: Boolean, value: LLVMValueRef? = null) : Int {
        val index = variables.size
        val slot = functionGenerationContext.alloca(type, isObjectType, variableLocation = null)
        if (value != null)
            functionGenerationContext.storeAny(value, slot, isObjectType, true)
        variables.add(SlotRecord(slot, type, true, isObjectType))
        return index
    }

    internal fun createImmutable(valueDeclaration: IrValueDeclaration, value: LLVMValueRef) : Int {
        if (contextVariablesToIndex.containsKey(valueDeclaration))
            throw Error("${ir2string(valueDeclaration)} is already defined")
        val index = variables.size
        variables.add(ValueRecord(value, valueDeclaration.name))
        contextVariablesToIndex[valueDeclaration] = index
        return index
    }

    fun indexOf(valueDeclaration: IrValueDeclaration) : Int {
        return contextVariablesToIndex.getOrElse(valueDeclaration) { -1 }
    }

    fun addressOf(index: Int): LLVMValueRef {
        return variables[index].address()
    }

    fun load(index: Int, resultSlot: LLVMValueRef?): LLVMValueRef {
        return variables[index].load(resultSlot)
    }

    fun store(value: LLVMValueRef, index: Int) {
        variables[index].store(value)
    }
}

internal data class VariableDebugLocation(val localVariable: DILocalVariableRef, val location:DILocationRef?, val file:DIFileRef, val line:Int)

internal fun debugInfoLocalVariableLocation(builder: DIBuilderRef?,
        functionScope: DIScopeOpaqueRef, diType: DITypeOpaqueRef, name:Name, file: DIFileRef, line: Int,
        location: DILocationRef?): VariableDebugLocation {
    val variableDeclaration = DICreateAutoVariable(
            builder = builder,
            scope = functionScope,
            name = name.asString(),
            file = file,
            line = line,
            type = diType)

    return VariableDebugLocation(localVariable = variableDeclaration!!, location = location, file = file, line = line)
}

internal fun debugInfoParameterLocation(builder: DIBuilderRef?,
                                        functionScope: DIScopeOpaqueRef, diType: DITypeOpaqueRef,
                                        name:Name, argNo: Int, file: DIFileRef, line: Int,
                                        location: DILocationRef?): VariableDebugLocation {
    val variableDeclaration = DICreateParameterVariable(
            builder = builder,
            scope = functionScope,
            name = name.asString(),
            argNo = argNo,
            file = file,
            line = line,
            type = diType)

    return VariableDebugLocation(localVariable = variableDeclaration!!, location = location, file = file, line = line)
}
