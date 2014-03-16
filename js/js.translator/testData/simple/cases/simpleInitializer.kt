package foo

class Test() {
    var a: Int
    {
        $a = 3
    }
}

fun box(): Boolean {
    return (Test().a == 3);
}
