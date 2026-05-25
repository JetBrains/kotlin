// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

val prop: Int get() = 1

val globalProp: Int
    @Composable get() = 1

class A {
    val bar: String @Composable get() = TODO()
}

@Composable fun Test(a: A) {
    println(<!COMPOSABLE_PROPERTY_REFERENCE!>a::bar<!>) // use it to avoid UNUSED warning
    println(<!COMPOSABLE_PROPERTY_REFERENCE!>::globalProp<!>) // use it to avoid UNUSED warning
    println(::prop) // use it to avoid UNUSED warning
}
