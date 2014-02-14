package foo

fun box(): String {
    val number = 3
    val s1 = "${number - 1}${number}"
    val s2 = "${5}${4}"
    return "${s1}${s2}"
}

