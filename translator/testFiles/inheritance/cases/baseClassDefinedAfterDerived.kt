package foo


class A() : B() {

}

open class B() {

    val a = 3
}

fun box() = (A().a == 3)