@kotlin.SinceKotlin(version = "1.3")
public val COROUTINE_SUSPENDED: kotlin.Any { get; }

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (kotlin.coroutines.Continuation<T>) -> kotlin.Any?): T

@kotlin.SinceKotlin(version = "1.3")
public fun <T> (suspend () -> T).createCoroutineUnintercepted(completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>

@kotlin.SinceKotlin(version = "1.3")
public fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(receiver: R, completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>

@kotlin.SinceKotlin(version = "1.3")
public fun <T> kotlin.coroutines.Continuation<T>.intercepted(): kotlin.coroutines.Continuation<T>

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(completion: kotlin.coroutines.Continuation<T>): kotlin.Any?

@kotlin.SinceKotlin(version = "1.3")
@kotlin.internal.InlineOnly
public inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(receiver: R, completion: kotlin.coroutines.Continuation<T>): kotlin.Any?