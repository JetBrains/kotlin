// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateSecondaryConstructorAction
open class Base(n: Int) {
    constructor(a: Int, b: Int): this(a + b)
}

class Foo : Base {<caret>
    val n: Int

    val x = 1

    fun foo() {

    }

    fun bar() {

    }
}