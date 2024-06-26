import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    val tsv = """
            a	b	c
            1	2	3
        """
    val df = DataFrame.readDelimStr(tsv, '\t')
    df.a
    df.b
    df.c
    return "OK"
}
