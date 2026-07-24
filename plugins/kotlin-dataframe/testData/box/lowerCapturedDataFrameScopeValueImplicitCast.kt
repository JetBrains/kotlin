import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf("name" to columnOf("abc"))

    df.groupBy { name }.aggregate {
        it.add("name1") { name }
        let { it }
    }

    return "OK"
}
