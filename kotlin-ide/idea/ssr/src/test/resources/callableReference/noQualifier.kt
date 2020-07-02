fun Int.isOddExtension() = this % 2 != 0

class MyClazz {
    fun isOddMember(x: Int) = x % 2 != 0
    fun constructorReference(init: () -> MyClazz) { print(init) }

    fun foo() {
        val functionTwo: (Int) -> Boolean = Int::isOddExtension
        val functionOne: (Int) -> Boolean = <warning descr="SSR">::isOddMember</warning>
        constructorReference(<warning descr="SSR">::MyClazz</warning>)
        print(functionOne(1) == functionTwo(2))
    }
}