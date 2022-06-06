import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.*

class OuterClass

@org.jetbrains.kotlinx.dataframe.annotations.DataSchema(isOpen = false)
interface Hello {
    val name: String
    val `test name`: InnerClass
    val nullableProperty: Int?
    val a: () -> Unit
    val d: List<List<*>>

    class InnerClass
}

val ColumnsContainer<Hello>.col1: DataColumn<String> get() = name
val ColumnsContainer<Hello>.col2: DataColumn<Hello.InnerClass> get() = `test name`
val ColumnsContainer<Hello>.col3: DataColumn<Int?> get() = nullableProperty
val ColumnsContainer<Hello>.col4: DataColumn<() -> Unit> get() = a
val ColumnsContainer<Hello>.col5: DataColumn<List<List<*>>> get() = d

val DataRow<Hello>.row1: String get() = name
val DataRow<Hello>.row2: Hello.InnerClass get() = `test name`
val DataRow<Hello>.row3: Int? get() = nullableProperty
val DataRow<Hello>.row4: () -> Unit get() = a
val DataRow<Hello>.row5: List<List<*>> get() = d