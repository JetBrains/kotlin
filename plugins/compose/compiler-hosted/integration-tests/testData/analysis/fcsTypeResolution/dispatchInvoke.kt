// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class Bam {
    @Composable fun Foo() {}
}

@Composable fun test() {
    with(Bam()) {
        Foo()
    }
}
