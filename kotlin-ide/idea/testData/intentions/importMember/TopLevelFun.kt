// INTENTION_TEXT: "Add import for 'kotlin.test.assertFailsWith'"
// RUNTIME_WITH_KOTLIN_TEST

fun foo() {
    kotlin.test.<caret>assertFailsWith<Exception>("", {})
    kotlin.test.assertFailsWith(RuntimeException::class, "", {})
}
