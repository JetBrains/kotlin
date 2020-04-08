// OUT_OF_CODE_BLOCK: FALSE

abstract class S(val f: () -> Unit)

fun foo(s: String, f: () -> Unit) {}

class ST : S(
        {
            foo("a <caret>test calculator") {

            }
        })