import androidx.compose.runtime.Composable


import androidx.compose.runtime.Stable

interface Foo { val x: Int }
@Stable
interface StableFoo { val x: Int }

fun used(x: Any?) {}
