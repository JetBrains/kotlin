package accessToOverridenPropertyWithBackingField

open class B1 {
    val prop = "OK"
}

class D1: B1()

fun main(args: Array<String>) {
    val d = D1()
    //Breakpoint!
    d.prop // <-- breakpoint here
}

// EXPRESSION: d.prop
// RESULT: "OK": Ljava/lang/String;