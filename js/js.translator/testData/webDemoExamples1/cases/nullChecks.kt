// Return null if str does not hold a number
fun myParseInt(str: String): Int? = str.toIntOrNull().also {
    if (it == null) println("One of arguments isn't Int")
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        print("No number supplied");
    }
    else {
        val x = myParseInt(args[0])
        val y = myParseInt(args[1])

        // We cannot say 'x * y' now because they may hold nulls
        if (x != null && y != null) {
            print(x * y) // Now we can
        }
    }
}
