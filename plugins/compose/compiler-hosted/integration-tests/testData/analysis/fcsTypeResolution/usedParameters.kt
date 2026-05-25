// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun Foo(x: Int, composeItem: @Composable () -> Unit = {}) {
    println(x)
    print(composeItem == {})
}

@Composable fun test(
    content: @Composable () -> Unit,
    value: Int,
    x: Int,
    content2: @Composable () -> Unit,
    value2: Int
) {
    Foo(123) {
        // named argument
        Foo(x=value)

        // indexed argument
        Foo(x)

        // tag
        content()
    }
    Foo(x=123, composeItem={
        val abc = 123

        // attribute value
        Foo(x=abc)

        // attribute value
        Foo(x=value2)

        // tag
        content2()
    })
}
