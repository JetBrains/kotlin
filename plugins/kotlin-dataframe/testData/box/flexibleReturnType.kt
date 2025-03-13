import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.experimental.ExperimentalTypeInference
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface ActivePlayer {
    val char: Int
    val timestamp: String
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun <T, V : Temporal> DataRow<T>.diff(unit: ChronoUnit, expression: RowExpression<T, V>): Long? = prev()?.let { p -> unit.between(expression(this, this), expression(p, p)) }


fun box(): String {
    val df = @DisableInterpretation DataFrame.readDelimStr("""
        char,level,race,charclass,zone,guild,timestamp
        59425,1,Orc,Rogue,Orgrimmar,165,01/01/08 00:02:04
        65494,9,Orc,Hunter,Durotar,-1,01/01/08 00:02:04
    """.trimIndent())
    val df1 = df.cast<ActivePlayer>()
    val format = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    val df2 = df1
        .convert { timestamp }.with { LocalDateTime.parse(it, format)!! }
        .add("tsDiff") { diff(ChronoUnit.MINUTES) { timestamp }?.let { it > 20  } ?: true }
        //.add("charDiff") { diff { char }?.let { it != 0 } ?: true }

    df2.print()
    return "OK"
}
