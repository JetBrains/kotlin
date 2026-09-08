import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@Refine
@Interpretable("DataColumnValueCounts")
public fun <T> DataColumn<T>.myValueCounts(
    sort: Boolean = true,
    ascending: Boolean = false,
    dropNA: Boolean = true,
    resultColumn: String = "count",
) = valueCounts(sort, ascending, dropNA, resultColumn)

fun box(): String {
    val df = dataFrameOf("a" to columnOf(42))
    df.a.myValueCounts().let { res ->
        res.compareSchemas(strict = true)
        val c: DataColumn<Int> = res.a
    }

    val myCol = df.a

    myCol.myValueCounts().let { res ->
        res.compareSchemas(strict = true)
        val c: DataColumn<Int> = res.a
    }

    val dfCount = dataFrameOf("count" to columnOf("1"))
    dfCount.count.myValueCounts().let { res ->
        res.compareSchemas(strict = true)
    }

    dfCount.count.myValueCounts(true, true, true, "count1").let { res ->
        res.compareSchemas(strict = true)
    }

    val dfCount1 = dataFrameOf("count1" to columnOf("1"))
    dfCount1.count1.myValueCounts(resultColumn = "count1").let { res ->
        res.compareSchemas(strict = true)
    }
    return "OK"
}
