import org.jetbrains.kotlinx.dataframe.plugin.testing.*

internal fun typeTest() {
    test(id = "type_1", call = type<Any?, _> { 42 })
}