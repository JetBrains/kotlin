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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.KtProperty
import java.util.*

class AndroidVariantData(val variant: AndroidVariant, private val layouts: Map<String, List<PsiFile>>): Map<String, List<PsiFile>> by layouts
class AndroidModuleData(val module: AndroidModule, private val variants: List<AndroidVariantData>): Iterable<AndroidVariantData> by variants {
    companion object {
        val EMPTY = AndroidModuleData(AndroidModule("android", listOf()), listOf())
    }
}

abstract class AndroidLayoutXmlFileManager(val project: Project) {

    public abstract val androidModule: AndroidModule?

    public open fun propertyToXmlAttributes(property: KtProperty): List<PsiElement> = listOf()

    public fun getLayoutXmlFiles(): AndroidModuleData {
        val androidModule = androidModule ?: return AndroidModuleData.EMPTY
        return AndroidModuleData(androidModule, androidModule.variants.map { getVariantData(it) })
    }

    public fun getVariantData(variant: AndroidVariant): AndroidVariantData {
        val psiManager = PsiManager.getInstance(project)
        val fileManager = VirtualFileManager.getInstance()

        fun VirtualFile.getAllChildren(): List<VirtualFile> {
            val allChildren = arrayListOf<VirtualFile>()
            val currentChildren = children ?: emptyArray()
            for (child in currentChildren) {
                if (child.isDirectory) {
                    allChildren.addAll(child.getAllChildren())
                }
                else {
                    allChildren.add(child)
                }
            }
            return allChildren
        }

        val resDirectories = variant.resDirectories.map { fileManager.findFileByUrl("file://$it") }
        val allChildren = resDirectories.flatMap { it?.getAllChildren() ?: listOf() }

        val allLayoutFiles = allChildren.filter { it.parent.name.startsWith("layout") && it.name.toLowerCase().endsWith(".xml") }
        val allLayoutPsiFiles = allLayoutFiles.fold(ArrayList<PsiFile>(allLayoutFiles.size)) { list, file ->
            val psiFile = psiManager.findFile(file)
            if (psiFile != null && psiFile.parent != null) {
                list += psiFile
            }
            list
        }

        val layoutNameToXmlFiles = allLayoutPsiFiles
                .groupBy { it.name.substringBeforeLast('.') }
                .mapValues { it.value.sortedBy { it.parent!!.name.length } }

        return AndroidVariantData(variant, layoutNameToXmlFiles)
    }

    companion object {
        public fun getInstance(module: Module): AndroidLayoutXmlFileManager? {
            val service = ModuleServiceManager.getService(module, AndroidLayoutXmlFileManager::class.java)
            return service ?: module.getComponent(AndroidLayoutXmlFileManager::class.java)
        }
    }

}
