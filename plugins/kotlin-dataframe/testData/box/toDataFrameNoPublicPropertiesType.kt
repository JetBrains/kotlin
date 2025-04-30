import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

class NoPublicPropsClass(private val a: Int, private val b: String)

fun box(): String {
    val objs: List<NoPublicPropsClass> = listOf(NoPublicPropsClass(1, "a"), NoPublicPropsClass(2, "b"))
    val df = objs.toDataFrame()

    val objsCol: DataColumn<NoPublicPropsClass> = df.value

    return "OK"
}
