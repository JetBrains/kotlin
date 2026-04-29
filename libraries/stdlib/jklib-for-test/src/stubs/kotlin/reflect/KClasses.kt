package kotlin.reflect

public interface KClass<T : Any> : KClassifier
public interface KCallable<out R>
public interface KClassifier
public interface KFunction<out R> : KCallable<R>
public interface KProperty<out V> : KCallable<V>
public interface KProperty0<out V> : KProperty<V>
public interface KProperty1<T, out V> : KProperty<V>
public interface KProperty2<D, E, out V> : KProperty<V>
public interface KMutableProperty<V> : KProperty<V>
public interface KMutableProperty0<V> : KProperty0<V>, KMutableProperty<V>
public interface KMutableProperty1<T, V> : KProperty1<T, V>, KMutableProperty<V>
public interface KMutableProperty2<D, E, V> : KProperty2<D, E, V>, KMutableProperty<V>
public interface KType
public interface KTypeParameter : KClassifier
public interface KTypeProjection
public enum class KVariance { INVARIANT, IN, OUT }
public annotation class TypeOf

@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public inline fun <reified T> typeOf(): KType = error("stub")
