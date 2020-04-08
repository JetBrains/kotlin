// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateSecondaryConstructorAction
open class Base {
    constructor(a: Int, vararg b: Int)
}

class Foo : Base {<caret>
    val x = 1

    fun foo() {

    }

    fun bar() {

    }
}