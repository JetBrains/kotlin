fun foo(p: String?): String {
    println()
    bar(p!!, <before><change>)
}

fun bar(s: String, p: Int) { }

// BACKSPACES: 4
// TYPE: "."
// EXIST: { itemText: "substring", attributes: "grayed" }