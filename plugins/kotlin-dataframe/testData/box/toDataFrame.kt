import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import java.time.Year

enum class Switch {
    ON, OFF
}

class Ss(
    val s3: Int,
)

class Record(val r: String)

class S(
    val str: String,
    val s1: Int,
    val s2: Ss,
    val switch: Switch,
    val temporal: Year,
    val boolean: Boolean,
    val instant: Instant,
    val numberI: Int,
    val number: Number,
    val numberFloat: Float,
    val ll: List<String>,
    val lld: List<Record>,
    val nullableNumber: Int?,
)

fun box(): String {
    val res = listOf(
        S(
            "123",
            321,
            Ss(12),
            Switch.ON,
            Year.now(), true,
            Clock.System.now(),
            12,
            12,
            12f,
            listOf("dd"),
            listOf(Record("ff")),
            null,
        ),
    ).toDataFrame(maxDepth = 2)
    res.str.print()
    res.s1.print()
    res.s2.s3.print()
    res.switch.print()
    res.temporal.print()
    res.boolean.print()
    res.instant.print()
    res.numberI.print()
    res.number.print()
    res.numberFloat.print()
    res.ll.print()
    res.lld.forEach { it.r.print() }
    res.nullableNumber
    return "OK"
}
