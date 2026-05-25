// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*;

@Composable fun Foo() {}

val bam: Int @Composable get() {
    Foo()
    return 123
}
