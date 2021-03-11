import java.util.*

fun foo(list: ArrayList<String>, p1: Any, p2: String) {
    list.remove(<caret>)
}

// ABSENT: p1
// EXIST: p2
// EXIST: { itemText: "String", tailText: "() (kotlin)" }
