package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.io.JSON
import org.jetbrains.kotlinx.dataframe.io.JSON.TypeClashTactic.ARRAY_AND_VALUE_COLUMNS
import org.jetbrains.kotlinx.dataframe.io.NameRepairStrategy
import org.jetbrains.kotlinx.dataframe.io.StringColumns
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readDelimStr
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.readJsonStr
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractSchemaModificationInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.IoSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.deserializeToPluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.string
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.toPluginDataFrameSchema
import java.io.File

internal class Read0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.path by string()
    val Arguments.header: List<String> by arg(defaultValue = Present(listOf()))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.read(path).schema().toPluginDataFrameSchema()
    }
}

internal class ReadCSV0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.fileOrUrl: String by arg()
    val Arguments.delimiter: Char by arg(defaultValue = Present(','))
    val Arguments.skipLines: Int by arg(defaultValue = Present(0))
    val Arguments.readLines: Int? by arg(defaultValue = Present(null))
    val Arguments.duplicate: Boolean by arg(defaultValue = Present(true))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val file = resolveFile(resolutionPath, fileOrUrl)
        val df = if (file != null && file.exists()) {
            DataFrame.readCSV(file, delimiter, skipLines = skipLines, readLines = readLines, duplicate = duplicate)
        } else {
            DataFrame.readCSV(fileOrUrl, delimiter, skipLines = skipLines, readLines = readLines, duplicate = duplicate)
        }
        return df.schema().toPluginDataFrameSchema()
    }
}

internal class ReadJson0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.path: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val schema = schemasDirectory?.let {
            try {
                val text = File(it, "schemas.json").readText()
                val json = Json.decodeFromString<List<IoSchema>>(text)
                val map = json.associate { it.argument to it.schema }
                val schema = map[path]?.deserializeToPluginDataFrameSchema()
                schema
            } catch (e: Exception) {
                null
            }
        }
        if (schema != null) return schema
        val df = readJson(resolutionPath, path)
        return df.schema().toPluginDataFrameSchema()
    }
}

fun readJson(resolutionPath: String?, path: String): DataFrame<Any?> {
    val file = resolveFile(resolutionPath, path)
    val df = if (file != null && file.exists()) {
        DataFrame.readJson(file)
    } else {
        DataFrame.readJson(path)
    }
    return df
}

private fun resolveFile(resolutionPath: String?, path: String): File? {
    return  resolutionPath?.let {
        try {
            val file = File(it)
            if (file.exists() && file.isDirectory) {
                File(file, path)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

internal class ReadDelimStr : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.text: String by arg()
    val Arguments.delimiter: Char by arg(defaultValue = Present(','))
    val Arguments.skipLines: Int by arg(defaultValue = Present(0))
    val Arguments.readLines: Int? by arg(defaultValue = Present(null))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.readDelimStr(text, delimiter, skipLines = skipLines, readLines = readLines).schema().toPluginDataFrameSchema()
    }
}

internal class ReadJsonStr : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.text: String by arg()
    val Arguments.typeClashTactic: JSON.TypeClashTactic by arg(defaultValue = Present(ARRAY_AND_VALUE_COLUMNS))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.readJsonStr(text, typeClashTactic = typeClashTactic).schema().toPluginDataFrameSchema()
    }
}

internal class ReadExcel : AbstractSchemaModificationInterpreter() {
    val Arguments.fileOrUrl: String by arg()
    val Arguments.sheetName: String? by arg(defaultValue = Present(null))
    val Arguments.skipRows: Int by arg(defaultValue = Present(0))
    val Arguments.columns: String? by arg(defaultValue = Present(null))
    val Arguments.stringColumns: StringColumns? by arg(defaultValue = Present(null))
    val Arguments.rowsCount: Int? by arg(defaultValue = Present(null))
    val Arguments.nameRepairStrategy: NameRepairStrategy by arg(defaultValue = Present(NameRepairStrategy.CHECK_UNIQUE))

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val df = DataFrame.readExcel(fileOrUrl, sheetName, skipRows, columns, stringColumns, rowsCount, nameRepairStrategy)
        return df.schema().toPluginDataFrameSchema()
    }
}

internal class StringColumnsConstructor : AbstractInterpreter<StringColumns>() {
    val Arguments.range: String by arg()

    override fun Arguments.interpret(): StringColumns {
        return StringColumns(range)
    }
}
