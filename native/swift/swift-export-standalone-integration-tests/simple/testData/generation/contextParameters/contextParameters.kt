// KIND: STANDALONE
// FREE_COMPILER_ARGS: -Xcontext-parameters
// MODULE: main
// FILE: main.kt

class Context

context(ctx: Context) fun foo(): Unit = TODO()
context(ctx: Context) fun bar(arg: Any): Any = TODO()
context(ctx: Context) val baz: Any get() = TODO()


object Foo {
    context(ctx: Context) fun foo(): Unit = TODO()
    context(ctx: Context) fun bar(arg: Any): Any = TODO()
    context(ctx: Context) val baz: Any get() = TODO()
}