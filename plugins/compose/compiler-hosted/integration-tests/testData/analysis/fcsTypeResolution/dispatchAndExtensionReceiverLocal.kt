// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class Foo {}

class Bam {
    @Composable operator fun Foo.invoke() {}
    @Composable operator fun invoke() {
        Foo()
    }
}
