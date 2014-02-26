package foo

fun box(): Boolean {
    var a = 4
    when(a) {
        3 -> {
            a = 10;
        }
        4 -> {
            a = 20;
        }
        else -> {
            a = 30;
        }
    }
    return (a == 20)
}