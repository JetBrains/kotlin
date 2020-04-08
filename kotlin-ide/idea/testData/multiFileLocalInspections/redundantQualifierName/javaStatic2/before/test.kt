package my.simple.name

import my.Reproducer
import my.Reproducer.test

fun test() = Reproducer()

fun main() {
    Reproducer<caret>.test().number()
}