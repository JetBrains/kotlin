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
    fun isCompatible(value: T?): Boolean
}

class MethodMirrorDelegate<T, F>(val name: String,
                                 private val mirrorProvider: MirrorProvider<T, F>,
                                 val signature: String? = null) : ReadOnlyProperty<ReferenceTypeProvider, MethodEvaluator.MirrorMethodEvaluator<T, F>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): MethodEvaluator.MirrorMethodEvaluator<T, F> {
        val methods = if (signature == null) thisRef.getCls().methodsByName(name) else thisRef.getCls().methodsByName(name, signature)
        return MethodEvaluator.MirrorMethodEvaluator(methods.singleOrNull(), mirrorProvider)
    }
}

class MethodDelegate<T>(val name: String, val signature: String? = null) : ReadOnlyProperty<ReferenceTypeProvider, MethodEvaluator<T>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): MethodEvaluator<T> {
        val methods = if (signature == null) thisRef.getCls().methodsByName(name) else thisRef.getCls().methodsByName(name, signature)
        return MethodEvaluator.DefaultMethodEvaluator(methods.singleOrNull())
    }
}

class FieldMirrorDelegate<T, F>(val name: String,
                                private val mirrorProvider: MirrorProvider<T, F>) : ReadOnlyProperty<ReferenceTypeProvider, FieldEvaluator.MirrorFieldEvaluator<T, F>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): FieldEvaluator.MirrorFieldEvaluator<T, F> {
        return FieldEvaluator.MirrorFieldEvaluator(thisRef.getCls().fieldByName(name), thisRef, mirrorProvider)
    }
}

class FieldDelegate<T>(val name: String) : ReadOnlyProperty<ReferenceTypeProvider, FieldEvaluator<T>> {
    override fun getValue(thisRef: ReferenceTypeProvider, property: KProperty<*>): FieldEvaluator<T> {
        return FieldEvaluator.DefaultFieldEvaluator(thisRef.getCls().fieldByName(name), thisRef)
    }
}
