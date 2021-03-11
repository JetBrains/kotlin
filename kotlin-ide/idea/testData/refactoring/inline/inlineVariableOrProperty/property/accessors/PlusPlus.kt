// ERROR: Unsupported usage: property++

var <caret>property: Int
    get() = 1
    set(value) {
    }

fun foo() {
    property++
}