package org.kotlinnative.translator.llvm

import org.kotlinnative.translator.exceptions.UnimplementedException
import org.kotlinnative.translator.llvm.types.LLVMType

open class LLVMSingleValue(open val type: LLVMType? = null, open var pointer: Int = 0) : LLVMNode() {

    open fun getType(): String = throw UnimplementedException()

}
