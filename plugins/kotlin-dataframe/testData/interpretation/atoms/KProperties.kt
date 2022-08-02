import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

internal interface KProperties {
    val col1: Int
    val col2: Int?
}

internal fun varargKPropertyTest() {
    test(id = "varargKProperty_0", call = varargKProperty(KProperties::col1, KProperties::col2))
}
