fun foo(p: Boolean) {
    "abc".filter { <caret> }
}

// EXIST: p
// EXIST: "c ->"
// EXIST: "c: Char ->"
