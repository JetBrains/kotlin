fun xfoo1(option1: String = "", option2: Int = 1, p: () -> Unit){}
fun xfoo2(option1: String = "", option2: Int = 1, p: (Int) -> Boolean){}
fun xfoo3(option1: String = "", option2: Int = 1, p: (String, Char) -> Unit){}
fun xfoo4(p: () -> Unit = {}){}

fun xfooBad1(option: String = "", notOption: Int, p: (String, Char) -> Unit){}
fun xfooBad2(option1: String = "", option2: Int = 1, p: (String, Char) -> Unit, option3: Int = 0){}

fun test(param: () -> Unit) {
    xfoo<caret>
}

// EXIST: { itemText: "xfoo1", tailText: "(option1: String = ..., option2: Int = ..., p: () -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo1", tailText: " {...} (..., p: () -> Unit) (<root>)", typeText:"Unit" }

// EXIST: { itemText: "xfoo2", tailText: "(option1: String = ..., option2: Int = ..., p: (Int) -> Boolean) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo2", tailText: " {...} (..., p: (Int) -> Boolean) (<root>)", typeText:"Unit" }

// EXIST: { itemText: "xfoo3", tailText: "(option1: String = ..., option2: Int = ..., p: (String, Char) -> Unit) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo3", tailText: " { String, Char -> ... } (..., p: (String, Char) -> Unit) (<root>)", typeText:"Unit" }

// EXIST: { itemText: "xfoo4", tailText: "(p: () -> Unit = ...) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo4", tailText: " {...} (p: () -> Unit = ...) (<root>)", typeText:"Unit" }
// EXIST: { itemText: "xfoo4", tailText: "(param) (<root>)", typeText:"Unit" }

// EXIST: { itemText: "xfooBad1", tailText: "(option: String = ..., notOption: Int, p: (String, Char) -> Unit) (<root>)", typeText:"Unit" }

// EXIST: { itemText: "xfooBad2", tailText: "(option1: String = ..., option2: Int = ..., p: (String, Char) -> Unit, option3: Int = ...) (<root>)", typeText:"Unit" }

// NOTHING_ELSE