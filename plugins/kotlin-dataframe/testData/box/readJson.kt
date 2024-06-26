import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = DataFrame.readJson("https://raw.githubusercontent.com/Kotlin/dataframe/master/data/jetbrains.json")
    df.name.print()
    return "OK"
}
