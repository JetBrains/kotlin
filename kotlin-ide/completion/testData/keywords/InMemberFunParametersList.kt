package TestData

class TestSample() {
    fun test(<caret>) {
    }
}

// EXIST: vararg
// EXIST: noinline
// EXIST: crossinline
/* TODO: val&var are not correct */
// EXIST: val
// EXIST: var
// NOTHING_ELSE
