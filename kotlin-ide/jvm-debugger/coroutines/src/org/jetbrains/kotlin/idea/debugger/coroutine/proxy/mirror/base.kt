/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

abstract class BaseDynamicMirror<T>(val value: ObjectReference, val name: String, val context: DefaultExecutionContext) {
    val log by logger

    private val cls: ReferenceType? = value.referenceType()

    private fun makeField(fieldName: String): Field? =
        cls?.fieldByName(fieldName)

    fun findMethod(methodName: String): Method? =
        cls?.methodsByName(methodName)?.single()

    fun findMethod(methodName: String, signature: String): Method? =
        cls?.methodsByName(methodName, signature)?.single()

    fun isCompatible(value: ObjectReference?) =
        value?.referenceType()?.isSubTypeOrSame(name) ?: false

    fun mirror(): T? {
        return if (!isCompatible(value)) {
            log.trace("Value ${value.referenceType()} is not compatible with $name.")
            null
        } else
            fetchMirror(value, context)
    }

    fun staticObjectValue(fieldName: String): ObjectReference? {
        val keyFieldRef = makeField(fieldName)
        return cls?.let { it.getValue(keyFieldRef) as? ObjectReference }
    }

    fun staticMethodValue(instance: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
        instance?.let {
            method?.let { m ->
                context.invokeMethod(it, m, values.asList()) as? ObjectReference
            }
        }

    fun staticMethodValue(method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
        cls?.let {
            method?.let {
                if (cls is ClassType)
                    context.invokeMethodSafe(cls, method, values.asList()) as? ObjectReference
                else
                    null
            }
        }

    fun stringValue(value: ObjectReference, field: Field?) =
        field?.let {
            (value.getValue(it) as? StringReference)?.value()
        }

    fun byteValue(value: ObjectReference, field: Field?) =
        field?.let {
            (value.getValue(it) as? ByteValue)?.value()
        }

    fun threadValue(value: ObjectReference, field: Field?) =
        field?.let {
            value.getValue(it) as? ThreadReference
        }

    fun stringValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext) =
        method?.let {
            (context.invokeMethod(value, it, emptyList()) as? StringReference)?.value()
        }

    fun objectValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        value?.let {
            method?.let {
                context.invokeMethodAsObject(value, method, *values)
            }
        }

    fun longValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        method?.let { (context.invokeMethod(value, it, values.asList()) as? LongValue)?.longValue() }

    fun intValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
        method?.let { (context.invokeMethod(value, it, values.asList()) as? IntegerValue)?.intValue() }

    fun booleanValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value): Boolean? {
        value ?: return null
        method ?: return null
        return (context.invokeMethod(value, method, values.asList()) as? BooleanValue)?.booleanValue()
    }

    fun objectValue(value: ObjectReference, field: Field?) =
        field?.let { value.getValue(it) as ObjectReference? }

    fun intValue(value: ObjectReference, field: Field?) =
        field?.let { (value.getValue(it) as? IntegerValue)?.intValue() }

    fun longValue(value: ObjectReference, field: Field?) =
        field?.let { (value.getValue(it) as? LongValue)?.longValue() }

    fun booleanValue(value: ObjectReference?, field: Field?) =
        field?.let { (value?.getValue(field) as? BooleanValue)?.booleanValue() }

    protected abstract fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): T?
}

abstract class BaseMirror<T: ObjectReference, F>(val name: String, context: DefaultExecutionContext) : ReferenceTypeProvider, MirrorProvider<T, F> {
    val log by logger
    private val cls = context.findClassSafe(name) ?: throw IllegalStateException("coroutine-debugger: class $name not found.")

    override fun getCls(): ClassType = cls

    fun makeField(fieldName: String): Field? =
            cls.fieldByName(fieldName)

    fun makeMethod(methodName: String): Method? =
            cls.methodsByName(methodName).singleOrNull()


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

    fun staticObjectValue(fieldName: String): ObjectReference? {
        val keyFieldRef = makeField(fieldName)
        return cls.let { it.getValue(keyFieldRef) as? ObjectReference }
    }

    fun staticMethodValue(instance: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
            instance?.let {
                method?.let { m ->
                    context.invokeMethod(it, m, values.asList()) as? ObjectReference
                }
            }

    fun staticMethodValue(method: Method?, context: DefaultExecutionContext, vararg values: Value?) =
            cls.let {
                method?.let {
                    context.invokeMethodSafe(cls, method, values.asList()) as? ObjectReference
                }
            }

    fun stringValue(value: ObjectReference, field: Field?) =
            field?.let {
                (value.getValue(it) as? StringReference)?.value()
            }

    fun byteValue(value: ObjectReference, field: Field?) =
            field?.let {
                (value.getValue(it) as? ByteValue)?.value()
            }

    fun threadValue(value: ObjectReference, field: Field?) =
            field?.let {
                value.getValue(it) as? ThreadReference
            }

    fun stringValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext) =
            method?.let {
                (context.invokeMethod(value, it, emptyList()) as? StringReference)?.value()
            }

    fun objectValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
            value?.let {
                method?.let {
                    context.invokeMethodAsObject(value, method, *values)
                }
            }

    fun longValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
            method?.let { (context.invokeMethod(value, it, values.asList()) as? LongValue)?.longValue() }

    fun intValue(value: ObjectReference, method: Method?, context: DefaultExecutionContext, vararg values: Value) =
            method?.let { (context.invokeMethod(value, it, values.asList()) as? IntegerValue)?.intValue() }

    fun booleanValue(value: ObjectReference?, method: Method?, context: DefaultExecutionContext, vararg values: Value): Boolean? {
        value ?: return null
        method ?: return null
        return (context.invokeMethod(value, method, values.asList()) as? BooleanValue)?.booleanValue()
    }

    fun objectValue(value: ObjectReference, field: Field?) =
            field?.let { value.getValue(it) as ObjectReference? }

    fun intValue(value: ObjectReference, field: Field?) =
            field?.let { (value.getValue(it) as? IntegerValue)?.intValue() }

    fun longValue(value: ObjectReference, field: Field?) =
            field?.let { (value.getValue(it) as? LongValue)?.longValue() }

    fun booleanValue(value: ObjectReference?, field: Field?) =
            field?.let { (value?.getValue(field) as? BooleanValue)?.booleanValue() }

    protected abstract fun fetchMirror(value: T, context: DefaultExecutionContext): F?
}
