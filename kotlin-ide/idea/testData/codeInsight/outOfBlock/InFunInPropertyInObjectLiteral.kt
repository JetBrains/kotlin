// OUT_OF_CODE_BLOCK: FALSE
interface Some

fun test() {
    val foo = object: Some {
        <caret>
    }
}