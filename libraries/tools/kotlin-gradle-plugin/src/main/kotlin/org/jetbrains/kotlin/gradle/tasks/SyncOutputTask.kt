/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import com.intellij.openapi.util.io.FileUtil.isAncestor
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import kotlin.properties.Delegates

/**
 * Copies kotlin classes to java output directory.
 * No other way to get our classes into apk for android until API is provided https://code.google.com/p/android/issues/detail?id=200714
 *
 * Utilises gradle for tracking input/output changes.
 * 0. Gradle takes snapshots of annotated properties after compilation.
 * 1. kotlinOutputDir as input files. These files will be copied.
 * Copying will be done incrementally unless output classes are wiped out
 * (by IncrementalJavaCompilationSafeguard task from android gradle plugin for example).
 * 2. kotlinClassesInJavaOutputDir as output files -- copied classes in java output dir.
 * When they are changed or removed gradle tells us that incremental build is not possible (inputs.isIncremental is false).
 * 3. We do non-incremental sync in the following way:
 *    a. previously copied files are deleted (to do so we save the list of copied files after copying);
 *    b. all kotlin files are copied to java output dir.
 *
 * It is possible that some file was converted from kotlin to java.
 * In this case gradle will tell us to make non-incremental sync.
 * To workaround this scenario we also save timestamp of copied class.
 * On step (a) of sync process we do not delete the class in java output dir
 * if it's timestamp now is newer than when we copied it
 * (assuming it was modified by javac when class was converted to java).
 */
internal open class SyncOutputTask : DefaultTask() {
    @get:InputFiles
    var kotlinOutputDir: File by Delegates.notNull()

    @get:InputFiles
    var kaptClassesDir: File by Delegates.notNull()

    // Marked as input to make Gradle fall back to non-incremental build when it changes.
    @get:Input
    private val classesDirs: List<File>
            get() = listOf(kotlinOutputDir, kaptClassesDir).filter(File::exists)

    var javaOutputDir: File by Delegates.notNull()
    var kotlinTask: KotlinCompile by Delegates.notNull()

    // OutputDirectory needed for task to be incremental
    @get:OutputDirectory
    val workingDir: File by lazy {
        File(kotlinTask.taskBuildDirectory, "sync")
    }
    private val timestampsFile: File by lazy {
        File(workingDir, TIMESTAMP_FILE_NAME)
    }
    private val timestamps: MutableMap<File, Long> by lazy {
        readTimestamps(timestampsFile, javaOutputDir)
    }

    init {
        outputs.upToDateWhen {
            for ((file, ts) in timestamps) {
                if (!file.exists()) {
                    logger.kotlinDebug { "$file does not exist" }
                    return@upToDateWhen false
                }

                if (file.lastModified() != ts) {
                    logger.kotlinDebug { "$file ts is different" }
                    return@upToDateWhen false
                }
            }

            return@upToDateWhen true
        }
    }

    @Suppress("unused")
    @TaskAction
    fun execute(inputs: IncrementalTaskInputs): Unit {
        val sourceDirs = classesDirs.joinToString()
        if (inputs.isIncremental) {
            logger.kotlinDebug { "Incremental copying files from $sourceDirs to $javaOutputDir" }
            inputs.outOfDate { processIncrementally(it) }
            inputs.removed { processIncrementally(it) }
        }
        else {
            logger.kotlinDebug { "Non-incremental copying files from $sourceDirs to $javaOutputDir" }
            processNonIncrementally()
        }

        saveTimestamps(timestampsFile, timestamps, javaOutputDir)
    }

    private fun processNonIncrementally() {
        for ((file, timestamp) in timestamps) {
            // wipe all files written by us
            // do not remove files converted to java (newer timestamp)
            if (file.lastModified() == timestamp) {
                file.delete()
            }
        }

        timestampsFile.delete()
        timestamps.clear()

        for (dir in classesDirs) {
            dir.walkTopDown().forEach {
                copy(it, it.siblingInJavaDir(baseDir = dir))
            }
        }
    }

    private fun processIncrementally(input: InputFileDetails) {
        val fileInKotlinDir = input.file
        val fileInJavaDir = fileInKotlinDir.siblingInJavaDir()

        if (input.isRemoved) {
            // file was removed in kotlin dir, remove from java as well
            remove(fileInJavaDir)
        }
        else {
            // copy modified or added file from kotlin to java
            copy(fileInKotlinDir, fileInJavaDir)
        }
    }

    private fun remove(fileInJavaDir: File) {
        if (!fileInJavaDir.isFile) return

        fileInJavaDir.delete()
        timestamps.remove(fileInJavaDir)

        logger.kotlinDebug {
            "Removed kotlin class ${fileInJavaDir.relativeTo(javaOutputDir).path} from $javaOutputDir"
        }
    }

    private fun copy(fileInKotlinDir: File, fileInJavaDir: File) {
        if (!fileInKotlinDir.isFile) return

        fileInJavaDir.parentFile.mkdirs()
        fileInKotlinDir.copyTo(fileInJavaDir, overwrite = true)

        timestamps[fileInJavaDir] = fileInJavaDir.lastModified()

        logger.kotlinDebug {
            "Copied kotlin class ${fileInKotlinDir.relativeTo(kotlinOutputDir).path} from $kotlinOutputDir to $javaOutputDir"
        }
    }

    private fun File.siblingInJavaDir(baseDir: File? = null): File {
        val base = baseDir ?: classesDirs.find { isAncestor(it, this, true) }!!
        return File(javaOutputDir, this.relativeTo(base).path)
    }
}

private val TIMESTAMP_FILE_NAME = "kotlin-files-in-java-timestamps.bin"

private fun readTimestamps(tsFile: File, filesBaseDir: File): MutableMap<File, Long> {
    val result = HashMap<File, Long>()
    if (!tsFile.isFile) return result

    ObjectInputStream(tsFile.inputStream()).use { input ->
        val size = input.readInt()

        repeat(size) {
            val path = input.readUTF()
            val timestamp = input.readLong()
            result[File(filesBaseDir, path)] = timestamp
        }
    }

    return result
}

private fun saveTimestamps(snapshotFile: File, timestamps: Map<File, Long>, filesBaseDir: File) {
    if (!snapshotFile.exists()) {
        snapshotFile.parentFile.mkdirs()
        snapshotFile.createNewFile()
    }
    else {
        snapshotFile.delete()
    }

    ObjectOutputStream(snapshotFile.outputStream()).use { out ->
        out.writeInt(timestamps.size)

        for ((file, timestamp) in timestamps) {
            out.writeUTF(file.relativeTo(filesBaseDir).path)
            out.writeLong(timestamp)
        }
    }
}