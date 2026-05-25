import androidx.compose.runtime.Composable  
@Composable 
fun Foo(child: @Composable () -> Unit) { child() }

@Composable 
fun Bar() {
    Foo { 
        print("A")
    }

    Foo {
        print("B")
    }
}

fun used(x: Any?) {}
