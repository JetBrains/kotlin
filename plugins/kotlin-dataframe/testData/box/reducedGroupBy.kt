import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val groupBy = dataFrameOf("a")(1, 1, 2, 3, 3).groupBy { a }.add("id") { index() }
    groupBy.maxBy { id }.into("group").compareSchemas()
    groupBy.maxBy { id }.into("group").compareSchemas()
    groupBy.first { id == 1 }.into("group").compareSchemas()
    groupBy.first().into("group").compareSchemas()
    groupBy.last { id == 1 }.into("group").compareSchemas()
    groupBy.last().into("group").compareSchemas()
    groupBy.minBy { id == 1 }.into("group").compareSchemas()
    return "OK"
}
