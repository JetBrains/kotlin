package test

class A {
    operator fun invoke(i: Int) {
    }
}

fun main(args: Array<String>) {
    val a = A()
    a(<caret>)
}

/*
Text: (<highlight>i: Int</highlight>), Disabled: false, Strikeout: false, Green: true
*/
