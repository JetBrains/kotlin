// "Change getter type to HashSet<Int>" "true"

class A() {
    val i: java.util.HashSet<Int>
        get(): <caret>Any = java.util.LinkedHashSet<Int>()
}
