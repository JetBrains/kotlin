public class BaseClass : SuperClass() {
    val <warning descr="SSR">a</warning>: Int = 3

    val <warning descr="SSR">b</warning> = 45

    val <warning descr="SSR">y</warning>: Any = 34

    val <warning descr="SSR">str</warning>: String = "test"
}

open class SuperClass : SuperSuperClass() {
    val <warning descr="SSR">c</warning> = 30
}

abstract class SuperSuperClass {
    val <warning descr="SSR">d</warning> = 50
}