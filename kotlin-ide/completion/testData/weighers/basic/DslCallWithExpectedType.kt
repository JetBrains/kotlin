// RUNTIME
package test

import bar.r
import bar.foo3

fun main() {
    val foo5 = 3
    r {
        val j: String = foo<caret>
    }
}

@DslMarker
annotation class Dsl


@Dsl
class R

fun r(body: R.() -> Unit) {

}

fun foo1(i: Int): String ""

fun foo3() {

}

@Dsl
fun R.foobar2(): String = ""

@Dsl
fun R.foobar4() {

}



// ORDER: foobar2
// ORDER: foo1
// ORDER: foo5
// ORDER: foobar4
// ORDER: foo3
