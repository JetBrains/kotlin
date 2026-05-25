// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable


@Composable
fun Parent() {
    with(Foo()) {
        Test()
        Test(a = "a")
        Test(b = 101)
        Test(a = "Yes", b = 10)
    }
}

context(foo: Foo)
@Composable
fun Test(a: String = "A", b: Int = 2) {
    val combineParams = a + b
    if (foo.someString == combineParams) {
        println("Same same")
    }
}
