// KIND: STANDALONE
// MODULE: UnboundGenerics
// FILE: Describe.kt
// Shared Kotlin-side probes: tell Swift what Kotlin actually received through Any / T.

class Foo(val id: Int)

fun describeAny(x: Any?): String = when (x) {
    null -> "null"
    is String -> "String:$x"
    is Byte -> "Byte:$x"
    is Short -> "Short:$x"
    is Int -> "Int:$x"
    is Long -> "Long:$x"
    is Float -> "Float:$x"
    is Double -> "Double:$x"
    is Boolean -> "Boolean:$x"
    is Foo -> "Foo:${x.id}"
    is List<*> -> "List[" + x.joinToString(",") { describeAny(it) } + "]"
    else -> "Other"
}

// FILE: Closures.kt
// Closures carrying T in every position (parameter, result, property), both directions.

fun <T> produce(f: () -> T): String = describeAny(f())
fun <T> mapValue(x: T, f: (T) -> T): T = f(x)
fun <T> constant(x: T): () -> T = { x }
fun <T> describer(): (T) -> String = { describeAny(it) }

class Callback<T> {
    var listener: ((T) -> T)? = null
    fun fire(x: T): String = describeAny(listener?.invoke(x))
}

fun <T> lambdaAsT(): T = ({ "lambda" } as () -> String) as T
fun invokeAny(f: Any): String = if (f is Function0<*>) describeAny(f()) else "not a Function0:" + describeAny(f)

// FILE: ExtensionReceiverT.kt
// Non-null extension receiver of type T: exported on `_KotlinBridgeable`, callable as a member on concrete types.

fun <T : Any> T.nonNullDescribe(): String = describeAny(this)

// FILE: DeclarationShapes.kt
// Other declaration shapes that carry T.

class Box<T>(var value: T?)

fun stringBox(): Box<String> = Box("boxed")
fun boxNothing(): Box<Nothing> = Box(null)
fun boxDescribe(b: Box<*>): String = describeAny(b.value)

fun <T> describeVararg(vararg xs: T): String = xs.joinToString(",") { describeAny(it) }
