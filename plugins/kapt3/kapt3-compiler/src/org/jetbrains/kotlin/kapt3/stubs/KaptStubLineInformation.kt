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

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.jetbrains.kotlin.kapt3.stubs.KaptLineMappingCollector.FileInfo
import org.jetbrains.kotlin.kapt3.util.getPackageNameJava9Aware

class KaptStubLineInformation {
    private val offsets = mutableMapOf<JCTree.JCCompilationUnit, FileInfo>()
    private val declarations = mutableMapOf<JCTree.JCCompilationUnit, List<JCTree>>()

    fun getPositionInKotlinFile(file: JCTree.JCCompilationUnit, element: JCTree): KotlinPosition? {
        val declaration = findDeclarationFor(element, file) ?: return null

        val fileInfo = offsets.getOrPut(file) { KaptLineMappingCollector.parseFileInfo(file) }
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
                    val newName = if (currentName.isEmpty()) className else currentName + "#" + className
                    if (declaration === parent) {
                        return newName
                    }

                    for (definition in parent.defs) {
                        getFqName(declaration, definition, className)?.let { return it }
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