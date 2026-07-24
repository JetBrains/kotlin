/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.throwIrLinkageError
import kotlin.reflect.KFunction
import kotlin.reflect.KType

@UsedFromCompilerGeneratedCode
internal sealed class KFunctionDescription {
    abstract val name: String

    class Correct(
            val flags: Int,
            val arity: Int,
            val boundValueCount: Int,
            val fqName: String,
            override val name: String,
            val returnType: KType,
    ) : KFunctionDescription()

    class LinkageError(
            override val name: String,
            val reflectionTargetLinkageError: String,
    ) : KFunctionDescription()
}

@UsedFromCompilerGeneratedCode
internal abstract class KFunctionImpl<out R>(val description: KFunctionDescription) : KFunction<R> {
    final override val name get() = description.name
    final override val returnType get() = description.checkCorrect().returnType

    open fun boundValueAt(index: Int): Any? = null

    override fun equals(other: Any?): Boolean {
        val desc = description.checkCorrect()
        if (other !is KFunctionImpl<*>) return false
        val otherDesc = other.description.checkCorrect()
        if (desc.fqName != otherDesc.fqName || desc.arity != otherDesc.arity ||
                desc.flags != otherDesc.flags || desc.boundValueCount != otherDesc.boundValueCount) return false

        repeat(desc.boundValueCount) { index ->
            if (boundValueAt(index) != other.boundValueAt(index)) return false
        }

        return true
    }

    override fun hashCode(): Int {
        val desc = description.checkCorrect()
        var res = desc.fqName.hashCode()
        res = res * 31 + desc.arity
        res = res * 31 + desc.flags

        repeat(desc.boundValueCount) { index ->
            res = res * 31 + boundValueAt(index).hashCode()
        }
        return res
    }

    // Although this function uses only the name property (which is unconditionally available), the linkage error is checked for consistency between backends.
    override fun toString(): String {
        val nameStrict = description.checkCorrect().name
        return if (nameStrict == "<init>") "constructor" else "function $nameStrict"
    }

    protected fun KFunctionDescription.checkCorrect(): KFunctionDescription.Correct = when (this) {
        is KFunctionDescription.Correct -> this
        is KFunctionDescription.LinkageError -> throwIrLinkageError(reflectionTargetLinkageError)
    }
}
