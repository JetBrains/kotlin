/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputBinaryFile
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.CompileEnvironmentUtil
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.extensions.ClassFileFactoryFinalizerExtension
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

class JvmAbiOutputExtension(
    private val outputPath: File,
    private val abiClassInfos: Map<String, AbiClassInfo>,
    private val messageCollector: MessageCollector,
    private val deleteNonPublicFromAbi: Boolean,
) : ClassFileFactoryFinalizerExtension {
    override fun finalizeClassFactory(factory: ClassFileFactory) {
        // We need to wait until the end to produce any output in order to strip classes
        // from the InnerClasses attributes.
        val outputFiles = AbiOutputFiles(abiClassInfos, factory)
        if (outputPath.extension == "jar") {
            // We don't include the runtime or main class in interface jars and always reset time stamps.
            CompileEnvironmentUtil.writeToJar(
                outputPath,
                false,
                true,
                true,
                null,
                outputFiles,
                messageCollector
            )
        } else {
            outputFiles.writeAllTo(outputPath)
        }
    }

    private class AbiOutputFiles(val abiClassInfos: Map<String, AbiClassInfo>, val outputFiles: OutputFileCollection) :
        OutputFileCollection {
        override fun get(relativePath: String): OutputFile? {
            error("AbiOutputFiles does not implement `get`.")
        }

        override fun asList(): List<OutputFile> {
            val metadata = outputFiles.asList().filter {
                !it.relativePath.endsWith(".class")
            }.sortedBy { it.relativePath }

            val classFiles = abiClassInfos.keys.sorted().mapNotNull { internalName ->
                // Note that outputFile may be null, e.g., for empty $DefaultImpls classes in the JVM backend.
                val outputFile = outputFiles.get("$internalName.class") ?: return@mapNotNull null

                when (val abiInfo = abiClassInfos.getValue(internalName)) {
                    is AbiClassInfo.Delete -> null // Delete the whole class

                    is AbiClassInfo.Keep -> outputFile // Copy verbatim

                    is AbiClassInfo.Strip -> {
                        val memberInfo = abiInfo.memberInfo
                        val writer = ClassWriter(0)
                        ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.API_VERSION, writer) {
                            override fun visitField(
                                access: Int,
                                name: String?,
                                descriptor: String?,
                                signature: String?,
                                value: Any?
                            ): FieldVisitor? {
                                return when (memberInfo[Member(name, descriptor)]) {
                                    AbiMethodInfo.KEEP -> super.visitField(access, name, descriptor, signature, value)
                                    else -> null //remove
                                }
                            }

                            override fun visitMethod(
                                access: Int,
                                name: String,
                                descriptor: String,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor? {
                                return when (memberInfo[Member(name, descriptor)]) {
                                    AbiMethodInfo.KEEP -> super.visitMethod(access, name, descriptor, signature, exceptions)
                                    AbiMethodInfo.STRIP -> StrippingMethodVisitor(
                                        super.visitMethod(access, name, descriptor, signature, exceptions)
                                    )
                                    null -> null //remove
                                }
                            }

                            // Strip source debug extensions if there are no inline functions.
                            override fun visitSource(source: String?, debug: String?) {
                                // TODO Normalize and strip unused line numbers from SourceDebugExtensions
                                if (memberInfo.values.any { it == AbiMethodInfo.KEEP })
                                    super.visitSource(source, debug)
                                else
                                    super.visitSource(source, null)
                            }

                            // Remove inner classes which are not present in the abi jar.
                            override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
                                if (abiClassInfos[name] != AbiClassInfo.Delete) {
                                    super.visitInnerClass(name, outerName, innerName, access)
                                }
                            }

                            // Strip private declarations from the Kotlin Metadata annotation.
                            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
                                val delegate = super.visitAnnotation(descriptor, visible)
                                if (descriptor != JvmAnnotationNames.METADATA_DESC)
                                    return delegate
                                return abiMetadataProcessor(delegate)
                            }
                        }, 0)
                        SimpleOutputBinaryFile(outputFile.sourceFiles, outputFile.relativePath, writer.toByteArray())
                    }
                }
            }

            return metadata + classFiles
        }
    }
}

private class StrippingMethodVisitor(visitor: MethodVisitor?) : MethodVisitor(
    Opcodes.API_VERSION,
    visitor
) {
    override fun visitCode() {
        with(mv) {
            visitCode()
            visitInsn(Opcodes.ACONST_NULL)
            visitInsn(Opcodes.ATHROW)
            visitMaxs(0, 0)
            visitEnd()
        }
        // Only instructions and locals follow after `visitCode`.
        mv = null
    }
}

