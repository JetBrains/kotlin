// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable
fun Leaf() {}

@Composable
fun foo() {
    val myList = listOf(1,2,3,4,5)
    myList.forEach @Composable <!ARGUMENT_TYPE_MISMATCH!>{ value: Int ->
        Leaf()
        System.out.println(value)
    }<!>
}
