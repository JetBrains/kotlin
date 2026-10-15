/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.scripting.compiler.plugin.ReplSnippetConfigurationCodec
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import kotlin.script.experimental.api.ScriptCompilationConfiguration

/**
 * Reads the REPL metadata from a compiled snippet class file without loading the class; `null` if it is not a snippet artifact.
 */
fun readSnippetArtifactMetadata(classFile: File): SnippetArtifactMetadata? =
    nullOnFailure { readSnippetArtifactMetadataOrThrow(classFile) }

/**
 * The snippet's configuration after compiler-side refinement; `null` if absent or not deserializable here.
 */
fun readRefinedCompilationConfiguration(classFile: File): ScriptCompilationConfiguration? =
    nullOnFailure {
        readSnippetArtifactMetadataOrThrow(classFile)?.serializedCompilationConfiguration?.let(ReplSnippetConfigurationCodec::decode)
    }

private inline fun <T : Any> nullOnFailure(block: () -> T?): T? =
    try {
        block()
    } catch (_: Exception) {
        null
    }

private fun readSnippetArtifactMetadataOrThrow(classFile: File): SnippetArtifactMetadata? =
    readReplPluginData(classFile)?.let(SnippetArtifactMetadataCodec::decode)

private const val KOTLIN_METADATA_DESC = "Lkotlin/Metadata;"

private fun readReplPluginData(classFile: File): ByteArray? {
    val [data, strings] = readKotlinMetadataArrays(classFile) ?: return null
    if (data.isEmpty()) return null
    val [nameResolver, classProto] = JvmProtoBufUtil.readClassDataFrom(data, strings)
    return classProto.compilerPluginDataList
        .firstOrNull { nameResolver.getString(it.pluginId) == REPL_SNIPPET_ARTIFACT_PLUGIN_ID }
        ?.data?.toByteArray()
}

private fun readKotlinMetadataArrays(classFile: File): Pair<Array<String>, Array<String>>? {
    val data = mutableListOf<String>()
    val strings = mutableListOf<String>()
    var seen = false
    ClassReader(classFile.readBytes()).accept(
        object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                if (descriptor != KOTLIN_METADATA_DESC) return null
                seen = true
                return object : AnnotationVisitor(Opcodes.API_VERSION) {
                    override fun visitArray(name: String?): AnnotationVisitor? {
                        val target = when (name) {
                            "d1" -> data
                            "d2" -> strings
                            else -> return null
                        }
                        return object : AnnotationVisitor(Opcodes.API_VERSION) {
                            override fun visit(name: String?, value: Any?) {
                                (value as? String)?.let(target::add)
                            }
                        }
                    }
                }
            }
        },
        ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
    )
    if (!seen) return null
    return data.toTypedArray() to strings.toTypedArray()
}
