// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

interface A

interface B {
    val property: A @Composable get() = TODO()
}

@Composable fun Test(b: B) {
    val a = object : A by b.property {}
    println(a)
}
