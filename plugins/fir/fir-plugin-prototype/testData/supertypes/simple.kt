import org.jetbrains.kotlin.fir.plugin.C

@C
class A {
    class MyNested : NestedClass()

    fun test() {
        MyNested().foo()
    }
}