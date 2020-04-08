// NAME_COUNT_TO_USE_STAR_IMPORT: 10
package bug.b

import bug.a.*

fun A.foo() = println("extension function")

fun main(args: Array<String>) {
    val func = MyFunction()
    func()

    val a = A(func)

    a.foo()
}