// PROBLEM: none
sealed class Foo {
    object BAR : Foo()

    companion object {
        val BAR: Foo = <caret>Foo.BAR
    }
}