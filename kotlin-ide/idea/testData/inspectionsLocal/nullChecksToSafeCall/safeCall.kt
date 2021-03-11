
fun main(args: Array<String>) {
    val a: Testtt? = Testtt()
    if (a?.a<caret> != null && a.a.a != null) {
 
    }
}

class Testtt {
    val a: Testtt? = null
}