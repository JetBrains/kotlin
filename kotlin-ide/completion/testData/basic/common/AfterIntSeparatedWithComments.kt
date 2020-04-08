infix fun Int.func(s: String): Int{}

fun test() {
    val floor = "Floor"
    val a = 1/**/f<caret>
}

// EXIST: func
