package kotlin.properties

public object Delegates {
    public final fun </*0*/ T : kotlin.Any> notNull(): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>
    public final inline fun </*0*/ T> observable(/*0*/ initialValue: T, /*1*/ crossinline onChange: (property: kotlin.reflect.KProperty<*>, oldValue: T, newValue: T) -> kotlin.Unit): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>
    public final inline fun </*0*/ T> vetoable(/*0*/ initialValue: T, /*1*/ crossinline onChange: (property: kotlin.reflect.KProperty<*>, oldValue: T, newValue: T) -> kotlin.Boolean): kotlin.properties.ReadWriteProperty<kotlin.Any?, T>
}

public abstract class ObservableProperty</*0*/ V> : kotlin.properties.ReadWriteProperty<kotlin.Any?, V> {
    /*primary*/ public constructor ObservableProperty</*0*/ V>(/*0*/ initialValue: V)
    protected open fun afterChange(/*0*/ property: kotlin.reflect.KProperty<*>, /*1*/ oldValue: V, /*2*/ newValue: V): kotlin.Unit
    protected open fun beforeChange(/*0*/ property: kotlin.reflect.KProperty<*>, /*1*/ oldValue: V, /*2*/ newValue: V): kotlin.Boolean
    public open override /*1*/ fun getValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>): V
    public open override /*1*/ fun setValue(/*0*/ thisRef: kotlin.Any?, /*1*/ property: kotlin.reflect.KProperty<*>, /*2*/ value: V): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.4") public interface PropertyDelegateProvider</*0*/ in T, /*1*/ out D> {
    public abstract operator fun provideDelegate(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>): D
}

public interface ReadOnlyProperty</*0*/ in T, /*1*/ out V> {
    public abstract operator fun getValue(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>): V
}

public interface ReadWriteProperty</*0*/ in T, /*1*/ V> : kotlin.properties.ReadOnlyProperty<T, V> {
    public abstract override /*1*/ fun getValue(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>): V
    public abstract operator fun setValue(/*0*/ thisRef: T, /*1*/ property: kotlin.reflect.KProperty<*>, /*2*/ value: V): kotlin.Unit
}