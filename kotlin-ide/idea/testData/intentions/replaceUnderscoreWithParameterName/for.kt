data class Data(val first: Int, val second: Int)

fun foo(list: List<Data>) {
    for ((<caret>_, _) in list) {

    }
}