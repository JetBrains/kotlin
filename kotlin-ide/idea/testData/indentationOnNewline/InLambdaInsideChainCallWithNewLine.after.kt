class Test {
    fun foo(f: (String) -> String): Test = this
}

fun test() {
    val abc = Test()
            .foo { "Str" }
            .foo {
                <caret>
            }
}