import kotlinx.cinterop.*
import stdio.*

fun parseLine(line: String, separator: Char) : List<String> {
    val result = mutableListOf<String>()
    var builder = StringBuilder()
    var quotes = 0
    for (ch in line) {
        when {
            ch == '\"' -> {
                quotes++
                builder.append(ch)
            }
            (ch == '\n') || (ch ==  '\r') -> {}
            (ch == separator) && (quotes % 2 == 0) -> {
                result.add(builder.toString())
                builder = StringBuilder()
            }
            else -> builder.append(ch)
        }
    }
    return result
}

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("usage: csvparser.kexe file.csv column count")
        return
    }
    val fileName = args[0]
    val column = args[1].toInt()
    val count = args[2].toInt()

    val file = fopen(fileName, "r")
    if (file == null) {
        perror("cannot open input file $fileName")
        return
    }

    try {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<CInt8Var>(bufferLength)

            for (i in 1..count) {
                val nextLine = fgets(buffer[0].ptr, bufferLength, file)?.toKString()
                if (nextLine == null || nextLine.isEmpty()) break

                val records = parseLine(nextLine, ',')
                println(records[column])
            }
        }
    } finally {
        fclose(file)
    }
}