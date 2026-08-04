// DUMP_IR

// Regression test for KT-88069
// `invoke` on a memoized @Composable lambda literal used to be lowered to a `ComposableFunctionN.invoke` instead of `FunctionN.invoke`

// FILE: main.kt
package home

import androidx.compose.runtime.Composable

// capturing: memoized with `rememberComposableLambda`
@Composable
fun App(state: Int) {
    (@Composable {
        used(state)
    }).invoke()
}

// non-capturing, but in a public inline scope: not moved into `ComposableSingletons`,
// so it is memoized with `composableLambdaInstance` rather than read from the singleton
@Composable
inline fun PublicInline() {
    (@Composable {
        used(0)
    }).invoke()
}

fun used(value: Any?) {}
