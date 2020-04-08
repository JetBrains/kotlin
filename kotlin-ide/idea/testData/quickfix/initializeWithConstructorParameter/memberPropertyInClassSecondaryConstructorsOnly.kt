// "Initialize with constructor parameter" "true"
open class A {
    <caret>val n: Int

    constructor(s: String)

    constructor(a: Int) {
        val t = 1
    }
}

class B : A("")

class C : A(1)

fun test() {
    val a = A("")
    val aa = A(1)
}