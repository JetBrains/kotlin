// "Create parameter 'x'" "false"
// ERROR: Unresolved reference: x
// ACTION: Create property 'x'
// ACTION: Rename reference
// ACTION: Add 'n =' to argument

enum class E(n: Int) {
    X(<caret>x)
}