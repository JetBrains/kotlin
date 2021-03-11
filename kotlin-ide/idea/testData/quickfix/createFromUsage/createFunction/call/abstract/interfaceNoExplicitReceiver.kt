// "Create abstract function 'foo'" "true"
interface A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo(1, "2"))
    }
}