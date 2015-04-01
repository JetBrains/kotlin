package foo

class Test() {
    var a: Int
    init {
        $a = 3
    }
}

fun box(): Boolean {
    return (Test().a == 3);
}
