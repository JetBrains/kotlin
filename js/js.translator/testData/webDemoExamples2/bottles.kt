/**
 * This example implements the famous "99 Bottles of Beer" program
 * See http://99-bottles-of-beer.net/
 *
 * The point is to print out a song with the following lyrics:
 *
 *     The "99 bottles of beer" song
 *
 *     99 bottles of beer on the wall, 99 bottles of beer.
 *     Take one down, pass it around, 98 bottles of beer on the wall.
 *
 *     98 bottles of beer on the wall, 98 bottles of beer.
 *     Take one down, pass it around, 97 bottles of beer on the wall.
 *
 *       ...
 *
 *     2 bottles of beer on the wall, 2 bottles of beer.
 *     Take one down, pass it around, 1 bottle of beer on the wall.
 *
 *     1 bottle of beer on the wall, 1 bottle of beer.
 *     Take one down, pass it around, no more bottles of beer on the wall.
 *
 *     No more bottles of beer on the wall, no more bottles of beer.
 *     Go to the store and buy some more, 99 bottles of beer on the wall.
 *
 * Additionally, you can pass the desired initial number of bottles to use (rather than 99)
 * as a command-line argument
 */

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printBottles(99)
    }
    else {
        val bottles = args[0].toIntOrNull()
        if (bottles != null) {
            printBottles(bottles);
        }
        else {
            println("You have passed '${args[0]}' as a number of bottles, " +
                    "but it is not a valid integral number")
        }
    }
}

fun printBottles(bottleCount: Int) {
    if (bottleCount <= 0) {
        println("No bottles - no song")
        return
    }

    println("The \"${bottlesOfBeer(bottleCount)}\" song\n")

    var bottles = bottleCount
    while (bottles > 0) {
        val bottlesOfBeer = bottlesOfBeer(bottles)
        print("$bottlesOfBeer on the wall, $bottlesOfBeer.\nTake one down, pass it around, ")
        bottles--
        println("${bottlesOfBeer(bottles)} on the wall.\n")
    }
    println("No more bottles of beer on the wall, no more bottles of beer.\n" +
            "Go to the store and buy some more, ${bottlesOfBeer(bottleCount)} on the wall.")
}

fun bottlesOfBeer(count: Int): String =
        when (count) {
            0 -> "no more bottles"
            1 -> "1 bottle"
            else -> "$count bottles"
        } + " of beer"

// From the std package
// This is an extension property, i.e. a property that is defined for the
// type Array<T>, but does not sit inside the class Array
fun <T> Array<T>.isEmpty() = size == 0
