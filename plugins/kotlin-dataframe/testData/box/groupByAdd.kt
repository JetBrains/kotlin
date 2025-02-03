import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.api.groupBy
import org.jetbrains.kotlinx.dataframe.io.*

enum class State {
    Idle,
    Productive,
    Maintenance,
}

class Event(val toolId: String, val state: State, val timestamp: Long)

fun box(): String {
    val tool1 = "tool_1"
    val tool2 = "tool_2"
    val tool3 = "tool_3"

    val events = listOf(
        Event(tool1, State.Idle, 0),
        Event(tool1, State.Productive, 5),
        Event(tool2, State.Idle, 0),
        Event(tool2, State.Maintenance, 10),
        Event(tool2, State.Idle, 20),
        Event(tool3, State.Idle, 0),
        Event(tool3, State.Productive, 25),
    ).toDataFrame()

    val lastTimestamp = events.maxOf { timestamp }
    val groupBy = events
        .groupBy { toolId }
        .sortBy { timestamp }
        .add("stateDuration") {
            (next()?.timestamp ?: lastTimestamp) - timestamp
        }.toDataFrame()

    groupBy.group[0].stateDuration

    groupBy.compareSchemas(strict = true)
    return "OK"
}
