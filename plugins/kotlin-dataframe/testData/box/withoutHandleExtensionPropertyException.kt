// WITHOUT_HANDLE_EXTENSION_PROPERTY_EXCEPTIONS
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface CorrectSchema {
    val col: Int
}

@DataSchema
interface WrongSchema {
    val col: String
}

fun box(): String {

    val dfCorrect = dataFrameOf("col" to listOf(1, 2, 3)).cast<CorrectSchema>()

    val correctCol: DataColumn<Int> = dfCorrect.col

    dfCorrect.select { col }
    dfCorrect.filter { col >= 2 }

    val dfWrong = dataFrameOf("col" to listOf(1, 2, 3)).cast<WrongSchema>(verify=false)

    try {
        dfWrong.filter { col == "2" }
    } catch (e: ClassCastException) { // catch unwrapped exception
         return "OK"
    }

    return "FAIL"
}
