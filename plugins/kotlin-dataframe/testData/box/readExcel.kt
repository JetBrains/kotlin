import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = @Import DataFrame.read("testResources/sample.xls")
    val d1: Double = df.col1[0]
    val d2: Double = df.col2[0]

    val df1 = @Import DataFrame.readExcel("testResources/sample.xls")
    val d11: Double = df1.col1[0]
    val d12: Double = df1.col2[0]
    return "OK"
}
