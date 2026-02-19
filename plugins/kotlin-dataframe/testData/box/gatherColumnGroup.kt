import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "group" to columnOf(
            "nested" to columnOf(123321),
        ),
    )

    val res = df.gather { group }.into("key", "value")
    val col: DataColumn<Int> = res.value.nested
//    df.gather { group }.explodeLists().into("key", "value")
    return "OK"
}
