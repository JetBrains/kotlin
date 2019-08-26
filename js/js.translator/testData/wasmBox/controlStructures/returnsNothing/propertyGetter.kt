var flag = true

val magic: Nothing get() { while (true); }

fun box(): String {
    val a: String
    if (flag) {
        a = "OK"
    }
    else {
        magic
    }
    return a
}