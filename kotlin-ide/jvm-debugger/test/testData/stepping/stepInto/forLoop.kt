package forLoop

// KT-5664 Wrong stepping through for loop
fun main(args: Array<String>) {
    //Breakpoint!
    var x = 1
    for (i in 0..1) {
        x++
    }
    var y = 1
    for (i in 0..1) {
        y++
        val y1 = y
    }
    val z = 1
}

// STEP_INTO: 17