// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class Foo {}
@Composable operator fun Foo.invoke() {}

@Composable fun test() {
    Foo()
}
