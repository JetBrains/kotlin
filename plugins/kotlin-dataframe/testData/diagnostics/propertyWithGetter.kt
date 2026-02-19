import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

class Context {
    val peopleDf: DataFrame<Any?> get() = <!DATAFRAME_PLUGIN_NOT_YET_SUPPORTED_IN_PROPERTY_ACCESSOR!>dataFrameOf<!>("firstName" to columnOf("Alice"))
}