import java.util.ArrayList

fun f() {
    val v : List<Int> = listOf()
    val copy1: List<Int> = ArrayList(<caret>v)
    val copy2 = ArrayList(v)
}
