/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.wasi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.utils.readUnsignedLeb128
import org.jetbrains.kotlin.utils.writeUnsignedLeb128
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * TODO(remove after bootstrap): The export name of the unit test runner in WASI test binaries of the bootstrap compiler.
 *  The current compiler uses [WASI_UNIT_TESTS_EXPORT_NAME].
 */
internal const val BOOTSTRAP_UNIT_TESTS_EXPORT_NAME = "startUnitTests"

/** The export name of the unit test runner in WASI test binaries of the current compiler. It is a WIT label. */
internal const val WASI_UNIT_TESTS_EXPORT_NAME = "startunittests"

/**
 * TODO(remove after bootstrap): Turns a WASI test binary of the bootstrap compiler into a WASI 0.2 component, with the
 *  same steps as `WasiComponentizer` in `wasm/wasm.tests`.
 *
 *  [BOOTSTRAP_UNIT_TESTS_EXPORT_NAME] isn't a WIT label, so the task first adds the alias [WASI_UNIT_TESTS_EXPORT_NAME].
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class ComponentizeWasiTestBinary : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val coreModule: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stdlibWit: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val wasmTools: RegularFileProperty

    @get:OutputFile
    abstract val component: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun componentize() {
        val inputModule = coreModule.get().asFile
        val workDir = temporaryDir.apply {
            deleteRecursively()
            mkdirs()
        }

        val aliased = workDir.resolve("aliased").resolve(inputModule.name).apply {
            parentFile.mkdirs()
            writeBytes(addFunctionExportAlias(inputModule.readBytes(), BOOTSTRAP_UNIT_TESTS_EXPORT_NAME, WASI_UNIT_TESTS_EXPORT_NAME))
        }

        val entryPointWit = workDir.resolve("entry-point").apply {
            mkdirs()
            resolve("$WASI_UNIT_TESTS_EXPORT_NAME.wit").writeText(
                """
                package kotlin-test:wasi-entry-points@1.0.0;

                world $WASI_UNIT_TESTS_EXPORT_NAME {
                    export $WASI_UNIT_TESTS_EXPORT_NAME: func();
                }
                """.trimIndent()
            )
        }

        val withStdlibWorld = workDir.resolve("embed/0/${inputModule.name}")
        withStdlibWorld.parentFile.mkdirs()
        wasmTools(
            "component", "embed", stdlibWit.get().asFile.absolutePath, aliased.absolutePath,
            "--world", "kotlin-stdlib:wasip2/wasip2-imports",
            "-o", withStdlibWorld.absolutePath,
        )
        val withEntryPointWorld = workDir.resolve("embed/1/${inputModule.name}")
        withEntryPointWorld.parentFile.mkdirs()
        wasmTools(
            "component", "embed", entryPointWit.absolutePath, withStdlibWorld.absolutePath,
            "-o", withEntryPointWorld.absolutePath,
        )
        wasmTools("component", "new", withEntryPointWorld.absolutePath, "-o", component.get().asFile.absolutePath)
    }

    private fun wasmTools(vararg arguments: String) {
        execOperations.exec {
            executable = wasmTools.get().asFile.absolutePath
            args = arguments.toList()
        }
    }
}

private const val EXPORT_SECTION_ID = 7
private const val FUNCTION_EXPORT_KIND = 0

/**
 * Returns [module] with the additional export [alias] of the function that [module] exports as [name].
 *
 * No part of a module refers to an export by its position, so only the export section changes. The offsets of the later
 * sections change too. Only source maps use these offsets, and the WASI test binaries have no source maps.
 */
internal fun addFunctionExportAlias(module: ByteArray, name: String, alias: String): ByteArray {
    val result = ByteArrayOutputStream()
    result.write(module, 0, 8) // magic number and version
    var position = 8

    fun readU32(): Int = readUnsignedLeb128({ module[position++] }).toInt()
    fun ByteArrayOutputStream.writeU32(value: Int) = writeUnsignedLeb128(value.toUInt()) { write(it.toInt()) }

    var aliased = false
    while (position < module.size) {
        val sectionStart = position
        val sectionId = module[position++].toInt() and 0xff
        val sectionSize = readU32()
        val payloadEnd = position + sectionSize
        if (sectionId != EXPORT_SECTION_ID) {
            result.write(module, sectionStart, payloadEnd - sectionStart)
        } else {
            val exportCount = readU32()
            val entriesStart = position
            var functionIndex: Int? = null
            repeat(exportCount) {
                val nameLength = readU32()
                val exportName = module.decodeToString(position, position + nameLength)
                position += nameLength
                val kind = module[position++].toInt()
                val index = readU32()
                if (kind == FUNCTION_EXPORT_KIND && exportName == name) {
                    functionIndex = index
                }
            }
            val aliasedIndex = checkNotNull(functionIndex) { "The module has no function export `$name`" }

            val payload = ByteArrayOutputStream()
            payload.writeU32(exportCount + 1)
            payload.write(module, entriesStart, payloadEnd - entriesStart)
            val aliasBytes = alias.encodeToByteArray()
            payload.writeU32(aliasBytes.size)
            payload.write(aliasBytes)
            payload.write(FUNCTION_EXPORT_KIND)
            payload.writeU32(aliasedIndex)

            result.write(sectionId)
            result.writeU32(payload.size())
            payload.writeTo(result)
            aliased = true
        }
        position = payloadEnd
    }
    check(aliased) { "The module has no export section" }
    return result.toByteArray()
}
