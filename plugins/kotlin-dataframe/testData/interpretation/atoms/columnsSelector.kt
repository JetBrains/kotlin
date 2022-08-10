import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*
import org.jetbrains.kotlinx.dataframe.columns.*


internal interface Schema0 {
    val intField: Int
    val group: Group0
}

internal interface Group0 {
    val stringField: String
}

internal val ColumnsContainer<Schema0>.intField: DataColumn<Int> get() = TODO()
internal val ColumnsContainer<Schema0>.group: ColumnGroup<Group0> get() = TODO()
internal val ColumnsContainer<Group0>.stringField: DataColumn<String> get() = TODO()

internal fun columnsSelectorTest() {
    test(id = "columnsSelector_1", call = columnsSelector<Schema0, _> { intField })
    test(id = "columnsSelector_2", call = columnsSelector<Schema0, _> { group.stringField })
}
