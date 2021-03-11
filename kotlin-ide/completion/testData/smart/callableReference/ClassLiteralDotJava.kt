fun<T : Any> foo(p: Class<T>){}

fun bar() {
    foo(String::<caret>)
}

// EXIST_JAVA_ONLY: { lookupString: "class.java", itemText: "class", tailText: ".java", attributes: "bold" }
// NOTHING_ELSE
