// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable
fun ListTest(list: List<Any>) {
    fun <!COMPOSABLE_EXPECTED!>test<!>() {
        list.flatMap { <!COMPOSABLE_INVOCATION!>Test<!>(it) }
    }
    test()
}

@Composable
fun Test(any: Any): List<Any> = TODO(any.toString())
