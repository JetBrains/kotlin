package kt12206BasePropertyWithoutBackingField

abstract class Base {
    val prop: String?
        get() = "OK"
}

class Derived : Base()

fun main(args: Array<String>) {
    val d = Derived()
    //Breakpoint!
    d.prop // <-- breakpoint here
}

// EXPRESSION: d.prop
// RESULT: "OK": Ljava/lang/String;