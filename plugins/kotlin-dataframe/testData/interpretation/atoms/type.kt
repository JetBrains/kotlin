import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

internal fun typeTest() {
    test(id = "type_1", call = type<Any?, _> { 42 })
}