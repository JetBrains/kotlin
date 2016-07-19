package org.kotlinnative.translator.llvm.types

import java.util.*

class LLVMReferenceType(val type: String, var prefix: String = "", override val align: Int = 4, var byRef: Boolean = true) : LLVMType() {

    override val defaultValue: String = ""

    override var size: Int = 4
    override fun toString() = "%$prefix${if (prefix.length > 0) "." else ""}$type"

    private val params = ArrayList<String>()

    fun addParam(param: String) {
        params.add(param)
    }
}