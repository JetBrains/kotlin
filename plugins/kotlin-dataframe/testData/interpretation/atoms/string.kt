import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

fun injectionScope() {
    test(id = "string_1", call = kotlinPrimitive("42"))
}
