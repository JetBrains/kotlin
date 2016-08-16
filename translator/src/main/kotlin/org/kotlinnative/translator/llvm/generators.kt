package org.kotlinnative.translator.llvm

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.cfg.pseudocode.getSubtypesPredicate
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.types.*


fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false, arm: Boolean = false) =
        "${if (declare) "declare" else "define weak"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.getType()} ${if (s.type is LLVMReferenceType && !(s.type as LLVMReferenceType).byRef) "byval" else ""} %${s.label}"
        }?.joinToString()}) #0"

fun LLVMInstanceOfStandardType(name: String, type: KotlinType, scope: LLVMScope = LLVMRegisterScope(), state: TranslationState): LLVMVariable = when {
    type.isFunctionTypeOrSubtype -> LLVMVariable(name, LLVMFunctionType(type, state), name, scope, pointer = 1)
    type.toString() == "Boolean" -> LLVMVariable(name, LLVMBooleanType(), name, scope)
    type.toString() == "Byte" -> LLVMVariable(name, LLVMByteType(), name, scope)
    type.toString() == "Char" -> LLVMVariable(name, LLVMCharType(), name, scope)
    type.toString() == "Short" -> LLVMVariable(name, LLVMShortType(), name, scope)
    type.toString() == "Int" -> LLVMVariable(name, LLVMIntType(), name, scope)
    type.toString() == "Long" -> LLVMVariable(name, LLVMLongType(), name, scope)
    type.toString() == "Float" -> LLVMVariable(name, LLVMFloatType(), name, scope)
    type.toString() == "Double" -> LLVMVariable(name, LLVMDoubleType(), name, scope)
    type.toString() == "String" -> LLVMVariable(name, LLVMStringType(0), name, scope)
    type.nameIfStandardType.toString() == "Nothing" -> LLVMVariable(name, LLVMNullType(), name, scope)
    type.isUnit() -> LLVMVariable("", LLVMVoidType(), name, scope)
    type.isMarkedNullable -> LLVMVariable(name, LLVMReferenceType(type.toString().dropLast(1), prefix = "class"), name, scope, pointer = 1)
    else -> {
        val refType = state.classes[type.toString()]?.type ?: LLVMReferenceType(type.toString(), align = state.pointerAlign, prefix = "class")

        val result = LLVMVariable(name, refType, name, scope, pointer = 1)
        refType.location.addAll(type.getSubtypesPredicate().toString().split(".").dropLast(1))
        result
    }
}

fun LLVMMapStandardType(type: KotlinType, state: TranslationState): LLVMType =
        LLVMInstanceOfStandardType("type", type, LLVMRegisterScope(), state).type
