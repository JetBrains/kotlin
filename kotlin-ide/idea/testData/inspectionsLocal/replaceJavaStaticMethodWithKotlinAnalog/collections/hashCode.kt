// WITH_RUNTIME
import java.util.Arrays

fun test() {
    val a = arrayOf(1)
    val hash = Arrays.<caret>hashCode(a)
}