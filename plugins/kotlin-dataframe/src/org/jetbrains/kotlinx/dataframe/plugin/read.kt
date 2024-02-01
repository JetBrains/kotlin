package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.readJson
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
        resolutionPath
        val file = resolveFile(fileOrUrl)
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
        val file = resolveFile(path)
        val df = if (file != null && file.exists()) {
            DataFrame.readJson(file)
        } else {
            DataFrame.readJson(path)
        }
        return df.schema().toPluginDataFrameSchema()
    }
}

private fun Arguments.resolveFile(path: String): File? {
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
