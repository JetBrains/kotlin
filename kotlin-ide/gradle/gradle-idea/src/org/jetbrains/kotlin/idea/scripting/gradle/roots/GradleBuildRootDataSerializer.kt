/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import org.jetbrains.kotlin.idea.core.util.readNullable
import org.jetbrains.kotlin.idea.core.util.readString
import org.jetbrains.kotlin.idea.core.util.writeNullable
import org.jetbrains.kotlin.idea.core.util.writeString
import org.jetbrains.kotlin.idea.scripting.gradle.GradleKotlinScriptConfigurationInputs
import org.jetbrains.kotlin.idea.scripting.gradle.LastModifiedFiles
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput

internal object GradleBuildRootDataSerializer {
    private val attribute = FileAttribute("kotlin-dsl-script-models", 5, false)

    fun read(buildRoot: VirtualFile): GradleBuildRootData? {
        return attribute.readAttribute(buildRoot)?.use {
            it.readNullable {
                readKotlinDslScriptModels(it, buildRoot.path)
            }
        }
    }

    fun write(buildRoot: VirtualFile, data: GradleBuildRootData?) {
        attribute.writeAttribute(buildRoot).use {
            it.writeNullable(data) {
                writeKotlinDslScriptModels(this, it)
            }
        }
    }

    fun remove(buildRoot: VirtualFile) {
        write(buildRoot, null)
        LastModifiedFiles.remove(buildRoot)
    }
}

internal fun writeKotlinDslScriptModels(output: DataOutput, data: GradleBuildRootData) {
    val strings = StringsPool.writer(output)
    strings.addStrings(data.projectRoots)
    strings.addStrings(data.templateClasspath)
    data.models.forEach {
        strings.addString(it.file)
        strings.addStrings(it.classPath)
        strings.addStrings(it.sourcePath)
        strings.addStrings(it.imports)
    }
    strings.writeHeader()
    strings.writeStringIds(data.projectRoots)
    strings.writeStringIds(data.templateClasspath)
    output.writeList(data.models) {
        strings.writeStringId(it.file)
        output.writeString(it.inputs.sections)
        output.writeLong(it.inputs.lastModifiedTs)
        strings.writeStringIds(it.classPath)
        strings.writeStringIds(it.sourcePath)
        strings.writeStringIds(it.imports)
    }
}

internal fun readKotlinDslScriptModels(input: DataInputStream, buildRoot: String): GradleBuildRootData {
    val strings = StringsPool.reader(input)

    val projectRoots = strings.readStrings()
    val templateClasspath = strings.readStrings()

    val models = input.readList {
        KotlinDslScriptModel(
            strings.readString(),
            GradleKotlinScriptConfigurationInputs(input.readString(), input.readLong(), buildRoot),
            strings.readStrings(),
            strings.readStrings(),
            strings.readStrings(),
            listOf()
        )
    }

    return GradleBuildRootData(projectRoots, templateClasspath, models)
}

private object StringsPool {
    fun writer(output: DataOutput) = Writer(output)

    class Writer(val output: DataOutput) {
        var freeze = false
        val ids = mutableMapOf<String, Int>()

        fun getStringId(string: String) = ids.getOrPut(string) {
            check(!freeze)
            ids.size
        }

        fun addString(string: String) {
            getStringId(string)
        }

        fun addStrings(list: Collection<String>) {
            list.forEach { addString(it) }
        }

        fun writeHeader() {
            freeze = true

            output.writeInt(ids.size)

            // sort for optimal performance and compression
            ids.keys.sorted().forEachIndexed { index, s ->
                ids[s] = index
                output.writeString(s)
            }
        }

        fun writeStringId(it: String) {
            output.writeInt(getStringId(it))
        }

        fun writeStringIds(strings: Collection<String>) {
            output.writeInt(strings.size)
            strings.forEach {
                writeStringId(it)
            }
        }
    }

    fun reader(input: DataInputStream): Reader {
        val strings = input.readList { input.readString() }
        return Reader(input, strings)
    }

    class Reader(val input: DataInputStream, val strings: List<String>) {
        fun getString(id: Int) = strings[id]

        fun readString() = getString(input.readInt())

        fun readStrings(): List<String> = input.readList { readString() }
    }
}

private inline fun <T> DataOutput.writeList(list: Collection<T>, write: (T) -> Unit) {
    writeInt(list.size)
    list.forEach { write(it) }
}

private inline fun <T> DataInput.readList(read: () -> T): List<T> {
    val n = readInt()
    val result = ArrayList<T>(n)
    repeat(n) {
        result.add(read())
    }
    return result
}
