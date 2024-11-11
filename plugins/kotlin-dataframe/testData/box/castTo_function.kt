import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.io.File

private fun convert(data: List<String>) = data.map { it.split(":") }.toDataFrame {
    "part1" from { it[0] }
    "part2" from { it[1].toInt() }
    "part3" from { it[2] }
}

fun serialize(data: List<String>, destination: File) {
    convert(data).writeJson(destination)
}

fun deserializeAndUse(file: File) {
    val df = DataFrame.readJson(file).castTo(schemaFrom = ::convert)
    df.part1.print()
}

fun box(): String {
    val file = File.createTempFile("temp", "json")
    serialize(listOf("b:1:abc", "c:2:bca"), file)
    deserializeAndUse(file)
    return "OK"
}
