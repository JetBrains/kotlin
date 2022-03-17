import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*

fun `Dsl is evaluated to `(df: DataFrame<*>) {
    df.add {
        "col1" from { 5 }
    }
    df.col1
}