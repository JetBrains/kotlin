fun String.method1(fn: String.() -> Unit = {}) {}

fun String.method2(param1: Int) {}

fun String.method3() {
    method1 {
        method2(<caret>)
    }
}

/* Text: (<highlight>param1: Int</highlight>), Disabled: false, Strikeout: false, Green: true */