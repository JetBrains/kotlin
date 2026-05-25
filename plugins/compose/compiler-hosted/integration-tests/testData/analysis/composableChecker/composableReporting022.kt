// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

fun <!COMPOSABLE_EXPECTED!>foo<!>() {
    val myList = listOf(1,2,3,4,5)
    myList.forEach { value: Int ->
        <!COMPOSABLE_INVOCATION!>Leaf<!>()
        println(value)
    }
}
