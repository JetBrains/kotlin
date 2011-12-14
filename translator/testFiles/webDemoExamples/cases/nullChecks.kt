// Return null if str does not hold a number
fun parseInt(str : String) : Int? {
    try{
        return  Integer.parseInt(str)
    } catch (e: NumberFormatException) {
        System.out?.println("One of argument isn't Int")
    }
    return null
}

fun main(args : Array<String>) {
    if (args.size < 2) {
        System.out?.print("No number supplied");
    } else {
        val x = parseInt(args[0])
        val y = parseInt(args[1])

        // We cannot say 'x * y' now because they may hold nulls
        if (x != null && y != null) {
            System.out?.print(x * y) // Now we can
        }
    }
}
