package ctrl_click

fun IntArray(vararg content : Int) : IntArray = content
fun <T> array(vararg t : T) : Array<T> = t

fun main(args : Array<String>) {
    var a = <caret>IntArray(array(1, 2, 3))
}

// REF: (ctrl_click).IntArray(vararg Int)
