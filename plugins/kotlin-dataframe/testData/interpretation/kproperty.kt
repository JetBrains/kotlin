import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*

internal interface Schema {
    val i: Int
    @ColumnName("name")
    val wwff: Int
}

internal fun kpropertyTest() {
    test(id = "kproperty_1", kproperty(Schema::i))
    test(id = "kproperty_2", kproperty(Schema::wwff))
}
