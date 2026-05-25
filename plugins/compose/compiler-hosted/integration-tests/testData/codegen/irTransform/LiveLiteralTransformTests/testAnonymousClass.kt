import androidx.compose.runtime.Composable

interface Foo { fun bar(): Int }
fun a(): Foo {
    return object : Foo {
        override fun bar(): Int { return 1 }
    }
}
