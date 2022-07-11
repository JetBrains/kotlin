import org.jetbrains.kotlinx.dataframe.plugin.testing.*

fun injectionScope() {
    test(id = "string_1", call = f("42"))
}
