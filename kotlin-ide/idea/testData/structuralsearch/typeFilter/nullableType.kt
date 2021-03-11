var nullable: Int? = null
var notNullable: Int = 0

fun f(s: Int?): Int? { return s }

fun main() {
    <warning descr="SSR">f(nullable)</warning>
    f(notNullable)
}