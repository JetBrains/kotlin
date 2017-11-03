val foo = "lorem"
val bar = "ipsum"
val baz = "dolor"

val foobarbaz = "$foo $bar $baz"

val case4 = "a ${"literal"} z"

fun simpleForTemplate(i: Int = 0) = "$i"

fun foo() {
    println("$baz")
    val template1 = "${simpleForTemplate()}"
    val template2 = ".${simpleForTemplate()}"
}
