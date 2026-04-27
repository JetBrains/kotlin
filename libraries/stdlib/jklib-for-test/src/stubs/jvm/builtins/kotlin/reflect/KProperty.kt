package kotlin.reflect
public actual interface KProperty<out V> : KCallable<V>
public actual interface KProperty0<out V> : KProperty<V>, () -> V
public actual interface KProperty1<T, out V> : KProperty<V>, (T) -> V
public actual interface KProperty2<D, E, out V> : KProperty<V>, (D, E) -> V
public actual interface KMutableProperty<V> : KProperty<V>
public actual interface KMutableProperty0<V> : KProperty0<V>, KMutableProperty<V>
public actual interface KMutableProperty1<T, V> : KProperty1<T, V>, KMutableProperty<V>
public actual interface KMutableProperty2<D, E, V> : KProperty2<D, E, V>, KMutableProperty<V>
