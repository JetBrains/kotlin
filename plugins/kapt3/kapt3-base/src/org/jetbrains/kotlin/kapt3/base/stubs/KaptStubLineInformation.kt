/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.stubs

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
import java.io.ByteArrayInputStream
import java.io.File
import java.io.ObjectInputStream

class KaptStubLineInformation {
    private val offsets = mutableMapOf<JCTree.JCCompilationUnit, FileInfo>()
    private val declarations = mutableMapOf<JCTree.JCCompilationUnit, List<JCTree>>()

    companion object {
        const val KAPT_METADATA_EXTENSION = ".kapt_metadata"
        const val METADATA_VERSION = 1

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
    }

    fun getPositionInKotlinFile(file: JCTree.JCCompilationUnit, element: JCTree): KotlinPosition? {
        val declaration = findDeclarationFor(element, file) ?: return null

        val fileInfo = offsets.getOrPut(file) { parseFileInfo(file) }
        val elementDescriptor = getKaptDescriptor(declaration, file, fileInfo) ?: return null

        return fileInfo.getPositionFor(elementDescriptor)
    }

    private fun findDeclarationFor(element: JCTree, file: JCTree.JCCompilationUnit): JCTree? {
        val fileDeclarations = declarations.getOrPut(file) { collectDeclarations(file) }
        return fileDeclarations.firstOrNull { element.isLocatedInside(it) }
    }

    private fun getKaptDescriptor(declaration: JCTree, file: JCTree.JCCompilationUnit, fileInfo: FileInfo): String? {
        fun getFqName(declaration: JCTree, parent: JCTree, currentName: String): String? {
            return when (parent) {
                is JCTree.JCCompilationUnit -> {
                    for (definition in parent.defs) {
                        // There could be only class definitions on the top level
                        definition as? JCTree.JCClassDecl ?: continue
                        getFqName(declaration, definition, "")?.let { return it }
                    }
                    return null
                }
                is JCTree.JCClassDecl -> {
                    val className = parent.simpleName.toString()
                    val newName = if (currentName.isEmpty()) className else currentName + "$" + className
                    if (declaration === parent) {
                        return newName
                    }

                    for (definition in parent.defs) {
                        getFqName(declaration, definition, newName)?.let { return it }
                    }

                    return null
                }
                is JCTree.JCVariableDecl -> {
                    if (declaration === parent) {
                        return currentName + "#" + parent.name.toString()
                    }

                    return null
                }
                is JCTree.JCMethodDecl -> {
                    // We don't need to process local declarations here as kapt does not support locals entirely.
                    if (declaration === parent) {
                        val nameAndSignature = fileInfo.getMethodDescriptor(parent) ?: return null
                        return currentName + "#" + nameAndSignature
                    }

                    return null
                }
                else -> null
            }
        }

        // Unfortunately, we have to do this the hard way, as symbols may be not available yet
        // (for instance, if this code is called inside the "enterTrees()")
        val simpleDescriptor = getFqName(declaration, file, "")
        val packageName = file.getPackageNameJava9Aware()?.toString()?.replace('.', '/')
        return if (packageName == null) simpleDescriptor else "$packageName/$simpleDescriptor"
    }

    private fun collectDeclarations(file: JCTree.JCCompilationUnit): List<JCTree> {
        val declarations = mutableListOf<JCTree>()

        // Note that super.visit...() is above the declarations saving.
        // This allows us to get the deepest declarations in the beginning of the list.
        file.accept(object : TreeScanner() {
            override fun visitClassDef(tree: JCTree.JCClassDecl) {
                super.visitClassDef(tree)
                declarations += tree
            }

            override fun visitVarDef(tree: JCTree.JCVariableDecl) {
                // Do not visit variable contents, there can be nothing but local declarations which we don't support
                declarations += tree
            }

            override fun visitMethodDef(tree: JCTree.JCMethodDecl) {
                // Do not visit methods contents, there can be nothing but local declarations which we don't support
                declarations += tree
            }

            override fun visitTree(tree: JCTree?) {}
        })

        return declarations
    }

    private fun JCTree.isLocatedInside(declaration: JCTree): Boolean {
        var found = false

        declaration.accept(object : TreeScanner() {
            override fun scan(tree: JCTree?) {
                if (!found && tree === this@isLocatedInside) {
                    found = true
                }

                if (found) return
                super.scan(tree)
            }

            override fun scan(trees: com.sun.tools.javac.util.List<out JCTree>?) {
                // We don't need to repeat the logic above here as scan(List) calls scan(JCTree)
                if (found) return
                super.scan(trees)
            }
        })

        return found
    }
}