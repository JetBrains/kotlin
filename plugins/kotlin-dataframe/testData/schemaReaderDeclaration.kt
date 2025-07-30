package org.jetbrains.kotlinx.dataframe.io

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.columns.*
import java.io.File
import kotlin.reflect.KType

public interface DataFrameProvider<T> {
    public val schemaKType: KType
    public fun default(): DataFrame<T>
    public fun read(path: String): DataFrame<T>
}

public interface SchemaReader {
    public fun accepts(path: String): Boolean
    public fun read(path: String): DataFrame<*>
    public fun default(path: String): DataFrame<*> = read(path)
}