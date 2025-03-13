import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.*
import kotlin.reflect.*
import kotlin.reflect.full.*

inline fun <reified T> runtimeSchema(row: DataRow<T>) = (typeOf<T>().classifier as KClass<*>).memberProperties.associateBy { it.name }
inline fun <reified T> runtimeSchema(row: DataFrame<T>) = (typeOf<T>().classifier as KClass<*>).memberProperties.associateBy { it.name }
fun <T> Map<String, T>.col(s: String) = get(s)!!

@DataSchema
class Record(val string: String)

fun box(): String {
    val df = dataFrameOf(Record("abc"))
    val df1 = df.add("row") {
        val row = dataFrameOf(Record("")).first()
        row.takeIf { index() % 2 == 0 }
    }

    require(runtimeSchema(df1.row[0]).col("string").returnType == typeOf<String?>())

    val row = dataFrameOf(Record(""))
        .add("int") { 1 }
        .add("double") { 3.0 }
        .add("char") { 'c' }
        .group { int and double }.into("g")
        .group { g and char }.into("f")
        .first()

    val df2 = df.add("row") {
        row.takeIf { index() % 2 == 0 }
    }

    require(runtimeSchema(df2.row.f.g[0]).col("int").returnType == typeOf<Int?>())
    require(runtimeSchema(df2.row.f[0]).col("char").returnType == typeOf<Char?>())
    return "OK"
}
