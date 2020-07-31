internal interface FunctionalI<A, B> {
    fun apply(x: A): B
}

internal class Test {
    fun <A, B> foo(value: A, `fun`: FunctionalI<A, B>): B {
        return `fun`.apply(value)
    }

    fun toDouble(x: Int?): Double {
        TODO("_root_ide_package_")
    }

    fun nya(): Double {
        TODO("_root_ide_package_")
    }
}
