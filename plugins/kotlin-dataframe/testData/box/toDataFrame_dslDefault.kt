import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Measurement(val v: Int)

fun box(): String {
    val data = listOf(Measurement(2), Measurement(3))

    val df = data.toDataFrame {
        properties()
    }

    df.compareSchemas(strict = true)

    return "OK"
}
