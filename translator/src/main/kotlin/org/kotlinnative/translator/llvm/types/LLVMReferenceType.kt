package org.kotlinnative.translator.llvm.types

import java.util.*

class LLVMReferenceType(val type: String, val prefix: String = "") : LLVMType() {

    override val align = 4
    override val size: Byte = 4
    override fun toString() = "%$prefix.$type"

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}