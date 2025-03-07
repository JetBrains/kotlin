import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.experimental.ExperimentalTypeInference
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

/**
char,level,race,charclass,zone,guild,timestamp
59425,1,Orc,Rogue,Orgrimmar,165,01/01/08 00:02:04
65494,9,Orc,Hunter,Durotar,-1,01/01/08 00:02:04
*/
@DataSchema
interface ActivePlayer {
    val char: Int
    val level: Int
    val race: String
    val charclass: String
    val zone: String
    val guild: Int
    val timestamp: String
}

/**
 * let's try to find bots among these players. say, players with uniterrutped play session of >24 hrs?
 */

fun main() {
    val df = DataFrame.read("")
    val df1 = df.cast<ActivePlayer>()

    val format = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")
    val df2 = df1
        .convert { timestamp }.with { LocalDateTime.parse(it, format) }

    val date: LocalDateTime = df2.timestamp[0]
}

fun box() = "OK"
