package org.kotlinnative.translator.llvm.types

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.LLVMExpression
import org.kotlinnative.translator.llvm.LLVMSingleValue

abstract class LLVMType() : Cloneable {

    companion object {
        fun mangleFunctionArguments(names: List<LLVMSingleValue>) =
                mangleFunctionTypes(names.map { it.type!! })

        fun mangleFunctionTypes(names: List<LLVMType>) =
                if (names.size > 0) "_${names.joinToString(separator = "_", transform = { it.mangle })}" else ""

        fun nullOrVoidType(type: LLVMType): Boolean =
                (type is LLVMNullType) or (type is LLVMVoidType)

        fun isReferredType(type: LLVMType?): Boolean =
                (type is LLVMNullType) or (type is LLVMReferenceType)

    }

    abstract val align: Int
    abstract val typename: String
    abstract var size: Int
    abstract val defaultValue: String
    abstract val mangle: String
    open val isPrimitive: Boolean = false

    open fun parseArg(inputArg: String) = inputArg
    open fun convertFrom(source: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    override fun toString() = typename

    open fun operatorPlus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorTimes(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorMinus(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorDiv(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorLt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGt(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorLeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorGeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorEq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorNeq(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorOr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorAnd(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorXor(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorShl(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorShr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorUshr(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorMod(firstOp: LLVMSingleValue, secondOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorInc(firstOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()
    open fun operatorDec(firstOp: LLVMSingleValue): LLVMExpression = throw UnimplementedException()

}