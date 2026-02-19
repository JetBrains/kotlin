package foo

interface A : B, E
interface B
open class C {
    fun foo() = true
}
interface D
interface E
interface F : G, D
interface G

fun box() = if (C().foo()) "OK" else "fail"