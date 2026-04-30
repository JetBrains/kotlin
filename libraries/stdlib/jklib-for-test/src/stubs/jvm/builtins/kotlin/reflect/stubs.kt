/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

public actual interface KClass<T : Any> : KClassifier {
    public actual val simpleName: String?
    public actual val qualifiedName: String?
    public actual fun isInstance(value: Any?): Boolean
}

public actual interface KFunction<out R> : KCallable<R>, Function<R>

public actual interface KProperty<out V> : KCallable<V>
public actual interface KMutableProperty<V> : KProperty<V>

public actual interface KProperty0<out V> : KProperty<V>, () -> V {
    public actual fun get(): V
}
public actual interface KMutableProperty0<V> : KProperty0<V>, KMutableProperty<V> {
    public actual fun set(value: V)
}

public actual interface KProperty1<T, out V> : KProperty<V>, (T) -> V {
    public actual fun get(receiver: T): V
}
public actual interface KMutableProperty1<T, V> : KProperty1<T, V>, KMutableProperty<V> {
    public actual fun set(receiver: T, value: V)
}

public actual interface KProperty2<D, E, out V> : KProperty<V>, (D, E) -> V {
    public actual fun get(receiver1: D, receiver2: E): V
}
public actual interface KMutableProperty2<D, E, V> : KProperty2<D, E, V>, KMutableProperty<V> {
    public actual fun set(receiver1: D, receiver2: E, value: V)
}

public actual class KTypeProjection

public actual interface KType {
    public actual val classifier: KClassifier?
    public actual val arguments: List<KTypeProjection>
    public actual val isMarkedNullable: Boolean
}

@ExperimentalAssociatedObjects
public actual inline fun <reified T : Annotation> KClass<*>.findAssociatedObject(): Any? {
    TODO("stub")
}
