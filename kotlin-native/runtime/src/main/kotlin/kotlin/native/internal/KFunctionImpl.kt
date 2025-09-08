/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.internal.throwIrLinkageError
import kotlin.reflect.KFunction
import kotlin.reflect.KType

internal sealed class KFunctionDescription {
    class Correct(
            val flags: Int,
            val arity: Int,
            val fqName: String,
            val name: String,
            val returnType: KType,
    ) : KFunctionDescription()

    class LinkageError(
            val reflectionTargetLinkageError: String,
    ) : KFunctionDescription()
}

internal abstract class KFunctionImpl<out R>(val description: KFunctionDescription) : KFunction<R> {
    final override val name get() = description.checkCorrect().name
    final override val returnType get() = description.checkCorrect().returnType
    val receiver get() = computeReceiver()

    open fun computeReceiver(): Any? = null

    override fun equals(other: Any?): Boolean {
        val desc = description.checkCorrect()
        if (other !is KFunctionImpl<*>) return false
        val otherDesc = other.description.checkCorrect()
        return desc.fqName == otherDesc.fqName && receiver == other.receiver
                && desc.arity == otherDesc.arity && desc.flags == otherDesc.flags
    }

    private fun evalutePolynom(x: Int, vararg coeffs: Int): Int {
        var res = 0
        for (coeff in coeffs)
            res = res * x + coeff
        return res
    }

    override fun hashCode(): Int {
        val desc = description.checkCorrect()
        return evalutePolynom(31, desc.fqName.hashCode(), receiver.hashCode(), desc.arity, desc.flags)
    }

    override fun toString(): String {
        return if (name == "<init>") "constructor" else "function $name"
    }

    private fun KFunctionDescription.checkCorrect(): KFunctionDescription.Correct = when (this) {
        is KFunctionDescription.Correct -> this
        is KFunctionDescription.LinkageError -> throwIrLinkageError(reflectionTargetLinkageError)
    }
}
