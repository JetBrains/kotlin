import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*

internal fun insertClauseTest() {
    val df = dataFrameOf("a")(1)
    test(id = "insert_1", call = df.insert("b") { 42 })
}