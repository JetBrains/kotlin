import org.jetbrains.dataframe.annotations.*
import org.jetbrains.dataframe.columns.*
import org.jetbrains.dataframe.*

class OuterClass

@org.jetbrains.dataframe.annotations.DataSchema(isOpen = false)
interface Hello {
    val name: String
    val `test name`: InnerClass
    val nullableProperty: Int?
    val a: () -> Unit
    val d: List<List<*>>

    class InnerClass
}

val DataFrameBase<Hello>.col1: DataColumn<String> get() = name
val DataFrameBase<Hello>.col2: DataColumn<Hello.InnerClass> get() = `test name`
val DataFrameBase<Hello>.col3: DataColumn<Int?> get() = nullableProperty
val DataFrameBase<Hello>.col4: DataColumn<() -> Unit> get() = a
val DataFrameBase<Hello>.col5: DataColumn<List<List<*>>> get() = d

val DataRowBase<Hello>.row1: String get() = name
val DataRowBase<Hello>.row2: Hello.InnerClass get() = `test name`
val DataRowBase<Hello>.row3: Int? get() = nullableProperty
val DataRowBase<Hello>.row4: () -> Unit get() = a
val DataRowBase<Hello>.row5: List<List<*>> get() = d