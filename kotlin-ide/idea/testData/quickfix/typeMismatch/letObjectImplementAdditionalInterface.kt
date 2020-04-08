// "Let the anonymous object implement interface 'A'" "true"
package let.implement

fun bar() {
    foo(<caret>object {})
}

fun foo(a: A) {
}

interface A