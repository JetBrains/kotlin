import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("a", "b", "c")(1, 2, 3)

    val groupBy = df.groupBy { a }

    val df1 = groupBy.updateGroups { it.remove { a } }.toDataFrame()
    df1.compileTimeSchema().print()
    return "OK"
}
