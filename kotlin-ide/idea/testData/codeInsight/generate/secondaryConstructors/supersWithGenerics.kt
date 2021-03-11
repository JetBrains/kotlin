// ACTION_CLASS: org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateSecondaryConstructorAction
open class Base<X, Y>(n: X) {
    constructor(x: X, y: Y): this(x)
}

class Foo<U> : Base<U, Int> {<caret>
    val x = 1

    fun foo() {

    }

    fun bar() {

    }
}