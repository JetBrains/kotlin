/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.lang.resolve.android

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.openapi.util.*
import com.intellij.openapi.vfs.impl.*
import com.intellij.openapi.vfs.*
import com.intellij.openapi.components.*
import com.intellij.openapi.extensions.*
import com.intellij.openapi.module.*
import org.jetbrains.kotlin.psi.JetProperty

public abstract class AndroidResourceManager(val project: Project) {

    public abstract val androidModuleInfo: AndroidModuleInfo?

    public open fun propertyToXmlAttributes(property: JetProperty): List<PsiElement> = listOf()

    public fun getLayoutXmlFiles(): Map<String, List<PsiFile>> {
        val info = androidModuleInfo ?: return mapOf()

        val psiManager = PsiManager.getInstance(project)
        val fileManager = VirtualFileManager.getInstance()

        fun VirtualFile.getAllChildren(): List<VirtualFile> {
            val allChildren = arrayListOf<VirtualFile>()
            val currentChildren = getChildren() ?: emptyArray()
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

        val resDirectories = info.resDirectories.map { fileManager.findFileByUrl("file://$it") }
        val allChildren = resDirectories.flatMap { it?.getAllChildren() ?: listOf() }

        return allChildren
                .filter { it.getParent().getName().startsWith("layout") && it.getName().toLowerCase().endsWith(".xml") }
                .map { psiManager.findFile(it) }
                .filterNotNull()
                .groupBy { it.getName().substringBeforeLast('.') }
                .mapValues { it.getValue().sortBy { it.getParent()!!.getName().length() } }
    }

    companion object {
        public fun getInstance(module: Module): AndroidResourceManager {
            val service = ModuleServiceManager.getService(module, javaClass<AndroidResourceManager>())
            return service ?: module.getComponent(javaClass<AndroidResourceManager>())
        }
    }

}

