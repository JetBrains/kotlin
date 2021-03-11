// FLOW: IN

class AClass(name1: String){
    var name : String = name1
    fun uses(){
        name = "uses: bye"
        this.name = "uses: after this"
        val v = "And now my name is '$name'"
    }
}

fun main(args: Array<String>) {
    val a = AClass("main: hello")
    a.name = "main: bye"
    val v = "Now my name is '${a.<caret>name}'"
}