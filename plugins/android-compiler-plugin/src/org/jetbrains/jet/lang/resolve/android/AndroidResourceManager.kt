/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFile

public abstract class AndroidResourceManager(val project: Project) {

    public abstract val androidModuleInfo: AndroidModuleInfo?

    public open fun idToXmlAttribute(id: String): PsiElement? = null

    open fun getLayoutXmlFiles(): List<PsiFile> {
        val info = androidModuleInfo
        if (info == null) return listOf()

        val psiManager = PsiManager.getInstance(project)
        val fileManager = VirtualFileManager.getInstance()

        fun VirtualFile.getAllChildren(): List<VirtualFile> {
            val allChildren = arrayListOf<VirtualFile>()
            val currentChildren = getChildren() ?: array()
            for (child in currentChildren) {
                if (child.isDirectory()) {
                    allChildren.addAll(child.getAllChildren())
                }
                else {
                    allChildren.add(child)
                }
            }
            return allChildren
        }

        val resDirectory = fileManager.findFileByUrl("file://" + info.mainResDirectory)
        val allChildren = resDirectory?.getAllChildren() ?: listOf()

        return allChildren
                .filter { it.getParent().getName().startsWith("layout") && it.getName().toLowerCase().endsWith(".xml") }
                .map { psiManager.findFile(it) }
                .filterNotNull()
                .sortBy { it.getName() }
    }

    inner class NoLayoutXmlFound : Exception("No android UI xmls found in " + androidModuleInfo?.mainResDirectory)

}
