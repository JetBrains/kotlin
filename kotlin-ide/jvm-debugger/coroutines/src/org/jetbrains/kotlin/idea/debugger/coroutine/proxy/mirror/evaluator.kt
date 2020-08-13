package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import java.lang.IllegalArgumentException

sealed class MethodEvaluator<T>(val method: Method?) {
    fun value(value: ObjectReference?, context: DefaultExecutionContext, vararg values: Value): T? {
        return method?.let {
            return if (method.isStatic) {
                // pass extension methods like the usual ones.
                val args = if (value != null) {
                    listOf(value) + values.toList()
                } else
                    values.toList()
                @Suppress("UNCHECKED_CAST")
                context.invokeMethod(method.declaringType() as ClassType, method, args) as T?
            } else if (value != null)
                @Suppress("UNCHECKED_CAST")
                context.invokeMethod(value, method, values.toList()) as T?
            else
                throw IllegalArgumentException("Exception while calling method " + method.signature() + " with an empty value.")
        }
    }

    class DefaultMethodEvaluator<T>(method: Method?) : MethodEvaluator<T>(method)

    class MirrorMethodEvaluator<T, F>(method: Method?, private val mirrorProvider: MirrorProvider<T, F>): MethodEvaluator<T>(method) {
        fun mirror(ref: ObjectReference, context: DefaultExecutionContext, vararg values: Value): F? {
            return mirrorProvider.mirror(value(ref, context, *values), context)
        }

        fun isCompatible(value: T) = mirrorProvider.isCompatible(value)
    }
}

sealed class FieldEvaluator<T>(val field: Field?, val thisRef: ReferenceTypeProvider) {
    @Suppress("UNCHECKED_CAST")
    fun value(value: ObjectReference): T? =
            field?.let { value.getValue(it) as T? }

    @Suppress("UNCHECKED_CAST")
    fun staticValue(): T? = thisRef.getCls().let { it.getValue(field) as? T }

    class DefaultFieldEvaluator<T>(field: Field?, thisRef: ReferenceTypeProvider) : FieldEvaluator<T>(field, thisRef)

    class MirrorFieldEvaluator<T, F>(field: Field?, thisRef: ReferenceTypeProvider, private val mirrorProvider: MirrorProvider<T, F>) : FieldEvaluator<T>(field, thisRef) {
        fun mirror(ref: ObjectReference, context: DefaultExecutionContext): F? =
                mirrorProvider.mirror(value(ref), context)

        fun mirrorOnly(value: T, context: DefaultExecutionContext) = mirrorProvider.mirror(value, context)

        fun isCompatible(value: T) = mirrorProvider.isCompatible(value)
    }
}
