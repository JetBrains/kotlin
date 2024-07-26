// ISSUE: KT-67355

open class A {
    open val foo: String = "1"
}

class B : A() {
    override val foo: String by lazy {
        // In ES6 mode, we expect a factory function to be called.
        // CHECK_CALLED_IN_SCOPE: function=B$foo$delegate$lambda scope=new_B_pyal3a_k$ TARGET_BACKENDS=JS_IR_ES6

        // In non-ES6 mode, an inline anonymous function should be generated.
        // CHECK_NOT_CALLED_IN_SCOPE: function=B$foo$delegate$lambda scope=B TARGET_BACKENDS=JS_IR
        super.foo + "2"
    }
}

fun bar(x: String): () -> String {
    return {
        // Ensure that an inline anonymous function is generated instead of a factory function call
        // CHECK_NOT_CALLED_IN_SCOPE: function=bar$lambda scope=bar
        object : A() {
            override val foo =
                super.foo + // This 'super' should not affect the generation of the lambda, because it's the local class's 'super'
                        x // Make sure the lambda is contextful so it's not lifted
        }.foo
    }
}

fun box(): String {
    var result = ""

    result += B().foo
    result += " "
    result += bar("3")()

    return if (result == "12 13") {
        "OK"
    } else {
        result
    }
}
