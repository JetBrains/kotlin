public object Delegates {
    public final fun <T : kotlin.Any> notNull(): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>

    public final inline fun <T> observable(initialValue: T, crossinline onChange: (property: kotlin.reflect.KProperty<*>, oldValue: T, newValue: T) -> kotlin.Unit): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>

    public final inline fun <T> vetoable(initialValue: T, crossinline onChange: (property: kotlin.reflect.KProperty<*>, oldValue: T, newValue: T) -> kotlin.Boolean): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>
}

public abstract class ObservableProperty<V> : kotlin.properties.ReadWriteProperty<kotlin.Any?, V> {
    public constructor ObservableProperty<V>(initialValue: V)

    protected open fun afterChange(property: kotlin.reflect.KProperty<*>, oldValue: V, newValue: V): kotlin.Unit

    protected open fun beforeChange(property: kotlin.reflect.KProperty<*>, oldValue: V, newValue: V): kotlin.Boolean

    public open override operator fun getValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>): V

    public open override operator fun setValue(thisRef: kotlin.Any?, property: kotlin.reflect.KProperty<*>, value: V): kotlin.Unit

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.4")
public fun interface PropertyDelegateProvider<in T, out D> {
    public abstract operator fun provideDelegate(thisRef: T, property: kotlin.reflect.KProperty<*>): D
}

public fun interface ReadOnlyProperty<in T, out V> {
    public abstract operator fun getValue(thisRef: T, property: kotlin.reflect.KProperty<*>): V
}

public interface ReadWriteProperty<in T, V> : kotlin.properties.ReadOnlyProperty<T, V> {
    public abstract override operator fun getValue(thisRef: T, property: kotlin.reflect.KProperty<*>): V

    public abstract operator fun setValue(thisRef: T, property: kotlin.reflect.KProperty<*>, value: V): kotlin.Unit
}