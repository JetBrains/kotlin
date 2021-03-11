// WITH_RUNTIME

class MyMap {
    val entries = listOf<Map.Entry<Int, Int>>()
}

fun foo(mm: MyMap) {
    for (entry<caret> in mm.entries) {
        val (key, value) = entry
    }
}