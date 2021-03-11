// FLOW: IN
// RUNTIME_WITH_REFLECT

class AClass(name1: String){
    var name by D()
    init {
        name = name1
    }

    fun uses(){
        name = "bye"
        println("Now my name is '$<caret>name'")
    }
}

fun main(args: Array<String>) {
    val a = AClass("hello")
    println("My name is '${a.name}'")
}