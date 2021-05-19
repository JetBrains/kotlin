/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.build.instrumentation

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URL
import java.util.ArrayList

internal class InstrumentationArgs(
    val filesToProcess: List<File>,
    val classpathFiles: List<File>,
    var javaHome: File
) {
    companion object {
        fun writeToFile(args: InstrumentationArgs, file: File) {
            ObjectOutputStream(file.outputStream().buffered()).use { out ->
                writeFiles(out, args.filesToProcess)
                writeFiles(out, args.classpathFiles)
                out.writeUTF(args.javaHome.absolutePath)
            }
        }

        fun readFromFile(file: File): InstrumentationArgs =
            ObjectInputStream(file.inputStream().buffered()).use { input ->
                val filesToProcess = readFiles(input)
                val classpathFiles = readFiles(input)
                val javaHome = File(input.readUTF())
                InstrumentationArgs(filesToProcess = filesToProcess, classpathFiles = classpathFiles, javaHome = javaHome)
            }

        private fun writeFiles(out: ObjectOutputStream, files: List<File>) {
            out.writeInt(files.size)
            files.forEach {
                out.writeUTF(it.absolutePath)
            }
        }

        private fun readFiles(input: ObjectInputStream): List<File> {
            val size = input.readInt()
            return ArrayList<File>(size).also { files ->
                repeat(size) {
                    files.add(File(input.readUTF()))
                }
            }
        }
    }
}