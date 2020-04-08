// "Create member property 'A.foo'" "true"
// ERROR: Property must be initialized or be abstract

class A

fun test() {
    println("a = ${A().<caret>foo}")
}