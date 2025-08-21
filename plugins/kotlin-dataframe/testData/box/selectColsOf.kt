import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

@DataSchema
interface JoinLeaf {
    val something: Int
    val somethingElse: String
}

@DataSchema
interface Join2 {
    val c: DataRow<JoinLeaf>
}

fun selectionDsl(df: DataFrame<Join2>) {
    df.ungroup { c }.select { colsOf<String>() }.somethingElse
}

fun box(): String {
    return "OK"
}
