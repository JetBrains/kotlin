import kotlin.explain.*

@ExplainIgnore
class AssertBuilder(val actual: Any?, val actualDiagram: CallExplanation)

@ExplainCall
fun assertThat(actual: Any?): AssertBuilder {
    return AssertBuilder(actual, ExplainCall.explanation ?: error("no power-assert: assertThat"))
}

@ExplainCall
fun AssertBuilder.isEqualTo(expected: Any?) {
    if (actual != expected) {
        val expectedDiagram = ExplainCall.explanation ?: error("no power-assert: isEqualTo")
        throw AssertionError(buildString {
            appendLine()
            appendLine("Expected:")
            append(expectedDiagram.toDefaultMessage())
            appendLine()
            appendLine("Actual:")
            append(actualDiagram.toDefaultMessage())
        })
    }
}

fun box(): String {
    return test1()
}

fun test1() = expectThrowableMessage {
    @Explain val hello = "Hello"
    @Explain val world = "World".substring(1, 4)

    @Explain
    val expected =
        hello.length
    @Explain val actual = world.length

    assertThat(actual).isEqualTo(expected)
}
