package org.jetbrains.kotlinx.dataframe.plugin

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.annotations.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.annotations.Arguments
import org.jetbrains.kotlinx.dataframe.annotations.Present
import org.jetbrains.kotlinx.dataframe.api.readCSVDefault
import org.jetbrains.kotlinx.dataframe.api.readJsonDefault
import org.jetbrains.kotlinx.dataframe.api.schema
import org.jetbrains.kotlinx.dataframe.io.read
import org.jetbrains.kotlinx.dataframe.io.readCSV
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
        val file = resolutionPath?.let {
            try {
                val file = File(it)
                if (file.exists() && file.isDirectory) {
                    File(file, fileOrUrl)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
        val df = if (file != null && file.exists()) {
            DataFrame.readCSV(file)
        } else {
            DataFrame.readCSVDefault(fileOrUrl)
        }
        return df.schema().toPluginDataFrameSchema()
    }
}

internal class ReadJson0 : AbstractInterpreter<PluginDataFrameSchema>() {
    val Arguments.path: String by arg()

    override fun Arguments.interpret(): PluginDataFrameSchema {
        return DataFrame.readJsonDefault(path).schema().toPluginDataFrameSchema()
    }
}
