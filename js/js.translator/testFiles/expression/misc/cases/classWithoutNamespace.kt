class A(var a: Int) {
    {
        $a = 3
    }
}

fun box() = (A(1).a == 3)
