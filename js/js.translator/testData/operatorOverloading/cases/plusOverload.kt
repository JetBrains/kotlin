package foo

class myInt(a: Int) {
    val value = a;

    fun plus(other: myInt): myInt = myInt(value + other.value)
}

fun box(): Boolean {

    return (myInt(3) + myInt(5)).value == 8
}