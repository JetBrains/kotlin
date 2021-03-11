class Test {

    val test = InnerClass(this)
}

fun main(args: Array<String>) {
    val test = Test()
    val innerClass = InnerClass(test)
    innerClass.fun1()
}

fun Test.foo() {
    InnerClass(this)
}