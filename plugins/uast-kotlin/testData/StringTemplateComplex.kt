val muchRecur = "${"${"${"abc"}"}"}"

val case4 = "a ${"literal"} z"

val case5 = "a ${"literal"} ${"literal"} z"

val literalInLiteral = "a ${"literal$case4"} z"

val literalInLiteral2 = "a ${"literal$case4".repeat(4)} z"

fun simpleForTemplate(i: Int = 0) = "$i"

fun foo() {
    println("$baz")
    val template1 = "${simpleForTemplate()}"
    val template2 = ".${simpleForTemplate()}"
}
