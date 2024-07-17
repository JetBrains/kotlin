import kotlin.js.Promise
import kotlin.coroutines.*
import Foo
import FooImpl

suspend fun test(klass: Foo, stepId: Int): String {
    val result = klass.foo()
    val exportedResult = klass.asDynamic().foo().unsafeCast<Promise<Int>>().await()

    if (result != stepId) return "The original result ($result) doesn't equal to stepId ($stepId)"
    if (exportedResult != stepId) return "The exported result ($exportedResult) doesn't equal to stepId ($stepId)"

    return "OK"
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { continuation ->
    this.then(
        { result -> continuation.resume(result) },
        { error -> continuation.resumeWithException(error) }
    )
}

suspend fun box(stepId: Int) = test(FooImpl(), stepId)
