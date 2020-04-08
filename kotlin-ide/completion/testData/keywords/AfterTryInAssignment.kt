fun foo() {
    val v = try {
        bar()
    }
    <caret>
}

// EXIST: catch
// EXIST: finally
// EXIST: false
// EXIST: null
// EXIST: true
// NOTHING_ELSE
