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
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.jetbrains.kotlin.bytecode.AnnotationsRemover
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.incremental.md5
import org.jetbrains.org.objectweb.asm.*
import java.io.*
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
open class SyncOutputTask : DefaultTask() {
    @get:InputFiles
    var kotlinOutputDir: File by Delegates.notNull()
    var javaOutputDir: File by Delegates.notNull()
    var kotlinTask: KotlinCompile by Delegates.notNull()
    private val sourceAnnotations: Set<String> by lazy {
        kotlinTask.sourceAnnotationsRegistry?.annotations ?: emptySet()
    }
    private val annotationsRemover by lazy {
        AnnotationsRemover(sourceAnnotations)
    }

    // OutputDirectory needed for task to be incremental
    @get:OutputDirectory
    val workingDir: File by lazy {
        File(kotlinTask.taskBuildDirectory, "sync").apply { mkdirs() }
    }
    private val timestampsFile: File by lazy {
        File(workingDir, TIMESTAMP_FILE_NAME)
    }
    private val timestamps: MutableMap<File, Long> by lazy {
        readTimestamps(timestampsFile)
    }

    @Suppress("unused")
    @get:OutputFiles
    val kotlinClassesInJavaOutputDir: Collection<File>
            get() = timestamps.keys

    @Suppress("unused")
    @TaskAction
    fun execute(inputs: IncrementalTaskInputs): Unit {
        if (inputs.isIncremental) {
            logger.kotlinDebug { "Incremental copying files from $kotlinOutputDir to $javaOutputDir" }
            inputs.outOfDate { processIncrementally(it) }
            inputs.removed { processIncrementally(it) }
        }
        else {
            logger.kotlinDebug { "Non-incremental copying files from $kotlinOutputDir to $javaOutputDir" }
            processNonIncrementally()
        }

        saveTimestamps(timestampsFile, timestamps)
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

        kotlinOutputDir.walkTopDown().forEach {
            copy(it, it.siblingInJavaDir)
        }
    }

    private fun processIncrementally(input: InputFileDetails) {
        val fileInKotlinDir = input.file
        val fileInJavaDir = fileInKotlinDir.siblingInJavaDir

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
        if (sourceAnnotations.isNotEmpty() && fileInKotlinDir.extension.toLowerCase() == "class") {
            logger.kotlinDebug { "Removing source annotations from class: $fileInKotlinDir" }
            annotationsRemover.transformClassFile(fileInKotlinDir, fileInJavaDir)
        }
        else {
            fileInKotlinDir.copyTo(fileInJavaDir, overwrite = true)
        }

        timestamps[fileInJavaDir] = fileInJavaDir.lastModified()

        logger.kotlinDebug {
            "Copied kotlin class ${fileInKotlinDir.relativeTo(kotlinOutputDir).path} from $kotlinOutputDir to $javaOutputDir"
        }
    }

    private val File.siblingInJavaDir: File
            get() = File(javaOutputDir, this.relativeTo(kotlinOutputDir).path)
}

private val TIMESTAMP_FILE_NAME = "kotlin-files-in-java-timestamps.bin"

private fun readTimestamps(file: File): MutableMap<File, Long> {
    val result = HashMap<File, Long>()
    if (!file.isFile) return result

    ObjectInputStream(file.inputStream()).use { input ->
        val size = input.readInt()

        repeat(size) {
            val path = input.readUTF()
            val timestamp = input.readLong()
            result[File(path)] = timestamp
        }
    }

    return result
}

private fun saveTimestamps(snapshotFile: File, timestamps: Map<File, Long>) {
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
            out.writeUTF(file.canonicalPath)
            out.writeLong(timestamp)
        }
    }
}