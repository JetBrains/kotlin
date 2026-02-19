// WITH_SCHEMA_READER

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

@DataSchemaSource
interface MySchema {
    companion <!CONFLICTING_COMPANION_OBJECT_DECLARATION!>object<!>
}