package test

import dependency.B

fun test(b: B) {
    b.<caret>f()
}

// REF: (in dependency.A).f()