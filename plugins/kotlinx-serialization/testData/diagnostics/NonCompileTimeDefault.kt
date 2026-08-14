// WITH_STDLIB

import kotlinx.serialization.*

class DateTime {
    companion object {
        fun now(): DateTime = DateTime()
    }
}

object DateTimeSerializer : KSerializer<DateTime> by TODO()

const val CONST_DEFAULT = "const"

val TOP_LEVEL_VAL = "computed".length

enum class MyEnum { A, B }

@Serializable
class Reevaluated(
    val name: String,
    @Contextual val date: DateTime = <!NON_COMPILE_TIME_DEFAULT_VALUE!>DateTime.now()<!>,
    val time: Long = <!NON_COMPILE_TIME_DEFAULT_VALUE!>currentMillis()<!>,
)

@Serializable
class ReevaluatedInBody {
    var counter: Int = <!NON_COMPILE_TIME_DEFAULT_VALUE!>nextCounter()<!>
}

// Pinned explicitly — nothing to warn about.
@Serializable
class Pinned(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @Contextual val always: DateTime = DateTime.now(),
    @EncodeDefault(EncodeDefault.Mode.NEVER) @Contextual val never: DateTime = DateTime.now(),
    @Required @Contextual val required: DateTime = DateTime.now(),
)

@Serializable
class Stable(
    val i: Int = 42,
    val s: String = "literal",
    val interpolated: String = "$CONST_DEFAULT-suffix",
    val fromConst: String = CONST_DEFAULT,
    val fromVal: Int = TOP_LEVEL_VAL,
    val nullable: String? = null,
    val negative: Int = -1,
    val enumEntry: MyEnum = MyEnum.A,
    val emptyList: List<Int> = emptyList(),
    val emptyMap: Map<Int, Int> = emptyMap(),
    val listOfNothing: List<Int> = listOf(),
    val mutable: MutableList<Int> = mutableListOf(),
    val array: Array<String> = emptyArray(),
    // A fresh instance of a serializable data class compares equal to the default.
    val nested: Nested = Nested(),
)

@Serializable
class Dependent(
    val i: Int = 1,
    // Deterministic given `i`, so the comparison in write$Self behaves consistently.
    val dependent: Int = i + 1,
    val nestedWithConstArg: Nested = Nested(CONST_DEFAULT.length),
    // An operator does not launder a re-evaluated operand.
    val notDependent: Long = <!NON_COMPILE_TIME_DEFAULT_VALUE!>currentMillis() + 1<!>,
    val nestedWithCallArg: Nested = <!NON_COMPILE_TIME_DEFAULT_VALUE!>Nested(nextCounter())<!>,
)

// No default at all.
@Serializable
class NoDefaults(val i: Int, val s: String)

@Serializable
data class Nested(val i: Int = 0)

fun currentMillis(): Long = 0L

fun nextCounter(): Int = 0
