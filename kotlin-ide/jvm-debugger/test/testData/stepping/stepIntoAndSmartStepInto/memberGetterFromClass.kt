package memberGetterFromClass

class A {
    fun bar() {
        //Breakpoint!
        foo
    }

    val foo: Int
        get() {
            val a = 1
            return 1
        }
}

fun main(args: Array<String>) {
    A().bar()
}
