/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.*


internal sealed class SyntheticJavaPropertyAccessor<out P, out V, out R>(override val property: P) :
    KProperty.Accessor<V>, KFunction<R> where P : KProperty<V> {
    override val isInline: Boolean by NoReflection
    final override val isExternal: Boolean by NoReflection
    final override val isOperator: Boolean by NoReflection
    final override val isInfix: Boolean by NoReflection
    final override val isSuspend: Boolean by NoReflection
    final override val annotations: List<Annotation> by NoReflection
    final override val parameters: List<KParameter> by NoReflection
    final override val returnType: KType by NoReflection
    final override val typeParameters: List<KTypeParameter> by NoReflection
    final override val visibility: KVisibility? by NoReflection
    final override val isFinal: Boolean by NoReflection
    final override val isOpen: Boolean by NoReflection
    final override val isAbstract: Boolean by NoReflection

    final override fun callBy(args: Map<KParameter, Any?>): R = reportNoReflectionForSyntheticJavaProperties()
    final override fun equals(other: Any?): Boolean = (other as? SyntheticJavaPropertyAccessor<*, *, *>)?.property == property
    final override fun hashCode(): Int = property.hashCode()
}

internal sealed class SyntheticJavaPropertyGetter<out P, out V>(property: P) :
    SyntheticJavaPropertyAccessor<P, V, V>(property) where P : KProperty<V> {
    final override val name: String
        get() = "get-${property.name}"

    final override fun toString(): String = "getter of $property"
}

internal sealed class SyntheticJavaPropertySetter<out P, out V>(property: P) :
    SyntheticJavaPropertyAccessor<P, V, Unit>(property) where P : KProperty<V> {
    final override val name: String
        get() = "set-${property.name}"

    final override fun toString(): String = "setter of $property"
}

internal class SyntheticJavaPropertyReference0Getter<out V>(property: KProperty0<V>) :
    SyntheticJavaPropertyGetter<KProperty0<V>, V>(property), KProperty0.Getter<V> {
    override fun invoke(): V = property.get()

    override fun call(vararg args: Any?): V {
        checkArguments(0, args)
        return property.get()
    }
}

internal class SyntheticJavaPropertyReference1Getter<T, out V>(property: KProperty1<T, V>) :
    SyntheticJavaPropertyGetter<KProperty1<T, V>, V>(property), KProperty1.Getter<T, V> {
    override fun invoke(receiver: T): V = property.get(receiver)
    override fun call(vararg args: Any?): V {
        checkArguments(1, args)
        @Suppress("UNCHECKED_CAST")
        return property.get(args[0] as T)
    }
}

internal class SyntheticJavaPropertyReference0Setter<V>(property: KMutableProperty0<V>) :
    SyntheticJavaPropertySetter<KMutableProperty0<V>, V>(property), KMutableProperty0.Setter<V> {
    override fun invoke(value: V) {
        property.set(value)
    }

    override fun call(vararg args: Any?) {
        checkArguments(1, args)
        @Suppress("UNCHECKED_CAST")
        property.set(args[0] as V)
    }
}

internal class SyntheticJavaPropertyReference1Setter<T, V>(property: KMutableProperty1<T, V>) :
    SyntheticJavaPropertySetter<KMutableProperty1<T, V>, V>(property), KMutableProperty1.Setter<T, V> {
    override fun invoke(receiver: T, value: V) {
        property.set(receiver, value)
    }

    override fun call(vararg args: Any?) {
        checkArguments(2, args)
        @Suppress("UNCHECKED_CAST")
        property.set(args[0] as T, args[1] as V)
    }
}

internal fun checkArguments(arity: Int, args: Array<*>) {
    if (arity != args.size) {
        throw IllegalArgumentException("Callable expects $arity arguments, but ${args.size} were provided.")
    }
}

internal fun reportNoReflectionForSyntheticJavaProperties(): Nothing {
    throw UnsupportedOperationException("Kotlin reflection is not yet supported for synthetic Java properties")
}

private object NoReflection {
    operator fun <T> getValue(any: Any, property: KProperty<*>): T = reportNoReflectionForSyntheticJavaProperties()
}