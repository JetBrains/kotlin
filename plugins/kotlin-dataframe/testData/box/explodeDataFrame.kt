import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val df = @Import DataFrame.readJson("testResources/achievements_all.json")

    val df1 = df.explode { achievements }
    df1.achievements.order
    return "OK"
}
