/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KType
import kotlin.reflect.KFunction

@FixmeReflection
internal abstract class KSuspendFunctionImpl<out R>(
        override val name: String, val fqName: String,
        val bound: Boolean, val receiver: Any?,
        override val returnType: KType
): KFunction<R> {

    override fun equals(other: Any?): Boolean {
        if (other !is KSuspendFunctionImpl<*>) return false
        return fqName == other.fqName && bound == other.bound && receiver == other.receiver
    }

    override fun hashCode(): Int {
        return (fqName.hashCode() * 31 + if (bound) 1 else 0) * 31 + receiver.hashCode()
    }

    override fun toString(): String {
        return "suspend function $name"
    }
}
