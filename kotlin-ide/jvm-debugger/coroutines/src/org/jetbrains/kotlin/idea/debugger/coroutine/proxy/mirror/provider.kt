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

interface MirrorProvider<T, F> {
    fun mirror(value: T?, context: DefaultExecutionContext): F?
}


class MethodMirrorDelegate<T, F>(val name: String, private val mirrorProvider: MirrorProvider<T,F>) : ReadOnlyProperty<ReferenceTypeProvider, MethodMirrorDelegate.MethodEvaluator<T, F>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): MethodEvaluator<T, F> {
        return MethodEvaluator(thisRef.getCls().methodsByName(name).singleOrNull(), mirrorProvider)
    }

    @Suppress("UNCHECKED_CAST")
    class MethodEvaluator<T, F>(val method: Method?,private val mirrorProvider: MirrorProvider<T,F>) {
        fun value(value: ObjectReference?, context: DefaultExecutionContext, vararg values: Value): T? {
            return value?.let {
                method?.let {
                    context.invokeMethodAsObject(value, method, *values) as T?
                }
            }
        }

        fun mirror(ref: ObjectReference, context: DefaultExecutionContext): F? {
            return mirrorProvider.mirror(value(ref, context), context)
        }
    }
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

class FieldMirrorDelegate<T, F>(val name: String,private val mirrorProvider: MirrorProvider<T,F>) : ReadOnlyProperty<ReferenceTypeProvider, FieldMirrorDelegate.FieldEvaluator<T, F>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): FieldEvaluator<T, F> {
        return FieldEvaluator(thisRef.getCls().fieldByName(name), mirrorProvider)
    }

    @Suppress("UNCHECKED_CAST")
    class FieldEvaluator<T, F>(val field: Field?, private val mirrorProvider: MirrorProvider<T, F>) {
        fun value(value: ObjectReference): T? =
                field?.let { value.getValue(it) as T? }

        fun mirror(ref: ObjectReference, context: DefaultExecutionContext): F? {
            return mirrorProvider.mirror(value(ref), context)
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

        fun jopa() = "1"
    }
}
