import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = @Import DataFrame.readExcel("testResources/sample.xls", stringColumns = StringColumns("A"))
    val d1: String = df.col1[0]
    val d2: Double = df.col2[0]
    return "OK"
}
