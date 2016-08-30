package org.kotlinnative.translator.llvm

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.kotlinnative.translator.TranslationState
import org.kotlinnative.translator.llvm.types.*


fun LLVMFunctionDescriptor(name: String, argTypes: List<LLVMVariable>?, returnType: LLVMType, declare: Boolean = false) =
        "${if (declare) "declare" else "define weak"} $returnType @$name(${
        argTypes?.mapIndexed { i: Int, s: LLVMVariable ->
            "${s.pointedType} ${if (s.type is LLVMReferenceType && !s.type.byRef) "byval" else ""} %${s.label}"
        }?.joinToString()}) #0"

fun LLVMInstanceOfStandardType(name: String, type: KotlinType, scope: LLVMScope = LLVMRegisterScope(), state: TranslationState): LLVMVariable {
    val typeName = type.toString().dropLastWhile { it == '?' }
    val pointerMark = if (type.toString().last() == '?') 1 else 0
    return when {
        type.isFunctionTypeOrSubtype -> LLVMVariable(name, LLVMFunctionType(type, state), name, scope, pointer = 1)
        typeName == "Boolean" -> LLVMVariable(name, LLVMBooleanType(), name, scope, pointerMark)
        typeName == "Byte" -> LLVMVariable(name, LLVMByteType(), name, scope, pointerMark)
        typeName == "Char" -> LLVMVariable(name, LLVMCharType(), name, scope, pointerMark)
        typeName == "Short" -> LLVMVariable(name, LLVMShortType(), name, scope, pointerMark)
        typeName == "Int" -> LLVMVariable(name, LLVMIntType(), name, scope, pointerMark)
        typeName == "Long" -> LLVMVariable(name, LLVMLongType(), name, scope, pointerMark)
        typeName == "Float" -> LLVMVariable(name, LLVMFloatType(), name, scope, pointerMark)
        typeName == "Double" -> LLVMVariable(name, LLVMDoubleType(), name, scope, pointerMark)
        typeName == "String" -> LLVMVariable(name, LLVMStringType(0), name, scope, pointerMark)
        type.nameIfStandardType.toString() == "Nothing" -> LLVMVariable(name, LLVMNullType(), name, scope)
        type.isUnit() -> LLVMVariable("", LLVMVoidType(), name, scope)
        type.isMarkedNullable -> LLVMVariable(name, LLVMReferenceType(typeName, prefix = "class"), name, scope, pointer = pointerMark)
        else -> {
            val declarationDescriptor = type.constructor.declarationDescriptor!!
            val refName = declarationDescriptor.fqNameSafe.asString()
            val refType = state.classes[type.toString()]?.type ?: LLVMReferenceType(refName, align = TranslationState.pointerAlign, prefix = "class")

            LLVMVariable(name, refType, name, scope, pointer = 1)
        }
    }
}

fun LLVMMapStandardType(type: KotlinType, state: TranslationState) =
        LLVMInstanceOfStandardType("type", type, LLVMRegisterScope(), state).type

fun String.addBeforeIfNotEmpty(add: String): String =
        if (this.length > 0) add + this else this

fun String.addAfterIfNotEmpty(add: String): String =
        if (this.length > 0) this + add else this

fun String.indexOfOrLast(str: Char, startIndex: Int = 0): Int {
    val pos = this.indexOf(str, startIndex)
    return if (pos < 0) this.length else pos
}

fun FqName.convertToNativeName(): String =
        this.asString().replace(".<init>", "")