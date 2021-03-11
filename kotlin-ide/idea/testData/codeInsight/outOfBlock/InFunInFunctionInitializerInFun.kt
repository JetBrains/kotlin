// OUT_OF_CODE_BLOCK: TRUE
// ERROR: Unresolved reference: a
val b = true

fun test() = if (b) {
    fun hello() {
        <caret>
    }
}
else {

}