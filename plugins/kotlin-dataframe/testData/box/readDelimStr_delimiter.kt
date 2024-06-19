import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.*

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
