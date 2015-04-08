class A(var a: Int) {
    init {
        $a = 3
    }
}

fun box() = (A(1).a == 3)
