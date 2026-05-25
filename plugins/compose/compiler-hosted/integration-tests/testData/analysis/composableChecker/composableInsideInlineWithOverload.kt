// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable
fun ListTest(list: List<Any>) {
    list.flatMap { Test(it) }
}

@Composable
fun Test(any: Any): List<Any> = TODO(any.toString())
