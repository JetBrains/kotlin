import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert0 {
    val year: Int
}

fun convert0(df: DataFrame<Convert0>) {
    val df1 = df.convert("year").to<String>().cast<Any>()
    fun col0(v: kotlin.String) {}
    col0(df1.year[0])
}

@DataSchema
interface Convert1 {
    val direction: String
}

enum class Direction { NORTH, SOUTH, WEST, EAST }

val DataFrame<Int>.direction: DataColumn<Direction> get() = TODO()

fun convert1(df: DataFrame<Convert1>) {
    val df1 = df.convert("direction").to<Direction>().cast<Int>()

    fun col0(v: DataColumn<Direction>) {}
    col0(df1.direction)
}