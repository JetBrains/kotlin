var flag = true

fun exit(): Nothing {
    while (true) {}
}

fun box(): String {
    val a: String
    if (flag) {
        a = "OK"
    }
    else {
        exit()
    }
    return a
}