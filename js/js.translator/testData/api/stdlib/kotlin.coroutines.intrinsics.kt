package kotlin.coroutines.intrinsics

@kotlin.SinceKotlin(version = "1.3") public val COROUTINE_SUSPENDED: kotlin.Any
    public fun <get-COROUTINE_SUSPENDED>(): kotlin.Any
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public suspend inline fun </*0*/ T> suspendCoroutineUninterceptedOrReturn(/*0*/ crossinline block: (kotlin.coroutines.Continuation<T>) -> kotlin.Any?): T
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> (suspend () -> T).createCoroutineUnintercepted(/*0*/ completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ R, /*1*/ T> (suspend R.() -> T).createCoroutineUnintercepted(/*0*/ receiver: R, /*1*/ completion: kotlin.coroutines.Continuation<T>): kotlin.coroutines.Continuation<kotlin.Unit>
@kotlin.SinceKotlin(version = "1.3") public fun </*0*/ T> kotlin.coroutines.Continuation<T>.intercepted(): kotlin.coroutines.Continuation<T>
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ T> (suspend () -> T).startCoroutineUninterceptedOrReturn(/*0*/ completion: kotlin.coroutines.Continuation<T>): kotlin.Any?
@kotlin.SinceKotlin(version = "1.3") @kotlin.internal.InlineOnly public inline fun </*0*/ R, /*1*/ T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(/*0*/ receiver: R, /*1*/ completion: kotlin.coroutines.Continuation<T>): kotlin.Any?