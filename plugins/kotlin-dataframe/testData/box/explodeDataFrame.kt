import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = dataFrameOf(
        "achievements" to List(2) { dataFrameOf("order")(1, 2) }
    )

    val df1 = df.explode { achievements }
    df1.achievements.order
    return "OK"
}
