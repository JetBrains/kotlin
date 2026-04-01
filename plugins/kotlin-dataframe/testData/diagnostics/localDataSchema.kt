import org.jetbrains.kotlinx.dataframe.annotations.*

fun main() {
    @DataSchema
    data <!DATA_SCHEMA_LOCAL_DECLARATION!>class Local<!>(val a: Int)
}
