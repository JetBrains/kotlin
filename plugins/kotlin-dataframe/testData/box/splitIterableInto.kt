import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    run {
        val df = dataFrameOf("a" to listOf(listOf(1, 2))).split { a }.into("a", "b")
        val col1: DataColumn<Int?> = df.a
        val col2: DataColumn<Int?> = df.b
        df.compareSchemas()
    }

    run {
        val df = dataFrameOf("a" to listOf(listOf(1, 2))).split { a }.inward("a", "b")
        val col1: DataColumn<Int?> = df.a.a
        val col2: DataColumn<Int?> = df.a.b
        df.compareSchemas()
    }

    return "OK"
}
