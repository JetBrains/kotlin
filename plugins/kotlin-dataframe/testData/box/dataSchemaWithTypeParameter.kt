import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface Schema<T> {
    val b: T
}

inline fun <reified R> DataFrame<*>.convertB(): DataFrame<Schema<R>> {
    val convert = convert { col("b") }
    val res = @DisableInterpretation convert.to<String>()
    return res.cast<Schema<R>>(verify = false)
}

fun box(): String {
    val df = dataFrameOf("b" to columnOf(3.0, 4.0))

    val res: DataFrame<Schema<String>> = df.convertB<String>()

    val str: DataColumn<String> = res.b
    return "OK"
}
