open class Subscription

interface Observer<T> {
    fun next(value: T)
}

open class Subscriber<T>(observer: Observer<T>? = null) : Subscription(), Observer<T> {
    override fun next(value: T) {}
}

fun interface Subscribe<T> {
    fun subscribe(subscriber: Subscriber<T>): Subscription
}

fun interface Operator<X, Y> {
    fun call(source: Observable<X>): Observable<Y>
}

open class Observable<T>(private val subscribeFn: Subscribe<T>) {
    fun <R> apply(operator: Operator<T, R>): Observable<R> {
        return operator.call(this)
    }

    fun subscribe(observer: Observer<T>): Subscription {
        return Subscription()
    }

    companion object {
        fun <T> of(vararg elems: T): Observable<T> {
            return Observable { Subscription() }
        }
    }
}
