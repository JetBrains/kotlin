import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*


fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1,2,3)
    val df1 = df.groupBy { a }
    df1.keys.compareSchemas(strict = true)
    return "OK"
}
