import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

fun addExpressionTest() {
    test(id = "addExpression_1", call = addExpression<Any?, _> { 42 })
    test(id = "addExpression_2", call = addExpression<Any?, Any?> { 42 })
}
