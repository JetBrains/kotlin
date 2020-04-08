class A {
    val x: String
        get() = "a<caret>bc"
}

// TYPE: "abc" -> <html>String</html>
