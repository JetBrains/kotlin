import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
   val df2 = dataFrameOf("id", "name", "preStage")(
        1, "11", null,
        2, "22", 1,
        3, "33", null,
    ).group { id and name and preStage }.into("achievements")

    val df3 = df2
        .filter { achievements.preStage != null }
        .join(df2) { achievements.preStage.match(right.achievements.id) }
    df3.achievements.name1
    return "OK"
}
