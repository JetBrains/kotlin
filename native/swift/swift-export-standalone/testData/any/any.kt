// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

object MyObject

fun getMainObject(): Any = MyObject
fun isMainObject(obj: Any): Boolean = obj == MyObject