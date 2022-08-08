import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

@DataSchema
interface Convert1 {
    val direction: String
}

enum class Direction { NORTH, SOUTH, WEST, EAST }

fun convert1(df: DataFrame<Convert1>) {
    val df1 = df.convert("direction").to<Direction>().cast<Int>()

    fun col0(v: Direction) {}
    col0(df1.direction[0])
}