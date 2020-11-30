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
import org.jetbrains.org.objectweb.asm.tree.*
import java.io.File

class JvmAbiOutputExtension(
    private val outputPath: File,
    private val abiClassInfos: Map<String, AbiClassInfo>,
) : ClassFileFactoryFinalizerExtension {
    override fun finalizeClassFactory(factory: ClassFileFactory) {
        // We need to wait until the end to produce any output in order to strip classes
        // from the InnerClasses attributes.
        val outputFiles = AbiOutputFiles(abiClassInfos, factory)
        if (outputPath.extension == "jar") {
            // We don't include the runtime or main class in interface jars and always reset time stamps.
            CompileEnvironmentUtil.writeToJar(outputPath, false, true, true, null, outputFiles, MessageCollector.NONE)
        } else {
            outputFiles.writeAllTo(outputPath)
        }
    }

    private class AbiOutputFiles(val abiClassInfos: Map<String, AbiClassInfo>, val outputFiles: OutputFileCollection) : OutputFileCollection {
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
                            // We remember all non-inline declarations in the stripped class so that we can
                            // order them deterministically after all inline methods.
                            private val fieldNodes = mutableListOf<FieldNode>()
                            private val methodNodes = mutableListOf<StrippedMethodNode>()

                            // Strip private fields.
                            override fun visitField(access: Int, name: String?, descriptor: String?, signature: String?, value: Any?): FieldVisitor? {
                                if (access and Opcodes.ACC_PRIVATE != 0)
                                    return null
                                return FieldNode(Opcodes.API_VERSION, access, name, descriptor, signature, value).also {
                                    fieldNodes += it
                                }
                            }

                            override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                                val info = methodInfo[Method(name, descriptor)]
                                    ?: return null

                                if (info == AbiMethodInfo.KEEP)
                                    return super.visitMethod(access, name, descriptor, signature, exceptions)

                                return StrippedMethodNode(access, name, descriptor, signature, exceptions).also {
                                    methodNodes += it
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
                                return abiMetadataProcessor(internalName, abiClassInfos.keys, delegate)
                            }

                            override fun visitEnd() {
                                fieldNodes.sortBy { "${it.name}${it.desc}" }
                                methodNodes.sortBy { "${it.name}${it.desc}" }
                                for (field in fieldNodes) {
                                    field.accept(writer)
                                }
                                for (method in methodNodes) {
                                    method.accept(writer)
                                }
                                super.visitEnd()
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

private class StrippedMethodNode(
    val access: Int,
    val name: String,
    val desc: String,
    val signature: String?,
    val exceptions: Array<out String>?,
) : MethodVisitor(Opcodes.API_VERSION) {
    private class MethodAnnotation(
        val visible: Boolean,
        val node: AnnotationNode,
    )

    private class MethodTypeAnnotation(
        val typeRef: Int,
        val typePath: TypePath?,
        val visible: Boolean,
        val node: AnnotationNode,
    )

    private class MethodParameterAnnotation(
        val parameter: Int,
        val visible: Boolean,
        val node: AnnotationNode,
    )

    private val parameters = mutableListOf<ParameterNode>()
    private val annotations = mutableListOf<MethodAnnotation>()
    private val typeAnnotations = mutableListOf<MethodTypeAnnotation>()
    private var visibleAnnotableParameterCount = 0
    private var invisibleAnnotableParameterCount = 0
    private val parameterAnnotations = mutableListOf<MethodParameterAnnotation>()
    private val attributes = mutableListOf<Attribute>()

    override fun visitParameter(name: String?, access: Int) {
        parameters.add(ParameterNode(name, access))
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor =
        AnnotationNode(Opcodes.API_VERSION, descriptor).also {
            annotations.add(MethodAnnotation(visible, it))
        }

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor =
        AnnotationNode(Opcodes.API_VERSION, descriptor).also {
            typeAnnotations.add(MethodTypeAnnotation(typeRef, typePath, visible, it))
        }

    override fun visitAttribute(attribute: Attribute) {
        attributes.add(attribute)
    }

    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        if (visible) {
            visibleAnnotableParameterCount = parameterCount
        } else {
            invisibleAnnotableParameterCount = parameterCount
        }
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor =
        AnnotationNode(Opcodes.API_VERSION, descriptor).also {
            parameterAnnotations.add(MethodParameterAnnotation(parameter, visible, it))
        }

    fun accept(classVisitor: ClassVisitor) {
        val visitor = classVisitor.visitMethod(access, name, desc, signature, exceptions)

        for (parameter in parameters) {
            visitor.visitParameter(parameter.name, parameter.access)
        }

        for (annotation in annotations) {
            visitor.visitAnnotation(annotation.node.desc, annotation.visible)?.let {
                annotation.node.accept(it)
            }
        }

        for (typeAnnotation in typeAnnotations) {
            visitor.visitTypeAnnotation(
                typeAnnotation.typeRef,
                typeAnnotation.typePath,
                typeAnnotation.node.desc,
                typeAnnotation.visible,
            )?.let {
                typeAnnotation.node.accept(it)
            }
        }

        visitor.visitAnnotableParameterCount(visibleAnnotableParameterCount, true)
        visitor.visitAnnotableParameterCount(invisibleAnnotableParameterCount, false)
        for (parameterAnnotation in parameterAnnotations) {
            visitor.visitParameterAnnotation(
                parameterAnnotation.parameter,
                parameterAnnotation.node.desc,
                parameterAnnotation.visible
            )?.let {
                parameterAnnotation.node.accept(it)
            }
        }

        for (attribute in attributes) {
            visitor.visitAttribute(attribute)
        }

        if (access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) == 0) {
            visitor.visitCode()
            visitor.visitInsn(Opcodes.ACONST_NULL)
            visitor.visitInsn(Opcodes.ATHROW)
            visitor.visitMaxs(0, parameters.size)
        }
        visitor.visitEnd()
    }
}
