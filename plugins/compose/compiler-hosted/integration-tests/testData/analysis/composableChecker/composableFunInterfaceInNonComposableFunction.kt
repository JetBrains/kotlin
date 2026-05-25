// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface FunInterfaceWithComposable {
    @Composable fun content()
}

fun Test1() {
    val funInterfaceWithComposable = FunInterfaceWithComposable {
        TODO()
    }
    println(funInterfaceWithComposable) // use it to avoid UNUSED warning
}

fun <!COMPOSABLE_EXPECTED!>Test2<!>() {
    val funInterfaceWithComposable = FunInterfaceWithComposable {
        TODO()
    }
    funInterfaceWithComposable.<!COMPOSABLE_INVOCATION!>content<!>()
}
