package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments
import org.jetbrains.kotlinx.dataframe.plugin.impl.Present
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readDelimStr
import org.jetbrains.kotlinx.dataframe.io.readJson
import org.jetbrains.kotlinx.dataframe.io.readJsonStr
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

    override fun Arguments.interpret(): PluginDataFrameSchema {
        val file = resolveFile(resolutionPath, fileOrUrl)
        val df = if (file != null && file.exists()) {
            DataFrame.readCSV(file)
        } else {
            DataFrame.readCSV(fileOrUrl)
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

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.readDelimStr(text).schema().toPluginDataFrameSchema()
    }
}

internal class ReadJsonStr : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.text: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.readJsonStr(text).schema().toPluginDataFrameSchema()
    }
}

