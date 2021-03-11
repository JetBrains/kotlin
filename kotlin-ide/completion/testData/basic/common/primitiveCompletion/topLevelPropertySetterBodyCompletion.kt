// FIR_COMPARISON

fun localFun() {}

var property: Int
    get() = 1
    set(value) {
        <caret>
    }

// EXIST: localFun