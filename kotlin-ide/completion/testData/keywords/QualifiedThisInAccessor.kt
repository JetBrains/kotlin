val String.foo: Int
    get() = this@<caret>.length()

// EXIST: "this@foo"
// NOTHING_ELSE
