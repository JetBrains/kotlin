// MODULE: main
// MODULE_KIND: LibraryBinary
// FILE: Foo.kt
package compose.ui

import androidx.compose.runtime.Composable

@Composable
fun Foo(child: @Composable () -> Unit) {
    FooHelp {
        child()
    }
}

@Composable
inline fun FooHelp(child: @Composable () -> Unit) {
    /**
     * Previous versions of the compose compiler 'leaked' this lambda through ComposableSingletons.
     * We're now generating backwards compatible declarations.
     */
    val localFooHelp: @Composable () -> Unit = {}
    child()
}

// FILE: Bar.kt
package compose.ui

import androidx.compose.runtime.Composable

@Composable
inline fun Bar() {
    /**
     * Previous versions of the compose compiler 'leaked' this lambda through ComposableSingletons.
     * We're now generating backwards compatible declarations.
     */
    val localBar: @Composable () -> Unit = {}
    Foo {
        print("Bar")
    }
}