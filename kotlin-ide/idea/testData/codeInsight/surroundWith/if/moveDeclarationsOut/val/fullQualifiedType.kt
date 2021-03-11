fun foo() {
    <caret>val a: kotlin.test.Asserter? = null

    a?.charAt(1)
}