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

@Refine
@Interpretable("Require1")
inline fun <T, reified C> DataFrame<T>.requireColumn1(noinline column: ColumnSelector<T, C>): DataFrame<T> =
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
    val abc: Int
}

fun box(): String {
    val rawDf = DataFrame.readJsonStr("[{\"abc\":\"123\"}]")
    rawDf
        // informing about a column that the compiler plugin doesn't know about yet
        .requireColumn1 { "abc"<String>() }
        .also {
            val v: String = it[0].abc
            it.checkCompileTimeSchemaEqualsRuntime()
        }

        // informing a column type has changed
        .parse()
        .requireColumn1 { "abc"<Int>() }
        .also {
            val v: Int = it[0].abc
            it.checkCompileTimeSchemaEqualsRuntime()
        }

        // informing about a nested column the compiler plugin doesn't know about
        .add { "group" { "new" from { "123" } } }
        .cast<Schema>(verify = false)
        .requireColumn1 { "group"["new"]<String>() }
        .also {
            val v: Int = it[0].abc
            val w: String = it[0].group.new
            it.checkCompileTimeSchemaEqualsRuntime()
        }

        // informing a nested column type has changed
        .parse()
        .requireColumn1 { "group"["new"]<Int>() }
        .also {
            val v: Int = it[0].abc
            val w: Int = it[0].group.new
            it.checkCompileTimeSchemaEqualsRuntime()
        }

    return "OK"
}
