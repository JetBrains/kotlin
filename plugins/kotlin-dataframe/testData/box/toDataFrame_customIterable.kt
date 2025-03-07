import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class D(
    val s: String
)

class Subtree(
    val p: Int,
    val l: List<Int>,
    val ld: List<D>,
)

class Root(val a: Subtree)

class MyList(val l: List<Root?>): List<Root?> by l

fun box(): String {
    val l = listOf(
        Root(Subtree(123, listOf(1), listOf(D("ff")))),
        null
    )
    val df = MyList(l).toDataFrame(maxDepth = 2)
    df.compareSchemas(strict = true)
    return "OK"
}
