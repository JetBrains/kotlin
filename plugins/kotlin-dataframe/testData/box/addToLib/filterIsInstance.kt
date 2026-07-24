import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.typeOf

@Refine
@Interpretable("FilterIsInstance")
// T parameter needs to be captured to resolve extensions in columns lambda. It won't work with <*, *>
// As an option, if filterIsInstance is a member function, T can be captured from DataFrame declaration
public inline fun <T, reified R> DataFrame<T>.filterIsInstance(noinline columns: ColumnsSelector<T, *>): DataFrame<*> {
    val resolved = getColumns(columns)
    val size = resolved.firstOrNull()?.size() ?: return this
    val indices = ArrayList<Int>()
    repeat(size) { row ->
        if (resolved.all { it[row] is R }) indices.add(row)
    }
    return getRows(indices).replace(columns = columns).with { DataColumn.createByType(it.name(), it.values() as List<R>, typeOf<R>()) }
}

fun box(): String {
    val df = (0 until 5).toDataFrame {
        "index" from { it }
        "mixed" from { (if (it % 2 == 0) it.toDouble() else it) as Any }
    }
        .filterIsInstance<_, Int> { mixed }
    val i: Int = df[0].mixed
    df.compareSchemas(strict = true)
    return "OK"
}
