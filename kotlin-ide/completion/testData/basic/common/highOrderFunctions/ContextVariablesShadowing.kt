interface I1 {
    val v: () -> Unit
    val v1: () -> Unit
}

interface I2 {
    val v: String
}

fun String.xfoo(p: () -> Unit){}

fun X.test(i1: I1, i2: I2) {
    with(i1) {
        with(i2) {
            "a".xfoo<caret>
        }
    }
}

interface X

// EXIST: { itemText: "xfoo", tailText: " {...} (p: () -> Unit) for String in <root>", typeText:"Unit" }
// EXIST: { itemText: "xfoo", tailText: "(v1) for String in <root>", typeText: "Unit" }
// NOTHING_ELSE
