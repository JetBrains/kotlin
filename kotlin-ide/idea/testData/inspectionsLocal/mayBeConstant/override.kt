// PROBLEM: none

interface Base {
    val property: Int
}

object Derived : Base {
    override val prop<caret>erty = 42
}