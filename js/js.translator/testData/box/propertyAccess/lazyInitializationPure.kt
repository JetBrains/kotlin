// IGNORE_BACKEND: JS
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt

val a = "A"

// FILE: B.kt
val b = "B".apply {}

val c = b

// FILE: C.kt
val d = "D".apply {}

val e = d

// FILE: main.kt

fun box(): String {
    // Get only e to initialize all properties in file
    e
    return if (
        js("a") === "A" &&
        js("typeof b") == "undefined" &&
        js("typeof c") == "undefined" &&
        js("d") === "D" &&
        js("e") === "D"
    )
        "OK"
    else "a = ${js("a")}; typeof b = ${js("typeof b")}; typeof c = ${js("typeof c")}; d = ${js("d")}; e = ${js("e")}"
}