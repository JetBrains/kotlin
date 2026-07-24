import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface WrongSchema {
    val wrongTypeCol: String
}

fun box(): String {

    val df = dataFrameOf("wrongTypeCol" to listOf(1, 2, 3)).cast<WrongSchema>(verify=false)

    try {
        df.filter { wrongTypeCol > "5" }
    } catch (e: IllegalStateException) {
        require(e.cause is ClassCastException)
        return "OK"
    }

    return "FAIL"
}
