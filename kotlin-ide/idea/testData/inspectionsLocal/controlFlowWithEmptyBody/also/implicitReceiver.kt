// PROBLEM: 'also' has empty body
// FIX: none
// WITH_RUNTIME

fun String.test() {
    <caret>also {  }
}