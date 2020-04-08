package my.sample

class A

fun A.check() {}

fun test() {
    (my.sample.A::check<caret>)(A())
}