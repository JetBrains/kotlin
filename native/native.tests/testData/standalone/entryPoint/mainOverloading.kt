// OUTPUT_DATA_FILE: mainOverloading.out

import kotlin.test.*

fun main() {
    fail()
}

fun <T> main(args: Array<String>) {
    fail()
}

fun main(args: Array<Int>) {
    fail()
}

fun main(args: Array<String>, second_arg: Int) {
    fail()
}

class Foo {
    fun main(args: Array<String>) {
        fail()
    }
}

fun main(args: Array<String>) {
    println("OK")
}