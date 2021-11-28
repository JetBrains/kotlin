package lab2

fun main() {
    println("something")
    val obj = SomePojo()
    obj.name = "test"
    val s: String = obj.name
    obj.age = 12
    val v = obj.isHuman
    obj.isHuman = !v
    println(obj)
//
//    val manualPojo = ManualPojo()
//
//    val foo: String? = manualPojo.getFoo()
//    val res: Any? = manualPojo.someMethod()
//
    val ddd = SomeData()

    JavaUsage.cycleUsage()
}

class SomeKotlinClass {
    fun call() {
        val ddd = SomeData()
        ddd.age = 12
        println(ddd)
    }
}
