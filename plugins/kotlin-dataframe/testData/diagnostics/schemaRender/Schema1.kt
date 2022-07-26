import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.schemaRender.*

/*
@DataSchema(isOpen = false)
interface Schema11 {
    val defaultValue: String?
    val name: String
    val returnType: String
}

val ColumnsContainer<Schema11>.defaultValue: DataColumn<String?>  get() = this["defaultValue"] as DataColumn<String?>
val DataRow<Schema11>.defaultValue: String?  get() = this["defaultValue"] as String?
val ColumnsContainer<Schema11>.name: DataColumn<String>  get() = this["name"] as DataColumn<String>
val DataRow<Schema11>.name: String  get() = this["name"] as String
val ColumnsContainer<Schema11>.returnType: DataColumn<String>  get() = this["returnType"] as DataColumn<String>
val DataRow<Schema11>.returnType: String  get() = this["returnType"] as String

@DataSchema
interface Schema1 {
    val function: String
    val functionReturnType: String
    val id: Int
    val parameters: DataFrame<Schema11>
    val receiverType: String
}

val ColumnsContainer<Schema1>.function: DataColumn<String>  get() = this["function"] as DataColumn<String>
val DataRow<Schema1>.function: String  get() = this["function"] as String
val ColumnsContainer<Schema1>.functionReturnType: DataColumn<String>  get() = this["functionReturnType"] as DataColumn<String>
val DataRow<Schema1>.functionReturnType: String  get() = this["functionReturnType"] as String
val ColumnsContainer<Schema1>.id: DataColumn<Int>  get() = this["id"] as DataColumn<Int>
val DataRow<Schema1>.id: Int  get() = this["id"] as Int
val ColumnsContainer<Schema1>.parameters: DataColumn<DataFrame<Schema11>>  get() = this["parameters"] as DataColumn<DataFrame<Schema11>>
val DataRow<Schema1>.parameters: DataFrame<Schema11>  get() = this["parameters"] as DataFrame<Schema11>
val ColumnsContainer<Schema1>.receiverType: DataColumn<String>  get() = this["receiverType"] as DataColumn<String>
val DataRow<Schema1>.receiverType: String  get() = this["receiverType"] as String
*/

internal fun schemaTest() {
    val df = schema1()
    fun col0(v: kotlin.Int) {}
    col0(df.id[0])
    fun col1(v: kotlin.String) {}
    col1(df.function[0])
    fun col2(v: kotlin.String) {}
    col2(df.functionReturnType[0])
    fun col3(v: kotlin.String) {}
    col3(df.parameters[0].name[0])
    fun col4(v: kotlin.String) {}
    col4(df.parameters[0].returnType[0])
    fun col5(v: kotlin.String?) {}
    col5(df.parameters[0].defaultValue[0])
    fun col6(v: kotlin.String) {}
    col6(df.receiverType[0])
}
