import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Refine
@Interpretable("Require0")
inline fun <T, reified C> DataFrame<T>.requireColumn(noinline column: ColumnSelector<T, C>): DataFrame<T> =
    requireImpl(column, typeOf<C>())

fun <T, C> DataFrame<T>.requireImpl(column: ColumnSelector<T, C>, type: KType): DataFrame<T> {
    val resolvedColumn = getColumnWithPath(column)
    val actualType = resolvedColumn.data.type
    require(resolvedColumn.data.isSubtypeOf(type)) {
        "Column '${resolvedColumn.path.joinToString()}' has type '$actualType', which is not subtype of required '$type' type."
    }
    return this
}

@DataSchema
interface Schema {
    val abc: Any
}

fun box(): String {
    val rawDf = DataFrame.readJsonStr("[{\"abc\":\"123\"}]")
    rawDf.requireColumn { "abc"<String>() }.let {
        val v: String = it[0].abc
    }
    return "OK"
}
