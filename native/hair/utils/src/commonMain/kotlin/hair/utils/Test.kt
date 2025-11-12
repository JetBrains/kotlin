package hair.utils

fun main() {
    data class Foo(val args: MutableList<Any>)

    class BaseContext {
        val Foo.arg0: Any
            get() = args[0]
    }

    class ModifyContext {
        var Foo.arg0: Any
            get() = args[0]
            set(value) {
                args[0] = value
            }
    }

    val f = Foo(mutableListOf(1, 2, 3, 4))
    with(BaseContext()) {
        println("arg0: ${f.arg0}")
        with(ModifyContext()) {
            f.arg0 = 42
            println("arg0: ${f.arg0}")
        }
        println(f.args)
    }
}