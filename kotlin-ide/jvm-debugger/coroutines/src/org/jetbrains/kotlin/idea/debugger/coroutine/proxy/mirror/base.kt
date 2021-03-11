/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

abstract class BaseMirror<T: ObjectReference, F>(val name: String, context: DefaultExecutionContext) : ReferenceTypeProvider, MirrorProvider<T, F> {
    val log by logger
    private val cls = context.findClassSafe(name) ?: throw IllegalStateException("coroutine-debugger: class $name not found.")

    override fun getCls(): ClassType = cls

    override fun isCompatible(value: T?) =
        value?.referenceType()?.isSubTypeOrSame(name) ?: false

    override fun mirror(value: T?, context: DefaultExecutionContext): F? {
        value ?: return null
        return if (!isCompatible(value)) {
            log.trace("Value ${value.referenceType()} is not compatible with $name.")
            null
        } else
            fetchMirror(value, context)
    }

    protected abstract fun fetchMirror(value: T, context: DefaultExecutionContext): F?
}
