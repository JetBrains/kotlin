// PROBLEM: none

fun main(args: Array<String>) {
    val a: Testtt? = Testtt()
    // Controversial case, better to do nothing here
    if (a<caret> != null && a.a?.a != null) {

    }
}

class Testtt {
    val a: Testtt? = null
}