/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KFunction
import kotlin.reflect.KType

@FixmeReflection
open class KFunctionImpl<out R>(override val name: String, val fqName: String, val bound: Boolean, val receiver: Any?,
                                override val returnType: KType): KFunction<R> {
    override fun equals(other: Any?): Boolean {
        if (other !is KFunctionImpl<*>) return false
        return fqName == other.fqName && bound == other.bound && receiver == other.receiver
    }

    override fun hashCode(): Int {
        return (fqName.hashCode() * 31 + if (bound) 1 else 0) * 31 + receiver.hashCode()
    }

    override fun toString(): String {
        return "${if (name == "<init>") "constructor" else "function " + name}"
    }
}