import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "lists" to columnOf(
            listOf(1, 2),
            listOf(1, 2, 3),
            listOf(1),
        )
    )

    val res = df.split { lists }.by { it }.default(0).into("a", "b", "c")
    val col: DataColumn<Int> = res.a

    val res1 = df.split { lists }.by { it }.default(null).into("a", "b", "c")
    val col1: DataColumn<Int?> = res1.a

    val res2 = df.split { lists }.by { it }.default(null).default(0).into("a", "b", "c")
    val col2: DataColumn<Int> = res2.a
    return "OK"
}
