import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class IncompatibleVersionErrorData<T>(val expected: T, val actual: T)
class DeserializedContainerSource(val incompatibility: IncompatibleVersionErrorData<*>)

fun box(): String  {
    val functions = listOf(DeserializedContainerSource(IncompatibleVersionErrorData(1, 2)))
    val df = functions.toDataFrame(maxDepth = 2)
    df.assert()
    return "OK"
}
