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
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.Method
import java.io.File

class JvmAbiOutputExtension(
    private val outputPath: File,
    private val abiClassInfos: Map<String, AbiClassInfo>,
    private val messageCollector: MessageCollector,
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
                val outputFile = outputFiles.get("$internalName.class")
                val abiInfo = abiClassInfos.getValue(internalName)
                when {
                    // Note that outputFile may be null, e.g., for empty $DefaultImpls classes in the JVM backend.
                    outputFile == null ->
                        null

                    abiInfo is AbiClassInfo.Public ->
                        // Copy verbatim
                        outputFile

                    else -> /* abiInfo is AbiClassInfo.Stripped */ {
                        val methodInfo = (abiInfo as AbiClassInfo.Stripped).methodInfo
                        val writer = ClassWriter(0)
                        ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.API_VERSION, writer) {
                            // Strip private fields.
                            override fun visitField(
                                access: Int,
                                name: String?,
                                descriptor: String?,
                                signature: String?,
                                value: Any?
                            ): FieldVisitor? {
                                if (access and Opcodes.ACC_PRIVATE != 0)
                                    return null
                                return super.visitField(access, name, descriptor, signature, value)
                            }

                            override fun visitMethod(
                                access: Int,
                                name: String,
                                descriptor: String,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor? {
                                val info = methodInfo[Method(name, descriptor)]
                                    ?: return null

                                val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)

                                if (info == AbiMethodInfo.KEEP || access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) != 0)
                                    return visitor

                                return object : MethodVisitor(Opcodes.API_VERSION, visitor) {
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
                            }

                            // Strip source debug extensions if there are no inline functions.
                            override fun visitSource(source: String?, debug: String?) {
                                // TODO Normalize and strip unused line numbers from SourceDebugExtensions
                                if (methodInfo.values.any { it == AbiMethodInfo.KEEP })
                                    super.visitSource(source, debug)
                                else
                                    super.visitSource(source, null)
                            }

                            // Remove inner classes which are not present in the abi jar.
                            override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
                                if (name in abiClassInfos.keys) {
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
