import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

internal fun rowValueExpressionTest() {
    test(id = "rowValueExpression_1", rowValueExpression<Any?, _> { 42 })
}