// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

class Bam {
    inner class Foo {}
}

@Composable operator fun Bam.Foo.invoke() {}

@Composable fun test() {
    with(Bam()) {
        Foo()
    }
}
