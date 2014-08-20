package bar

fun topLevelFun(s: String) = "topLevelFun: ${s}";

var topLevelVar = 100

val topLevelVal = 200

class A(val v: String) {
    fun memA(s: String) = "memA: ${v} ${s}"
    var propVar: Int = 1000
    val propVal: Int = 2000
    var text: String = "text"
}

fun A.ext1(s: String): String = "A.ext1: ${this.v} ${s}"

var A.extProp: String
    get() = "${this.text}"
    set(value) {
        this.text = value
    }