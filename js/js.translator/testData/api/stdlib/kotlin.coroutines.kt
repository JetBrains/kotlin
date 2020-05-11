package kotlin.coroutines

@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public val coroutineContext: kotlin.coroutines.CoroutineContext
    public inline fun <get-coroutineContext>(): kotlin.coroutines.CoroutineContext
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> Continuation(/*0*/ context: kotlin.coroutines.CoroutineContext, /*1*/ crossinline resumeWith: (kotlin.Result<T>) -> kotlin.Unit): kotlin.coroutines.Continuation<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public suspend inline fun </*0*/ T> suspendCoroutine(/*0*/ crossinline block: (kotlin.coroutines.Continuation<T>) -> kotlin.Unit): T
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> (suspend () -> T).createCoroutine(/*0*/ completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ R, /*1*/ T> (suspend R.() -> T).createCoroutine(/*0*/ receiver: R, /*1*/ completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun </*0*/ E : kotlin.coroutines.CoroutineContext.Element> kotlin.coroutines.CoroutineContext.Element.getPolymorphicElement(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<E>): E?
@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public fun kotlin.coroutines.CoroutineContext.Element.minusPolymorphicKey(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.coroutines.Continuation<T>.resume(/*0*/ value: T): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> kotlin.coroutines.Continuation<T>.resumeWithException(/*0*/ exception: kotlin.Throwable): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> (suspend () -> T).startCoroutine(/*0*/ completion: kotlin.coroutines.Continuation<T>): kotlin.Unit
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ R, /*1*/ T> (suspend R.() -> T).startCoroutine(/*0*/ receiver: R, /*1*/ completion: kotlin.coroutines.Continuation<T>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3") public abstract class AbstractCoroutineContextElement : kotlin.coroutines.CoroutineContext.Element {
    /*primary*/ public constructor AbstractCoroutineContextElement(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>)
    public open override /*1*/ val key: kotlin.coroutines.CoroutineContext.Key<*>
        public open override /*1*/ fun <get-key>(): kotlin.coroutines.CoroutineContext.Key<*>
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.ExperimentalStdlibApi public abstract class AbstractCoroutineContextKey</*0*/ B : kotlin.coroutines.CoroutineContext.Element, /*1*/ E : B> : kotlin.coroutines.CoroutineContext.Key<E> {
    /*primary*/ public constructor AbstractCoroutineContextKey</*0*/ B : kotlin.coroutines.CoroutineContext.Element, /*1*/ E : B>(/*0*/ baseKey: kotlin.coroutines.CoroutineContext.Key<B>, /*1*/ safeCast: (element: kotlin.coroutines.CoroutineContext.Element) -> E?)
}

@kotlin.SinceKotlin(version = "1.3") public interface Continuation</*0*/ in T> {
    public abstract val context: kotlin.coroutines.CoroutineContext
        public abstract fun <get-context>(): kotlin.coroutines.CoroutineContext
    public abstract fun resumeWith(/*0*/ result: kotlin.Result<T>): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.3") public interface ContinuationInterceptor : kotlin.coroutines.CoroutineContext.Element {
    public open override /*1*/ fun </*0*/ E : kotlin.coroutines.CoroutineContext.Element> get(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<E>): E?
    public abstract fun </*0*/ T> interceptContinuation(/*0*/ continuation: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<T>
    public open override /*1*/ fun minusKey(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
    public open fun releaseInterceptedContinuation(/*0*/ continuation: kotlin.coroutines.Continuation<*>): kotlin.Unit

    public companion object Key : kotlin.coroutines.CoroutineContext.Key<kotlin.coroutines.ContinuationInterceptor> {
    }
}

@kotlin.SinceKotlin(version = "1.3") public interface CoroutineContext {
    public abstract fun </*0*/ R> fold(/*0*/ initial: R, /*1*/ operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R
    public abstract operator fun </*0*/ E : kotlin.coroutines.CoroutineContext.Element> get(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<E>): E?
    public abstract fun minusKey(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
    public open operator fun plus(/*0*/ context: kotlin.coroutines.CoroutineContext): kotlin.coroutines.CoroutineContext

    public interface Element : kotlin.coroutines.CoroutineContext {
        public abstract val key: kotlin.coroutines.CoroutineContext.Key<*>
            public abstract fun <get-key>(): kotlin.coroutines.CoroutineContext.Key<*>
        public open override /*1*/ fun </*0*/ R> fold(/*0*/ initial: R, /*1*/ operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R
        public open override /*1*/ fun </*0*/ E : kotlin.coroutines.CoroutineContext.Element> get(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<E>): E?
        public open override /*1*/ fun minusKey(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
    }

    public interface Key</*0*/ E : kotlin.coroutines.CoroutineContext.Element> {
    }
}

@kotlin.SinceKotlin(version = "1.3") public object EmptyCoroutineContext : kotlin.coroutines.CoroutineContext, kotlin.io.Serializable {
    public open override /*1*/ fun </*0*/ R> fold(/*0*/ initial: R, /*1*/ operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R
    public open override /*1*/ fun </*0*/ E : kotlin.coroutines.CoroutineContext.Element> get(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<E>): E?
    public open override /*2*/ fun hashCode(): kotlin.Int
    public open override /*1*/ fun minusKey(/*0*/ key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
    public open override /*1*/ fun plus(/*0*/ context: kotlin.coroutines.CoroutineContext): kotlin.coroutines.CoroutineContext
    public open override /*2*/ fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3") @kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS}) @kotlin.annotation.Retention(value = AnnotationRetention.BINARY) public final annotation class RestrictsSuspension : kotlin.Annotation {
    /*primary*/ public constructor RestrictsSuspension()
}