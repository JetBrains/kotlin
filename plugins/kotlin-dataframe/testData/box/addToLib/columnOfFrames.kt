// CHECK_TYPE_WITH_EXACT

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.columns.*
import org.jetbrains.kotlinx.dataframe.io.*

@Refine
@Interpretable("ColumnOfFrames")
public fun <T> myColumnOf(vararg frames: DataFrame<T>): FrameColumn<T> = columnOf(*frames)

fun box(): String {
    val single = myColumnOf(dataFrameOf("a" to columnOf(1)))
    checkExactType<Int>(single[0].a[0])
    dataFrameOf("frames" to single).compareSchemas(strict = true)

    val col = myColumnOf(
        dataFrameOf("a" to columnOf(1), "b" to columnOf("x")),
        dataFrameOf("a" to columnOf(2)),
    )
    checkExactType<Int>(col[0].a[0])
    checkExactType<Int>(col[1].a[0])
    dataFrameOf("frames" to col).compareSchemas(strict = true)

    val commonType = myColumnOf(
        dataFrameOf("value" to columnOf(1)),
        dataFrameOf("value" to columnOf(2.0)),
    )
    checkExactType<Number>(commonType[0].value[0])
    dataFrameOf("frames" to commonType).compareSchemas(strict = true)

    val nested = myColumnOf(
        dataFrameOf("group" to columnOf("x" to columnOf(1), "y" to columnOf("a"))),
        dataFrameOf("group" to columnOf("x" to columnOf(2))),
    )
    checkExactType<Int>(nested[0].group.x[0])
    dataFrameOf("frames" to nested).compareSchemas(strict = true)

    val df = dataFrameOf("id" to columnOf(1, 2), "frames" to col)
    checkExactType<Int>(df.frames[0].a[0])
    df.compareSchemas(strict = true)
    return "OK"
}
