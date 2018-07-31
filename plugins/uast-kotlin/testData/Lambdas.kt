import java.util.stream.Stream

fun foo() {
    Stream.empty<String>().filter { it.isEmpty() }
}

fun doSelectItem(selectItemFunction: () -> Unit) {
    selectItemFunction()
    val baz = fun() {
        Local()
    }
    baz()
}