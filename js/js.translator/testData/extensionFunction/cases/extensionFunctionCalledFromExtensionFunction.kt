package foo

class A() {

}

fun A.one() = 1
fun A.two() = one() + one()

fun box(): Boolean {
    return (A().two() == 2)
}