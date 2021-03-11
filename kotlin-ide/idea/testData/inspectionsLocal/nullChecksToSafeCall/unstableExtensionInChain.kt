// PROBLEM: none
val a: Testtt? = Testtt()

fun main(args: Array<String>) {
    if (a<caret> != null && a.a != null) {

    }
}

class Testtt

val Testtt?.a: Testtt? get() = null