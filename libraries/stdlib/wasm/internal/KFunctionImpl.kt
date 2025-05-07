/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

internal abstract class KFunctionImpl(val flags: Int, val arity: Int, val id: String) {
    protected open fun computeReceiver(): Any? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KFunctionImpl &&
                this.flags == other.flags &&
                this.arity == other.arity &&
                this.id == other.id &&
                this.computeReceiver() == other.computeReceiver()
    }

    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + arity
        result = 31 * result + id.hashCode()
        result = 31 * result + computeReceiver().hashCode()
        return result
    }
}

internal abstract class KFunctionErrorImpl(val message: String) {
    override fun equals(other: Any?): Boolean = throwLinkageError(message)

    override fun hashCode(): Int = throwLinkageError(message)

    override fun toString(): String = throwLinkageError(message)
}