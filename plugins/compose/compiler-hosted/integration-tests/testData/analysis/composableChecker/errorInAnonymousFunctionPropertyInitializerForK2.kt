// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable
@Composable fun ComposableFunction() {}
fun getMyClass(): Any {
    class MyClass {
        val property = <!COMPOSABLE_EXPECTED!>fun()<!> {
            <!COMPOSABLE_INVOCATION!>ComposableFunction<!>()  // invocation
        }
    }
    return MyClass()
}
