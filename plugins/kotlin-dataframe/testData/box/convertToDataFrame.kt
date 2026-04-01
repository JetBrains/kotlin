import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Sessions(
    val roomId: List<Int>
)

@DataSchema
data class Rooms(
    val id: Int,
    val sort: Int,
    val name: String,
)

class Wrapper(val rooms: List<Rooms>)

fun box(): String {
    val rooms = dataFrameOf(Rooms(1, 2, "n"))
    val sessions = dataFrameOf(Sessions(listOf(1, 2)))

    val df = sessions.convert { roomId }.with {
        listOf(Wrapper(listOf(Rooms(1, 2, "n")))).toDataFrame(maxDepth = 2)
    }

    // test 1: column converted to nested schema, structure is available
    df.roomId[0].rooms[0].id

    // test 2: add operation correctly extracted full schema from df
    df.add("test") { 1 }.let {
        val v: Int = it[0].roomId[0].rooms[0].id
        it.compareSchemas(strict = true)
    }
    return "OK"
}
