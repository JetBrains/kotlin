package test

fun bar(i: Int): String {
    return "$i   ${quux()}   $i"
}

fun quux(): String {
    return "quux"
}