/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface ReferenceTypeProvider {
    fun getCls(): ClassType
}

class MethodDelegate<T>(val name: String) : ReadOnlyProperty<ReferenceTypeProvider, MethodDelegate.MethodEvaluator<T>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): MethodEvaluator<T> {
        return MethodEvaluator(thisRef.getCls().methodsByName(name).singleOrNull())
    }

    @Suppress("UNCHECKED_CAST")
    class MethodEvaluator<T>(val method: Method?) {
        fun value(value: ObjectReference?, context: DefaultExecutionContext, vararg values: Value): T? {
            return value?.let {
                method?.let {
                    context.invokeMethodAsObject(value, method, *values) as T?
                }
            }
        }
    }
}

class FieldDelegate<T>(val name: String) : ReadOnlyProperty<ReferenceTypeProvider, FieldDelegate.FieldEvaluator<T>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): FieldEvaluator<T> {
        return FieldEvaluator(thisRef.getCls().fieldByName(name))
    }

    @Suppress("UNCHECKED_CAST")
    class FieldEvaluator<T>(val field: Field?) {
        fun value(value: ObjectReference): T? =
            field?.let { value.getValue(it) as T? }
    }
}
