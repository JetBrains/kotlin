import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class A(
    @ColumnName("hello_world")
    val helloWorld: Int
)

fun box(): String {
    val df = dataFrameOf(A(123)).convert { helloWorld }.with { "test" }
    val col: DataColumn<String> = df.hello_world

    val df1 = dataFrameOf(A(123)).rename { helloWorld }.into("hiWorld")
    val col1: DataColumn<Int> = df1.hiWorld
    return "OK"
}
