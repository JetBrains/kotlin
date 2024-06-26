import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = @Import DataFrame.readJson("testResources/achievements_all.json")

    val df2 = df.explode { achievements }

    val df3 = df2
        .filter { achievements.preStage != null }
        .join(df2) { achievements.preStage.match(right.achievements.id) }
    df3.achievements.name1
    return "OK"
}
