import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.*
import org.jetbrains.kotlinx.dataframe.plugin.testing.atoms.*

internal interface Schema1 {
    val i: Int
}

@DataSchema(isOpen = false)
interface Schema21 {
    val age: Number?
}

@DataSchema
interface Schema2 {
    val person: DataRow<Schema21>
}

internal fun injectionScope() {
    val df = dataFrameOf("i")(1, 2, 3).cast<Schema1>()
    test(id = "dataFrame_1", call = dataFrame(df))
    val df1: AnyFrame = dataFrameOf("i")(1, 2, 3)
    test(id = "dataFrame_2", call = dataFrame(df1))
    val df2: DataFrame<Schema2> = TODO()
    test(id = "dataFrame_3", call = dataFrame(df2))
}