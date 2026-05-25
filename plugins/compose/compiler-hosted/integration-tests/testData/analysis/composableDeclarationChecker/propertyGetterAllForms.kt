// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

val bar2: Int @Composable get() = 123
@get:Composable val bar3: Int get() = 123

interface Foo {
    val bar2: Int @Composable get() = 123
    @get:Composable val bar3: Int get() = 123
}
