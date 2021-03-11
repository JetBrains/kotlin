internal class Test {
    fun <A, B> foo(value: A, `fun`: FunctionalI<A, B>?): B {
        TODO("_root_ide_package_")
    }

    fun toDouble(x: Int?): Double {
        TODO("_root_ide_package_")
    }

    fun nya(): Double {
        return foo(1, { x: Int? -> this.toDouble(x) })
    }
}
