// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

class Foo

@Composable fun main(foo: Foo) {
    print(foo)
}
