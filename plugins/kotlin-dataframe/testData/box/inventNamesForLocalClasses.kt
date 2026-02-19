import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("versions")(mapOf("a" to 1)).convert { versions }.with { dataFrameOf(it.keys)(it.values) }
    return "OK"
}
