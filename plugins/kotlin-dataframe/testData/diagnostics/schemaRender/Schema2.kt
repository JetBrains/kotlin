import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.schemaRender.*

/*
@DataSchema(isOpen = false)
interface Schema11 {
    val name: String
    val returnType: String
}

val ColumnsContainer<Schema11>.name: DataColumn<String>  get() = this["name"] as DataColumn<String>
val DataRow<Schema11>.name: String  get() = this["name"] as String
val ColumnsContainer<Schema11>.returnType: DataColumn<String>  get() = this["returnType"] as DataColumn<String>
val DataRow<Schema11>.returnType: String  get() = this["returnType"] as String

@DataSchema(isOpen = false)
interface Schema13 {
    val name: String
}

val ColumnsContainer<Schema13>.name: DataColumn<String>  get() = this["name"] as DataColumn<String>
val DataRow<Schema13>.name: String  get() = this["name"] as String

@DataSchema(isOpen = false)
interface Schema12 {
    val nestedGroup: DataRow<Schema13>
}

val ColumnsContainer<Schema12>.nestedGroup: ColumnGroup<Schema13>  get() = this["nestedGroup"] as ColumnGroup<Schema13>
val DataRow<Schema12>.nestedGroup: DataRow<Schema13>  get() = this["nestedGroup"] as DataRow<Schema13>

@DataSchema
interface Schema1 {
    val function: DataRow<Schema11>
    val functions: DataFrame<Schema11>
    val group: DataRow<Schema12>
    val name: String
}

val ColumnsContainer<Schema1>.function: ColumnGroup<Schema11>  get() = this["function"] as ColumnGroup<Schema11>
val DataRow<Schema1>.function: DataRow<Schema11>  get() = this["function"] as DataRow<Schema11>
val ColumnsContainer<Schema1>.functions: DataColumn<DataFrame<Schema11>>  get() = this["functions"] as DataColumn<DataFrame<Schema11>>
val DataRow<Schema1>.functions: DataFrame<Schema11>  get() = this["functions"] as DataFrame<Schema11>
val ColumnsContainer<Schema1>.group: ColumnGroup<Schema12>  get() = this["group"] as ColumnGroup<Schema12>
val DataRow<Schema1>.group: DataRow<Schema12>  get() = this["group"] as DataRow<Schema12>
val ColumnsContainer<Schema1>.name: DataColumn<String>  get() = this["name"] as DataColumn<String>
val DataRow<Schema1>.name: String  get() = this["name"] as String
*/

internal fun schemaTest() {
    val df = schema2()
    fun col0(v: kotlin.String) {}
    col0(df.name[0])
    fun col1(v: kotlin.String) {}
    col1(df.functions[0].name[0])
    fun col2(v: kotlin.String) {}
    col2(df.functions[0].returnType[0])
    fun col3(v: kotlin.String) {}
    col3(df.function.name[0])
    fun col4(v: kotlin.String) {}
    col4(df.function.returnType[0])
    fun col5(v: kotlin.String) {}
    col5(df.group.nestedGroup.name[0])
}
