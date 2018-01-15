/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.*

data class KotlinPosition(val path: String, val isRelativePath: Boolean, val pos: Int)

private typealias LineInfoMap = MutableMap<String, KotlinPosition>

class KaptLineMappingCollector(private val kaptContext: KaptContext<*>) {
    companion object {
        const val KAPT_METADATA_EXTENSION = ".kapt_metadata"
        private const val METADATA_VERSION = 1

        fun parseFileInfo(file: JCTree.JCCompilationUnit): FileInfo {
            val sourceUri = file.sourcefile
                ?.toUri()
                ?.takeIf { it.isAbsolute && !it.isOpaque && it.path != null && it.scheme?.toLowerCase() == "file" } ?: return FileInfo.EMPTY

            val sourceFile = File(sourceUri).takeIf { it.exists() } ?: return FileInfo.EMPTY
            val kaptMetadataFile = File(sourceFile.parentFile, sourceFile.nameWithoutExtension + KAPT_METADATA_EXTENSION)

            if (!kaptMetadataFile.isFile) {
                return FileInfo.EMPTY
            }

            return deserialize(kaptMetadataFile.readBytes())
        }

        private fun deserialize(data: ByteArray): FileInfo {
            val lineInfo: LineInfoMap = mutableMapOf()
            val signatureInfo = mutableMapOf<String, String>()

            val ois = ObjectInputStream(ByteArrayInputStream(data))

            val version = ois.readInt()
            if (version != METADATA_VERSION) {
                return FileInfo.EMPTY
            }

            val lineInfoCount = ois.readInt()
            repeat(lineInfoCount) {
                val fqName = ois.readUTF()
                val path = ois.readUTF()
                val isRelative = ois.readBoolean()
                val pos = ois.readInt()

                lineInfo[fqName] = KotlinPosition(path, isRelative, pos)
            }

            val signatureCount = ois.readInt()
            repeat(signatureCount) {
                val javacSignature = ois.readUTF()
                val methodDesc = ois.readUTF()

                signatureInfo[javacSignature] = methodDesc
            }

            return FileInfo(lineInfo, signatureInfo)
        }

        private fun getJavacSignature(decl: JCTree.JCMethodDecl): String {
            val name = decl.name.toString()
            val params = decl.parameters.joinToString { it.getType().toString() }
            return "$name($params)"
        }
    }

    private val lineInfo: LineInfoMap = mutableMapOf()
    private val signatureInfo = mutableMapOf<String, String>()

    private val filePaths = mutableMapOf<PsiFile, Pair<String, Boolean>>()

    fun registerClass(clazz: ClassNode) {
        register(clazz, clazz.name)
    }

    fun registerMethod(clazz: ClassNode, method: MethodNode) {
        register(method, clazz.name + "#" + method.name + method.desc)
    }

    fun registerField(clazz: ClassNode, field: FieldNode) {
        register(field, clazz.name + "#" + field.name)
    }

    fun registerSignature(decl: JCTree.JCMethodDecl, method: MethodNode) {
        signatureInfo[getJavacSignature(decl)] = method.name + method.desc
    }

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = kaptContext.origins[asmNode]?.element ?: return
        register(fqName, psiElement)
    }

    private fun register(fqName: String, psiElement: PsiElement) {
        val textRange = psiElement.textRange ?: return

        val (path, isRelative) = getFilePathRelativePreferred(psiElement.containingFile)
        lineInfo[fqName] = KotlinPosition(path, isRelative, textRange.startOffset)
    }

    private fun getFilePathRelativePreferred(file: PsiFile): Pair<String, Boolean> {
        return filePaths.getOrPut(file) {
            val absolutePath = file.virtualFile.canonicalPath ?: file.virtualFile.path
            val absoluteFile = File(absolutePath)
            val baseFile = file.project.basePath?.let { File(it) }

            if (absoluteFile.exists() && baseFile != null && baseFile.exists()) {
                val relativePath = absoluteFile.relativeToOrNull(baseFile)?.path
                if (relativePath != null) {
                    return@getOrPut Pair(relativePath, true)
                }
            }

            return@getOrPut Pair(absolutePath, false)
        }
    }

    fun serialize(): ByteArray {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(METADATA_VERSION)

        oos.writeInt(lineInfo.size)
        for ((fqName, kotlinPosition) in lineInfo) {
            oos.writeUTF(fqName)
            oos.writeUTF(kotlinPosition.path)
            oos.writeBoolean(kotlinPosition.isRelativePath)
            oos.writeInt(kotlinPosition.pos)
        }

        oos.writeInt(signatureInfo.size)
        for ((javacSignature, methodDesc) in signatureInfo) {
            oos.writeUTF(javacSignature)
            oos.writeUTF(methodDesc)
        }

        oos.flush()
        return os.toByteArray()
    }

    class FileInfo(private val lineInfo: LineInfoMap, private val signatureInfo: Map<String, String>) {
        companion object {
            val EMPTY = FileInfo(mutableMapOf(), emptyMap())
        }

        fun getPositionFor(fqName: String) = lineInfo[fqName]
        fun getMethodDescriptor(decl: JCTree.JCMethodDecl) = signatureInfo[getJavacSignature(decl)]
    }
}