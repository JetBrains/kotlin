import androidx.compose.runtime.Composable
 
fun higherOrderFunction(child: Any.() -> Unit) {

}


@Composable 
fun Foo() {
    higherOrderFunction {
        print("Foo")
    }
}

fun used(x: Any?) {}
