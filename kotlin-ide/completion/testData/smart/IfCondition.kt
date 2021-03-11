fun bar(b: Boolean, c: Char){
    if (<caret>
}

// EXIST: b
// ABSENT: c
// ABSENT: true
// ABSENT: false
