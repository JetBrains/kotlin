var nullable: Int? = null
var notNullable: Int = 0

fun f(s: Int?) { <warning descr="SSR">print(s)</warning> }

fun main() {
    <warning descr="SSR">f(nullable)</warning>
    f(notNullable)
}