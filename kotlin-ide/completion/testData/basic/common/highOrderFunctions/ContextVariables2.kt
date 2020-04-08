interface I : () -> Unit

fun String.xfoo(p: () -> Unit){}

val global: () -> Unit = { }

fun X.test(p1: () -> Unit, p2: () -> String, p3: I) {
    val local: () -> Unit = { }
    "a".xfoo<caret>
}

interface X {
    public val publicVal: () -> Unit
    protected val protectedVal: () -> Unit
}

val X.extension: () -> Unit
    get() = {}

val String.wrongExtension: () -> Unit
    get() = {}

// EXIST: { itemText: "xfoo", tailText: " {...} (p: () -> Unit) for String in <root>", typeText:"Unit" }
// EXIST: { itemText: "xfoo", tailText: "(p1) for String in <root>", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(p3) for String in <root>", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(local) for String in <root>", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(global) for String in <root>", typeText: "Unit" }
// EXIST: { itemText: "xfoo", tailText: "(publicVal) for String in <root>", typeText: "Unit" }

// excluded by performance reasons:
// ABSENT: { itemText: "xfoo", tailText: "(extension) for String in <root>", typeText: "Unit" }

// NOTHING_ELSE
