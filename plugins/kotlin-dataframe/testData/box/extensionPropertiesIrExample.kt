import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.*

interface Marker

val ColumnsContainer<Marker>.intProperty: DataColumn<Int> get() = this["intProperty"] <!UNCHECKED_CAST!>as DataColumn<Int><!>
val DataRow<Marker>.intProperty: Int get() = this["intProperty"] as Int
