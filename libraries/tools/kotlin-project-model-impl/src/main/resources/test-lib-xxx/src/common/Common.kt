package testProject

expect class Printer {
    fun print(msg: String)
}

fun businessLogic(args: Array<String>, printer: Printer) =
    if (args.size != 2) {
        printer.print("2 Args required, got: ${args.joinToString(",")}")
    } else {
        businessLogic(args[0], args[1], printer)
    }

fun businessLogic(a: String, b: String, printer: Printer) {
    val ai = a.iterator()
    val bi = b.iterator()

    val charArray = CharArray(a.length + b.length)
    var ci = 0

    while (ai.hasNext() && bi.hasNext()) {
        charArray[ci] = ai.nextChar()
        charArray[ci + 1] = bi.nextChar()
        ci += 2
    }

    while (ai.hasNext()) {
        charArray[ci++] = ai.nextChar()
    }

    while (bi.hasNext()) {
        charArray[ci++] = bi.nextChar()
    }

    printer.print(charArray.concatToString())
}