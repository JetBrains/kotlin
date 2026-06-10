import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface WrongSchema {
    val wrongCol: Int
}

fun box(): String {

    val df = dataFrameOf("actualCol" to listOf(1, 2, 3)).cast<WrongSchema>(verify=false)

    try {
        df.filter { wrongCol > 5 }
    } catch (e: IllegalStateException) {
        require(e.cause is IllegalArgumentException)
        return "OK"
    }

    return "FAIL"
}
