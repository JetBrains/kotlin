import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.reflect.typeOf

fun box(): String {
    val nullableColumnDf = dataFrameOf(
        "a" to columnOf(1, 2, null, 3),
        "b" to columnOf("a", "a", "b", "b")
    )

    nullableColumnDf.implode { a }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("a").type == typeOf<List<Int?>>())
        val col: DataColumn<List<Int?>> = it.a
    }

    nullableColumnDf.implode(dropNA = true) { a }.let {
        val col: DataColumn<List<Int>> = it.a
    }

    val columnGroupDf = dataFrameOf(
        "a" to columnOf(
            "a1" to columnOf(1, 2, null, 3),
            "a2" to columnOf(1, 2, null, 3),
        ),
        "b" to columnOf("a", "a", "b", "b")
    )

    columnGroupDf.implode { a }.let {
        // FrameColumn
        val col: DataColumn<Int?> = it.a[0].a1
        val col1: DataColumn<Int?> = it.a[0].a2
    }

    val df = dataFrameOf(
        "a" to columnOf(1, 2, 3, 4),
        "b" to columnOf("a", "b", "c", "d"),
        "c" to columnOf("a", "b", "c", null),
    )

    df.implode(dropNA = true).let {
        val l1: List<Int> = it.a
        val l2: List<String> = it.b
        val l3: List<String> = it.c
    }

    df.implode().let {
        val l1: List<Int> = it.a
        val l2: List<String> = it.b
        val l3: List<String?> = it.c
    }

    val frameColDf = dataFrameOf(
        "frameCol" to columnOf(dataFrameOf("a" to columnOf(1, 2, 3))),
        "col" to columnOf(1),
    )

    frameColDf.implode { frameCol }.let {
        val schema = it.compileTimeSchema()
        assert(schema.column("frameCol").type == typeOf<List<DataFrame<*>>>())
    }
    return "OK"
}
