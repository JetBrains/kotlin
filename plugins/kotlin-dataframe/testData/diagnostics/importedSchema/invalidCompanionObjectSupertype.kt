// WITH_SCHEMA_READER

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*

@DataSchemaSource
interface MySchema {
    companion <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : <!INVALID_SUPERTYPE!>DataFrameProvider<String><!>
}