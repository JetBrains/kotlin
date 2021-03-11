fun foo(p: Int) {
    "abc".fold(1) { acc, c -> <caret> }
}

// EXIST: p
// ABSENT: "acc: Int, c: Char ->"
// ABSENT: "acc, c ->"
