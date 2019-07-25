/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.stats.completion.storage

import com.intellij.openapi.util.text.StringUtil
import com.intellij.stats.logger.LineStorage
import com.intellij.stats.logger.LogFileManager
import com.intellij.stats.storage.UniqueFilesProvider
import com.intellij.testFramework.PlatformTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File


class FilesProviderTest {
    private lateinit var provider: UniqueFilesProvider

    @Before
    fun setUp() {
        provider = UniqueFilesProvider("chunk", ".", "logs-data")
        provider.getStatsDataDirectory().deleteRecursively()
    }

    @After
    fun tearDown() {
        provider.getStatsDataDirectory().deleteRecursively()
    }
    
    @Test
    fun `test three new files created`() {
        provider.getUniqueFile().createNewFile()
        provider.getUniqueFile().createNewFile()
        provider.getUniqueFile().createNewFile()
        
        val createdFiles = provider.getDataFiles().count()
        
        assertThat(createdFiles).isEqualTo(3)
    }
}

class AsciiMessageStorageTest {
    private lateinit var storage: LineStorage
    private lateinit var tmpFile: File

    @Before
    fun setUp() {
        storage = LineStorage()
        tmpFile = File("tmp_test")
        tmpFile.delete()
    }

    @After
    fun tearDown() {
        tmpFile.delete()
    }

    @Test
    fun `test size with new lines`() {
        val line = "text"
        storage.appendLine(line)
        assertThat(storage.sizeWithNewLine("")).isEqualTo(line.length + 2 * System.lineSeparator().length)
    }
    
    @Test
    fun `test size is same as file size`() {
        val line = "text"
        storage.appendLine(line)
        storage.appendLine(line)

        val expectedSize = 2 * (line.length + System.lineSeparator().length)
        assertThat(storage.size).isEqualTo(expectedSize)

        storage.dump(tmpFile)
        assertThat(tmpFile.length()).isEqualTo(expectedSize.toLong())
    }
}

class FileLoggerTest : PlatformTestCase() {
    private lateinit var fileLogger: LogFileManager
    private lateinit var filesProvider: UniqueFilesProvider
    private lateinit var tempDirectory: File

    override fun setUp() {
        super.setUp()
        tempDirectory = createTempDirectory()
        filesProvider = UniqueFilesProvider("chunk", tempDirectory.absolutePath, "logs-data")
        fileLogger = LogFileManager(filesProvider)
    }

    override fun tearDown() {
        tempDirectory.deleteRecursively()
        super.tearDown()
    }

    fun `test chunk is around 256Kb`() {
        val bytesToWrite = 1024 * 200
        val text = StringUtil.repeat("c", bytesToWrite)
        fileLogger.println(text)
        fileLogger.flush()

        val chunks = filesProvider.getDataFiles()
        assertThat(chunks).hasSize(1)

        val fileLength = chunks.first().length()
        assertThat(fileLength).isLessThan(256 * 1024)
        assertThat(fileLength).isGreaterThan(200 * 1024)
    }
    
    fun `test multiple chunks`() {
        writeKb(1024)

        val files = filesProvider.getDataFiles()
        val fileIndexes = files.map { it.name.substringAfter('_').toInt() }
        assertThat(files.isNotEmpty()).isTrue()
        assertThat(fileIndexes).isEqualTo((0 until files.size).toList())
    }

    fun `test delete old stuff`() {
        writeKb(4096)

        val files = filesProvider.getDataFiles()
        val totalSizeAfterCleanup = files.fold(0L) { total, file -> total + file.length() }
        assertThat(totalSizeAfterCleanup < 2 * 1024 * 1024).isTrue()

        val firstAfter = files
          .map { it.name.substringAfter('_').toInt() }
          .sorted()
          .first()
        
        assertThat(firstAfter).isGreaterThan(0)
    }

    private fun writeKb(kb: Int) {
        val lineLength = System.lineSeparator().length
        (0..kb * 1024 / lineLength).forEach {
            fileLogger.println("")
        }

        fileLogger.flush()
    }
}