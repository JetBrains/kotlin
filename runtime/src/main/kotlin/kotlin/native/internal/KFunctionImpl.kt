/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KFunction
import kotlin.reflect.KType

@FixmeReflection
internal abstract class KFunctionImpl<out R>(
        override val name: String, val fqName: String, val receiver: Any?,
        val arity: Int, val flags: Int, override val returnType: KType
): KFunction<R> {
    override fun equals(other: Any?): Boolean {
        if (other !is KFunctionImpl<*>) return false
        return fqName == other.fqName && receiver == other.receiver
                && arity == other.arity && flags == other.flags
    }

    private fun evalutePolynom(x: Int, vararg coeffs: Int): Int {
        var res = 0
        for (coeff in coeffs)
            res = res * x + coeff
        return res
    }

    override fun hashCode() = evalutePolynom(31, fqName.hashCode(), receiver.hashCode(), arity, flags)

    override fun toString(): String {
        return "${if (name == "<init>") "constructor" else "function " + name}"
    }
}