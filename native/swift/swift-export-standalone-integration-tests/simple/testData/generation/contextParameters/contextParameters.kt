// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

class Context
class ContextA
class ContextB

context(ctx: Context) fun foo(): Unit = TODO()
context(ctx: Context) fun bar(arg: Any): Any = TODO()
context(ctx: Context) val baz: Any get() = TODO()

context(context: Context, contextA: ContextA, contextB: ContextB)
fun String.complexContextFunction(yes: Boolean): Int = TODO()

context(contextA: ContextA, contextB: ContextB)
var String.complexContextProperty: Boolean
    get() = TODO()
    set(value) = TODO()

context(ctx: Context, _: ContextB)
fun unnamedContextParametersFunction(): Unit = TODO()

context(_: ContextA, ctx: Context)
var unnamedContextParametersProperty: Int
    get() = TODO()
    set(value) = TODO()

object Foo {
    context(ctx: Context) fun foo(): Unit = TODO()
    context(ctx: Context) fun bar(arg: Any): Any = TODO()
    context(ctx: Context) val baz: Any get() = TODO()

    context(contextA: ContextA, context: Context, contextB: ContextB)
    fun String.complexContextFunction(count: Int): Boolean = TODO()

    context(contextB: ContextB, contextA: ContextA)
    var String.complexContextProperty: Int
        get() = TODO()
        set(value) = TODO()

    context(_: Context, ctx: ContextB)
    fun unnamedContextParametersFunction(): Unit = TODO()

    context(ctx: ContextA, _: Context)
    var unnamedContextParametersProperty: String
        get() = TODO()
        set(value) = TODO()
}

fun contextBlockA(block: context(ContextA, ContextB) Int.(String) -> Unit): Unit = TODO()

fun contextBlockB(): context(ContextB, ContextA) String.(Int) -> Unit = TODO()

fun contextBlockC(block: context(Context) (String) -> Unit): Unit = TODO()

fun contextBlockD(): context(Context) (Int) -> Unit = TODO()
