// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class Test {
    fun bar(a: String) = 1

    fun test(x: Int) {
        val foo: (a: String) -> Int = when (x) {
            1 -> <caret>this::bar
            else -> this::bar
        }
    }
}