// DUMP_IR

// FILE: main.kt

import org.jetbrains.kotlin.fir.plugin.MyComposable

interface IFoo {
    var callFoo: @MyComposable (task: String) -> Unit
}

data class Foo(override var callFoo: @MyComposable (task: String) -> Unit): IFoo

@MyComposable
fun runFoo(f: IFoo) {
    f.callFoo(task = "Task1")
}

fun box(): String = "OK"