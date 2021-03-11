public class BaseClass : SuperClass() {
    val a: Int = 3

    val <warning descr="SSR">b</warning>: Int = 45

    val y: Any = 34

    val str: String = "test"
}

open class SuperClass : SuperSuperClass() {
    val c = 30
}

abstract class SuperSuperClass {
    val <warning descr="SSR">d</warning> = 45
}