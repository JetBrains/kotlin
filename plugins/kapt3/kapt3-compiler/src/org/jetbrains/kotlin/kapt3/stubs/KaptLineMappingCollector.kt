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
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.*
import java.util.*

data class KotlinPosition(val path: String, val isRelativePath: Boolean, val line: Int, val column: Int)

private typealias LineInfoMap = MutableMap<String, KotlinPosition>

class KaptLineMappingCollector(private val kaptContext: KaptContext<*>) {
    companion object {
        val KAPT_METADATA_ANNOTATION_FQNAME = FqName("kapt.internal.KaptMetadata")
        val KAPT_SIGNATURE_ANNOTATION_FQNAME = FqName("kapt.internal.KaptSignature")

        fun parseMetadataAnnotation(file: JCTree.JCCompilationUnit): LineInfo {
            val metadata = file.defs.asSequence()
                                              .filterIsInstance<JCTree.JCClassDecl>()
                                              .mapNotNull { getAnnotationValue(KAPT_METADATA_ANNOTATION_FQNAME, it.mods) }
                                              .firstOrNull() ?: return LineInfo.EMPTY

            return LineInfo(deserializeMetadata(metadata))
        }

        private fun deserializeMetadata(data: String): LineInfoMap {
            val map: LineInfoMap = mutableMapOf()

            val ois = ObjectInputStream(ByteArrayInputStream(Base64.getDecoder().decode(data)))
            val count = ois.readInt()

            repeat(count) {
                val fqName = ois.readUTF()
                val path = ois.readUTF()
                val isRelative = ois.readBoolean()
                val line = ois.readInt()
                val column = ois.readInt()

                map[fqName] = KotlinPosition(path, isRelative, line, column)
            }

            return map
        }
    }

    private val lineInfo: LineInfoMap = mutableMapOf()
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

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = kaptContext.origins[asmNode]?.element ?: return
        register(fqName, psiElement)
    }

    private fun register(fqName: String, psiElement: PsiElement) {
        val textRange = psiElement.textRange ?: return
        val psiFile = psiElement.containingFile
        val lineAndColumn = DiagnosticUtils.getLineAndColumnInPsiFile(psiFile, textRange)

        if (lineAndColumn.line >= 0 && lineAndColumn.column >= 0) {
            val (path, isRelative) = getFilePathRelativePreferred(psiFile)
            lineInfo[fqName] = KotlinPosition(path, isRelative, lineAndColumn.line, lineAndColumn.column)
        }
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

    fun serializeLineInfo(): String {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)

        oos.writeInt(lineInfo.size)
        for ((fqName, kotlinPosition) in lineInfo) {
            oos.writeUTF(fqName)
            oos.writeUTF(kotlinPosition.path)
            oos.writeBoolean(kotlinPosition.isRelativePath)
            oos.writeInt(kotlinPosition.line)
            oos.writeInt(kotlinPosition.column)
        }

        oos.flush()
        return Base64.getEncoder().encodeToString(os.toByteArray())
    }

    class LineInfo(private val map: LineInfoMap) {
        companion object {
            val EMPTY = LineInfo(mutableMapOf())
        }

        fun getPositionFor(fqName: String) = map[fqName]
    }
}