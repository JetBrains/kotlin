val testing = 12
val test = "Hello"

val more = test<caret>

// EXIST: { lookupString: "test", itemText: "test", tailText: " (<root>)", typeText: "String", attributes: "" }
// EXIST: { lookupString: "testing", itemText: "testing", tailText: " (<root>)", typeText: "Int", attributes: "" }
