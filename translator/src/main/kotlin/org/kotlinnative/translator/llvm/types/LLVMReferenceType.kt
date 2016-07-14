package org.kotlinnative.translator.llvm.types

import java.util.*

class LLVMReferenceType(val type: String, var prefix: String = "", var isReturn: Boolean = false) : LLVMType() {

    override val defaultValue: String = ""

    override val align = 4
    override val size: Byte = 4
    override fun toString() = "%$prefix${if (prefix.length > 0) "." else ""}$type"

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}