import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

enum class Switch {
    ON, OFF
}

class AnotherRecord(val d: Double)

class Ss(
    val s3: Int,
    val preservedProperty: AnotherRecord
)

class Record(val r: String)

class PreservedRecord(val i: Int)

class S(
    val str: String,
    val s1: Int,
    val s2: Ss,
    val switch: Switch,
    val boolean: Boolean,
    val numberI: Int,
    val number: Number,
    val numberFloat: Float,
    val ll: List<String>,
    val lld: List<Record>,
    val nullableNumber: Int?,
    val preservedRecord: PreservedRecord,
)

fun box(): String {
    val res = listOf(
        S(
            "123",
            321,
            Ss(12, AnotherRecord(3.0)),
            Switch.ON,
            true,
            12,
            12,
            12f,
            listOf("dd"),
            listOf(Record("ff")),
            null,
            PreservedRecord(3)
        ),
    ).toDataFrame {
        properties(maxDepth = 4) {
            exclude(Int::class)
            exclude(Record::r)
            preserve(PreservedRecord::class)
            preserve(Ss::preservedProperty)
        }
    }
    res.str.print()
    res.switch.print()
    res.boolean.print()
    res.number.print()
    res.numberFloat.print()
    res.ll.print()
    res.nullableNumber

    val preservedRecord: DataColumn<PreservedRecord> = res.preservedRecord
    val anotherRecord: DataColumn<AnotherRecord> = res.s2.preservedProperty
    return "OK"
}
