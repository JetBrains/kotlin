class C {
    companion object {
        fun create(p: (Int) -> Unit) = C()
    }
}

val handler: (Int) -> Unit = {}

fun v: C = cr<caret>

// EXIST: { allLookupStrings: "C, create", itemText: "C.create", tailText: " {...} (p: (Int) -> Unit) (<root>)", typeText:"C" }
// EXIST: { allLookupStrings: "C, create", itemText: "C.create", tailText: "(handler) (<root>)", typeText:"C" }
