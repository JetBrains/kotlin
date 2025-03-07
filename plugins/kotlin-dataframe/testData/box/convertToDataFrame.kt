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

class Aaa(val a: List<Rooms>)

fun box(): String {
    val rooms = dataFrameOf(Rooms(1, 2, "n"))
    val sessions = dataFrameOf(Sessions(listOf(1, 2)))

    val df = sessions.convert { roomId }.with {
        listOf(Aaa(listOf(Rooms(1, 2, "n")))).toDataFrame(maxDepth = 2)
    }

    df.roomId[0].a[0].id
    return "OK"
}
