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
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.visitWithSplitting
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper
import org.jetbrains.org.objectweb.asm.commons.Remapper
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.File
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature

class JvmAbiOutputExtension(
    private val outputPath: File,
    private val abiClassInfoBuilder: () -> Map<String, AbiClassInfo>,
    private val messageCollector: MessageCollector,
    private val removeDebugInfo: Boolean,
    private val removeDataClassCopyIfConstructorIsPrivate: Boolean,
    private val preserveDeclarationOrder: Boolean,
    private val treatInternalAsPrivate: Boolean,
) : ClassFileFactoryFinalizerExtension {
    override fun finalizeClassFactory(factory: ClassFileFactory) {
        // We need to wait until the end to produce any output in order to strip classes
        // from the InnerClasses attributes.
        val outputFiles =
            AbiOutputFiles(
                abiClassInfoBuilder(),
                factory,
                removeDebugInfo,
                removeDataClassCopyIfConstructorIsPrivate,
                preserveDeclarationOrder,
                treatInternalAsPrivate,
            )
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

    private class InnerClassInfo(val name: String, val outerName: String?, val innerName: String?, val access: Int)

    private class AbiOutputFiles(
        val abiClassInfos: Map<String, AbiClassInfo>,
        val outputFiles: OutputFileCollection,
        val removeDebugInfo: Boolean,
        val removeCopyAlongWithConstructor: Boolean,
        val preserveDeclarationOrder: Boolean,
        val treatInternalAsPrivate: Boolean,
    ) : OutputFileCollection {
        private val classesToBeDeleted = abiClassInfos.mapNotNullTo(mutableSetOf()) { (className, action) ->
            className.takeIf { action == AbiClassInfo.Deleted }
        }

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
                    is AbiClassInfo.Deleted -> null
                    is AbiClassInfo.Public -> outputFile // Copy verbatim
                    is AbiClassInfo.Stripped -> {
                        val prune = abiInfo.prune
                        val memberInfo = abiInfo.memberInfo
                        val innerClassesToKeep = mutableSetOf<String>()

                        var sourceFile: String? = null
                        var sourceMap: SourceMapCopier? = null
                        var sourceMapAnnotationPresent = false

                        val writer = ClassWriter(0)
                        val remapper = ClassRemapper(writer, object : Remapper() {
                            override fun map(internalName: String): String =
                                internalName.also { innerClassesToKeep.add(it) }
                        })
                        val parsingOptions = if (removeDebugInfo) ClassReader.SKIP_DEBUG else 0
                        ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.API_VERSION, remapper) {
                            private val keptFields = mutableListOf<FieldNode>()
                            private val keptMethods = mutableListOf<MethodNode>()
                            private val innerClassInfos = mutableMapOf<String, InnerClassInfo>()

                            override fun visitSource(source: String?, debug: String?) {
                                sourceFile = source
                                sourceMap = debug.takeIf { !prune }?.let(SMAPParser::parseOrNull)
                                    ?.let { SourceMapCopier(SourceMapper(sourceFile, it), it) }
                            }

                            // Strip private fields.
                            override fun visitField(
                                access: Int,
                                name: String,
                                descriptor: String,
                                signature: String?,
                                value: Any?,
                            ): FieldVisitor? {
                                if (prune || memberInfo[JvmFieldSignature(name, descriptor)] != AbiMethodInfo.KEEP)
                                    return null
                                return FieldNode(access, name, descriptor, signature, value).also {
                                    keptFields += it
                                }
                            }

                            override fun visitMethod(
                                access: Int,
                                name: String,
                                descriptor: String,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor? {
                                if (prune) return null
                                val info = memberInfo[JvmMethodSignature(name, descriptor)] ?: return null

                                val node = MethodNode(access, name, descriptor, signature, exceptions).also {
                                    keptMethods += it
                                }

                                return if (info != AbiMethodInfo.KEEP && access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) == 0)
                                    BodyStrippingMethodVisitor(node)
                                else
                                    node
                            }

                            // Remove inner classes which are not present in the abi jar.
                            override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                                // `visitInnerClass` is called before `visitField`/`visitMethod`, so we don't know
                                // which types are referenced by kept methods yet.
                                innerClassInfos[name] = InnerClassInfo(name, outerName, innerName, access)
                            }

                            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                                if (descriptor == JvmAnnotationNames.SOURCE_DEBUG_EXTENSION_DESC) {
                                    sourceMapAnnotationPresent = true
                                    return null
                                }

                                val delegate = super.visitAnnotation(descriptor, visible)
                                if (descriptor != JvmAnnotationNames.METADATA_DESC)
                                    return delegate
                                // Strip private declarations from the Kotlin Metadata annotation.
                                return abiMetadataProcessor(
                                    delegate,
                                    removeCopyAlongWithConstructor,
                                    preserveDeclarationOrder,
                                    classesToBeDeleted,
                                    prune,
                                    treatInternalAsPrivate,
                                )
                            }

                            override fun visitEnd() {
                                if (!preserveDeclarationOrder) {
                                    // Output class members in sorted order so that changes in original ordering don't affect the ABI JAR.
                                    keptFields.sortWith(compareBy(FieldNode::name, FieldNode::desc))
                                    keptMethods.sortWith(compareBy(MethodNode::name, MethodNode::desc))
                                }

                                for (field in keptFields) {
                                    field.accept(cv)
                                }

                                for (method in keptMethods) {
                                    val mv = with(method) { cv.visitMethod(access, name, desc, signature, exceptions?.toTypedArray()) }
                                    // Mapping the line numbers should only be done *after* sorting methods, as otherwise the order
                                    // of inline methods may be visible in their synthetic line numbers.
                                    method.accept(sourceMap?.let { SourceMapCopyingMethodVisitor(it, mv) } ?: mv)
                                }

                                val sourceMapText = sourceMap?.parent?.takeIf { !it.isTrivial }
                                    ?.let { SMAPBuilder.build(it.resultMappings, backwardsCompatibleSyntax = false) }
                                // This is technically not the right way to use `ClassVisitor` (`visitSource` should be called before
                                // `visitMethod` and such), but `ClassWriter` doesn't care, and we're a bit constrained here (see above).
                                cv.visitSource(sourceFile, sourceMapText)
                                if (sourceMapAnnotationPresent && sourceMapText != null) {
                                    val av = cv.visitAnnotation(JvmAnnotationNames.SOURCE_DEBUG_EXTENSION_DESC, false)
                                    av.visitWithSplitting("value", sourceMapText)
                                    av.visitEnd()
                                }

                                innerClassesToKeep.addInnerClasses(innerClassInfos, internalName)
                                innerClassesToKeep.addOuterClasses(innerClassInfos)
                                for (name in innerClassesToKeep.sorted()) {
                                    innerClassInfos[name]?.let { cv.visitInnerClass(it.name, it.outerName, it.innerName, it.access) }
                                }

                                super.visitEnd()
                            }
                        }, parsingOptions)

                        SimpleOutputBinaryFile(outputFile.sourceFiles, outputFile.relativePath, writer.toByteArray())
                    }
                }
            }

            return metadata + classFiles
        }

        // Outer class infos for a class and all classes transitively nested in it (that are public ABI)
        // should be kept in its own class file even if the classes are otherwise unused.
        private fun MutableSet<String>.addInnerClasses(innerClassInfos: Map<String, InnerClassInfo>, internalName: String) {
            val innerClassesByOuterName = innerClassInfos.values.groupBy { it.outerName }
            val stack = mutableListOf(internalName)
            while (stack.isNotEmpty()) {
                val next = stack.removeLast()
                add(next)
                // Classes form a tree by nesting, so none of the children have been visited yet.
                innerClassesByOuterName[next]?.mapNotNullTo(stack) { info ->
                    info.name.takeUnless { abiClassInfos[it] == AbiClassInfo.Deleted }
                }
            }
        }

        // For every class A.B, if its outer class info is kept then so should be A's.
        private fun MutableSet<String>.addOuterClasses(innerClassInfos: Map<String, InnerClassInfo>) {
            for (name in toList()) {
                var info = innerClassInfos[name]
                while (info != null) {
                    info = info.outerName?.takeIf(::add)?.let(innerClassInfos::get)
                }
            }
        }
    }
}

private class BodyStrippingMethodVisitor(visitor: MethodVisitor) : MethodVisitor(Opcodes.API_VERSION, visitor) {
    override fun visitCode() {
        with(mv) {
            visitCode()
            visitInsn(Opcodes.ACONST_NULL)
            visitInsn(Opcodes.ATHROW)
            visitMaxs(0, 0)
            visitEnd()
        }
        // Only instructions, frames, try-catch, and locals follow after `visitCode`.
        mv = null
    }
}
