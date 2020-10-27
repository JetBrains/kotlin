package coverage.basic.controlflow

fun main() {

    // If Expression

    var a = 1
    var b = 2
    if (a < b) println("a < b")

    if (a > b) {
        println("a > b")
    } else if (a == b) {
        println("a == b")
    } else {
        println("a < b")
    }

    if (a < b) {
        println("a < b")
    }
    else
    {
        println("a >= b")
    }

    var max = if (a > b) a else b

    max = if (a > b) {
        println("Choose a")
        a
    } else {
        println("Choose b")
        b
    }

    if (a < b)

        println("a < b")
    else

        println("a >= b")

    if (a > b)

        println("a > b")

    else

        println("a <= b")

    // When Expression

    when {
        a < b -> {
            println("a < b")
        }

        a == b ->

            println("a == b")
        a > b -> {
            println("a > b")
        }

        else -> {



        }
    }

    var x = 1
    when (x) {
        1 -> print("x == 1")
        2 -> print("x == 2")
        else -> { // Note the block
            print("x is neither 1 nor 2")
        }
    }
    x = 2
    when (x) {
        1 -> print("x == 1")
        2 -> print("x == 2")
        else -> { // Note the block
            print("x is neither 1 nor 2")
        }
    }
    x = 3
    when (x) {
        1 -> print("x == 1")
        2 -> print("x == 2")
        else -> { // Note the block
            print("x is neither 1 nor 2")
        }
    }

    when (x) {
        0, 1
        ->
            print("x == 0 or x == 1")
        else ->
            print("otherwise")
    }

    when {
        else -> println("=)")
    }

    // While Loops

    do {
        b++
    } while (b < 5)

    while (a < 10) {
        a++
        if (a > 7) {
            println(a)
        }
    }

    while (a > 0)

        a--
}