// ISSUE: KT-67355

open class A {
    open val foo: String = "1"
}

class B : A() {
    override val foo: String by lazy {
        // Ensure that an inline anonymous function is generated instead of a factory function call
        // CHECK_NOT_CALLED_IN_SCOPE: function=B$foo$delegate$lambda scope=new_B_pyal3a_k$ TARGET_BACKENDS=JS_IR_ES6
        // CHECK_NOT_CALLED_IN_SCOPE: function=B$foo$delegate$lambda scope=B TARGET_BACKENDS=JS_IR

        // CHECK_SUPER_COUNT: function=new_B_pyal3a_k$ count=0 includeNestedDeclarations=true TARGET_BACKENDS=JS_IR_ES6
        super.foo + "2" + ({ "(" + super.foo + ")" }).invoke()
    }

    fun baz(x: String): () -> String {
        // CHECK_SUPER_COUNT: function=baz_8yhxfl_k$ count=2 includeNestedDeclarations=true TARGET_BACKENDS=JS_IR_ES6
        return {
            super.foo + x + ({ super.foo + x + "!" }).invoke()
        }
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
    result += B().baz("3")()
    result += " "
    result += bar("4")()

    return if (result == "12(1) 1313! 14") {
        "OK"
    } else {
        result
    }
}
