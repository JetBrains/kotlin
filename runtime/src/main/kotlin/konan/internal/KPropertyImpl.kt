package konan.internal

import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KProperty2
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KMutableProperty2
import kotlin.UnsupportedOperationException

@FixmeReflection
open class KProperty0Impl<out R>(override val name: String, val getter: () -> R): KProperty0<R> {
    override fun get(): R {
        return getter()
    }
    override fun invoke(): R {
        return getter()
    }

}

@FixmeReflection
open class KProperty1Impl<T, out R>(override val name: String, val getter: (T) -> R): KProperty1<T, R> {
    override fun get(receiver: T): R {
        return getter(receiver)
    }
    override fun invoke(receiver: T): R {
        return getter(receiver)
    }
}

@FixmeReflection
open class KProperty2Impl<T1, T2, out R>(override val name: String, val getter: (T1, T2) -> R): KProperty2<T1, T2, R> {
    override fun get(receiver1: T1, receiver2: T2): R {
        return getter(receiver1, receiver2)
    }
    override fun invoke(receiver1: T1, receiver2: T2): R {
        return getter(receiver1, receiver2)
    }
}

@FixmeReflection
class KMutableProperty0Impl<R>(name: String, getter: () -> R, val setter: (R) -> Unit)
    : KProperty0Impl<R>(name, getter), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        setter(value)
    }
}

@FixmeReflection
class KMutableProperty1Impl<T, R>(name: String, getter: (T) -> R, val setter: (T, R) -> Unit)
    : KProperty1Impl<T, R>(name, getter), KMutableProperty1<T, R> {
    override fun set(receiver: T, value: R): Unit {
        setter(receiver, value)
    }
}

@FixmeReflection
class KMutableProperty2Impl<T1, T2, R>(name: String, getter: (T1, T2) -> R, val setter: (T1, T2, R) -> Unit)
    : KProperty2Impl<T1, T2, R>(name, getter), KMutableProperty2<T1, T2, R> {
    override fun set(receiver1: T1, receiver2: T2, value: R): Unit {
        setter(receiver1, receiver2, value)
    }
}

open class KLocalDelegatedPropertyImpl<out R>(override val name: String): KProperty0<R> {
    override fun get(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }
    override fun invoke(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }
}

class KLocalDelegatedMutablePropertyImpl<R>(name: String): KLocalDelegatedPropertyImpl<R>(name), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }
}