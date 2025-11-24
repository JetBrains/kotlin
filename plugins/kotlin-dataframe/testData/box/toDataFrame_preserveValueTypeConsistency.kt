import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val list = listOf(mapOf("a" to 1))
    val df = list.toDataFrame(maxDepth = 1)
    df.compareSchemas(strict = true)
    df.compileTimeSchema().print()

    class MapContainer(val map: Map<String, Int>)

    val list1 = listOf(MapContainer(mapOf("a" to 1)))
    val df1 = list1.toDataFrame(maxDepth = 2)
    df1.compareSchemas(strict = true)
    df1.compileTimeSchema().print()
    return "OK"
}
