import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.io.File

fun box(): String {
    val df = listOf(File("abc.csv")).toDataFrame(columnName = "data")
    val res: DataColumn<File> = df.data
    return "OK"
}
