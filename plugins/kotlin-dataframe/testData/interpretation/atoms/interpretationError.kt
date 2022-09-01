import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

fun interpretationErrorTest() {
    test(id = "interpretationError_1", call = <!ERROR!>interpretationError()<!>)
}