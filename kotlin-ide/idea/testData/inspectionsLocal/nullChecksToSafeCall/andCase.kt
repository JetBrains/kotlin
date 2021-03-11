fun main(args: Array<String>) {
    val a: Testtt? = Testtt()
    if (a<caret> != null && a.a != null && a.a.a != null && a.a.a.a != null) {

    }
}

class Testtt {
    val a: Testtt? = null
}