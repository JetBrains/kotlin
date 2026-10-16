package org.jetbrains.teamcity

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Properties

object PropertiesFileReader {
    @Throws(IOException::class)
    fun loadPropertiesFromFile(): Properties {
        val filePath = System.getenv(Constants.BUILD_PROPERTIES_FILE_ENV_VAR) ?: return Properties()

        val debugServiceMessage = ServiceMessage(
            name = "message",
            attributes = mutableMapOf(
                "text" to "Reading the Teamcity build properties file '$filePath'",
                "tc:tags" to "tc:internal"
            )
        )
        println(debugServiceMessage.asString())
        val properties = Properties()
        RandomAccessFile(filePath, "r").use { randomAccessFile ->
            val data = ByteArray(randomAccessFile.length().toInt())
            randomAccessFile.readFully(data)
            properties.load(ByteArrayInputStream(data))
        }

        return properties
    }
}
