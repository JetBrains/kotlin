/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.ListVariable
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.VariableImpl
import com.intellij.debugger.streams.trace.impl.handler.type.ListType

class KotlinListVariable(override val type: ListType, name: String) : VariableImpl(type, name), ListVariable {
    override operator fun get(index: Expression): Expression = call("get", index)
    override operator fun set(index: Expression, newValue: Expression): Expression = call("set", index, newValue)
    override fun add(element: Expression): Expression = call("add", element)

    override fun contains(element: Expression): Expression = call("contains", element)

    override fun size(): Expression = property("size")

    override fun defaultDeclaration(): VariableDeclaration =
        KotlinVariableDeclaration(this, false, type.defaultValue)
}