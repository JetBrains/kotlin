package org.kotlinnative.translator.llvm.types

class LLVMNullType(var baseType: LLVMType? = null) : LLVMType() {

    override val align = 1
    override var size = 0
    override val defaultValue = "null"
    override val mangle = ""
    override val typename = baseType?.typename.orEmpty()

    override fun parseArg(inputArg: String) = "null"
    override fun toString() = baseType?.toString().orEmpty()

    override fun equals(other: Any?) =
            other is LLVMNullType

    override fun hashCode() =
            "null".hashCode()

}