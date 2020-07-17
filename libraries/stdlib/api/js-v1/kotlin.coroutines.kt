@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public val coroutineContext: kotlin.coroutines.CoroutineContext { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> Continuation(context: kotlin.coroutines.CoroutineContext, crossinline resumeWith: (kotlin.Result<T>) -> kotlin.Unit): kotlin.coroutines.Continuation<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public suspend inline fun <T> suspendCoroutine(crossinline block: (kotlin.coroutines.Continuation<T>) -> kotlin.Unit): T

@kotlin.SinceKotlin(version = "1.3")
public fun <T> (suspend () -> T).createCoroutine(completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>

@kotlin.SinceKotlin(version = "1.3")
public fun <R, T> (suspend R.() -> T).createCoroutine(receiver: R, completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public fun <E : kotlin.coroutines.CoroutineContext.Element> kotlin.coroutines.CoroutineContext.Element.getPolymorphicElement(key: kotlin.coroutines.CoroutineContext.Key<E>): E?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public fun kotlin.coroutines.CoroutineContext.Element.minusPolymorphicKey(key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.coroutines.Continuation<T>.resume(value: T): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> kotlin.coroutines.Continuation<T>.resumeWithException(exception: kotlin.Throwable): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun <T> (suspend () -> T).startCoroutine(completion: kotlin.coroutines.Continuation<T>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public fun <R, T> (suspend R.() -> T).startCoroutine(receiver: R, completion: kotlin.coroutines.Continuation<T>): kotlin.Unit

@kotlin.SinceKotlin(version = "1.3")
public abstract class AbstractCoroutineContextElement : kotlin.coroutines.CoroutineContext.Element {
    public constructor AbstractCoroutineContextElement(key: kotlin.coroutines.CoroutineContext.Key<*>)

    public open override val key: kotlin.coroutines.CoroutineContext.Key<*> { get; }
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.ExperimentalStdlibApi
public abstract class AbstractCoroutineContextKey<B : kotlin.coroutines.CoroutineContext.Element, E : B> : kotlin.coroutines.CoroutineContext.Key<E> {
    public constructor AbstractCoroutineContextKey<B : kotlin.coroutines.CoroutineContext.Element, E : B>(baseKey: kotlin.coroutines.CoroutineContext.Key<B>, safeCast: (element: kotlin.coroutines.CoroutineContext.Element) -> E?)
}

@kotlin.SinceKotlin(version = "1.3")
public interface Continuation<in T> {
    public abstract val context: kotlin.coroutines.CoroutineContext { get; }

    public abstract fun resumeWith(result: kotlin.Result<T>): kotlin.Unit
}

@kotlin.SinceKotlin(version = "1.3")
public interface ContinuationInterceptor : kotlin.coroutines.CoroutineContext.Element {
    public open override operator fun <E : kotlin.coroutines.CoroutineContext.Element> get(key: kotlin.coroutines.CoroutineContext.Key<E>): E?

    public abstract fun <T> interceptContinuation(continuation: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<T>

    public open override fun minusKey(key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext

    public open fun releaseInterceptedContinuation(continuation: kotlin.coroutines.Continuation<*>): kotlin.Unit

    public companion object of ContinuationInterceptor Key : kotlin.coroutines.CoroutineContext.Key<kotlin.coroutines.ContinuationInterceptor> {
    }
}

@kotlin.SinceKotlin(version = "1.3")
public interface CoroutineContext {
    public abstract fun <R> fold(initial: R, operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R

    public abstract operator fun <E : kotlin.coroutines.CoroutineContext.Element> get(key: kotlin.coroutines.CoroutineContext.Key<E>): E?

    public abstract fun minusKey(key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext

    public open operator fun plus(context: kotlin.coroutines.CoroutineContext): kotlin.coroutines.CoroutineContext

    public interface Element : kotlin.coroutines.CoroutineContext {
        public abstract val key: kotlin.coroutines.CoroutineContext.Key<*> { get; }

        public open override fun <R> fold(initial: R, operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R

        public open override operator fun <E : kotlin.coroutines.CoroutineContext.Element> get(key: kotlin.coroutines.CoroutineContext.Key<E>): E?

        public open override fun minusKey(key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext
    }

    public interface Key<E : kotlin.coroutines.CoroutineContext.Element> {
    }
}

@kotlin.SinceKotlin(version = "1.3")
public object EmptyCoroutineContext : kotlin.coroutines.CoroutineContext, kotlin.io.Serializable {
    public open override fun <R> fold(initial: R, operation: (R, kotlin.coroutines.CoroutineContext.Element) -> R): R

    public open override operator fun <E : kotlin.coroutines.CoroutineContext.Element> get(key: kotlin.coroutines.CoroutineContext.Key<E>): E?

    public open override fun hashCode(): kotlin.Int

    public open override fun minusKey(key: kotlin.coroutines.CoroutineContext.Key<*>): kotlin.coroutines.CoroutineContext

    public open override operator fun plus(context: kotlin.coroutines.CoroutineContext): kotlin.coroutines.CoroutineContext

    public open override fun toString(): kotlin.String
}

@kotlin.SinceKotlin(version = "1.3")
@kotlin.annotation.Target(allowedTargets = {AnnotationTarget.CLASS})
@kotlin.annotation.Retention(value = AnnotationRetention.BINARY)
public final annotation class RestrictsSuspension : kotlin.Annotation {
    public constructor RestrictsSuspension()
}