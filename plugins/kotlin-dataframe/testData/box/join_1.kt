import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
data class Sessions(
    val roomId: Int
)

@DataSchema
data class Rooms(
    val id: Int,
    val sort: Int,
    val name: String,
)

fun box(): String {
    val rooms = dataFrameOf(Rooms(1, 2, "n"))
    val sessions = dataFrameOf(Sessions(1))

    val df = sessions.join(rooms.group { id and name and sort }.into("room")) { roomId.match(right.room.id) }
    df.roomId
    df.room.sort
    df.room.name
    return "OK"
}
