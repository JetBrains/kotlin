/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.reflect.KFunction
import kotlin.internal.throwIrLinkageError
import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal abstract class KFunctionImpl<out R>(val flags: Int, val arity: Int, val id: String, val boundValueCount: Int, override val name: String) : KFunction<R> {
    open fun boundValueAt(index: Int): Any? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KFunctionImpl<*>) return false
        if (this.flags != other.flags || this.arity != other.arity ||
            this.id != other.id || this.boundValueCount != other.boundValueCount
        ) return false

        repeat(boundValueCount) { index ->
            if (boundValueAt(index) != other.boundValueAt(index)) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + arity
        result = 31 * result + id.hashCode()

        repeat(boundValueCount) { index ->
            result = 31 * result + boundValueAt(index).hashCode()
        }
        // name does not need to be hashed explicitly, since id is a
        // hash of fqName which contains name.
        return result
    }
}

// Old (contrary to its name) version to be removed once bootstrap compiler gets updated.
@UsedFromCompilerGeneratedCode
internal abstract class KFunctionImplNew<out R>(val flags: Int, val arity: Int, val id: String, val receiver: Any?, override val name: String) : KFunction<R> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KFunctionImplNew<*> &&
            this.flags == other.flags &&
            this.arity == other.arity &&
            this.id == other.id &&
            this.receiver == other.receiver
    }

    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + arity
        result = 31 * result + id.hashCode()
        result = 31 * result + receiver.hashCode()
        // name does not need to be hashed explicitly, since id is a
        // hash of fqName which contains name.
        return result
    }
}

@UsedFromCompilerGeneratedCode
internal abstract class KFunctionErrorImpl(val message: String, val name: String) {
    override fun equals(other: Any?): Boolean = throwIrLinkageError(message)

    override fun hashCode(): Int = throwIrLinkageError(message)

    override fun toString(): String = throwIrLinkageError(message)
}
