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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlToken
import org.jetbrains.kotlin.android.synthetic.res.AndroidLayoutXmlFileManager

public class AndroidPsiTreeChangePreprocessor : PsiTreeChangePreprocessor, SimpleModificationTracker() {

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (event.code in HANDLED_EVENTS) {
            val child = event.child

            // We should get more precise event notification (not just "that file was changed somehow")
            if (child == null && event.code == PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED) {
                return
            }

            if (checkIfLayoutFile(child)) {
                incModificationCount()
                return
            }

            val file = event.file ?: return
            if (!checkIfLayoutFile(file)) return

            val xmlAttribute = findXmlAttribute(child)
            if (xmlAttribute != null) {
                val name = xmlAttribute.name
                if (name != "android:id" && name != "class") return
            }

            incModificationCount()
        }
    }

    private companion object {
        private val HANDLED_EVENTS = setOf(
                PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED,
                PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED)

        private fun checkIfLayoutFile(element: PsiElement): Boolean {
            val xmlFile = element as? XmlFile ?: return false

            val projectFileIndex = ProjectRootManager.getInstance(xmlFile.project).fileIndex
            val module = projectFileIndex.getModuleForFile(xmlFile.virtualFile)

            if (module != null) {
                val resourceManager = AndroidLayoutXmlFileManager.getInstance(module)
                val resDirectories = resourceManager.getModuleResDirectories()
                val baseDirectory = xmlFile.parent?.parent?.virtualFile

                if (baseDirectory in resDirectories && xmlFile.isLayoutXmlFile()) {
                    return true
                }
            }

            return false
        }

        private fun findXmlAttribute(element: PsiElement?): XmlAttribute? {
            return when (element) {
                is XmlToken, is XmlAttributeValue -> findXmlAttribute(element.parent)
                is XmlAttribute -> element
                else -> null
            }
        }

        private fun AndroidLayoutXmlFileManager.getModuleResDirectories(): List<VirtualFile> {
            val info = androidModuleInfo ?: return listOf()

            val fileManager = VirtualFileManager.getInstance()
            return info.resDirectories.map { fileManager.findFileByUrl("file://$it") }.filterNotNull()
        }

        private fun PsiFile.isLayoutXmlFile(): Boolean {
            if (fileType != XmlFileType.INSTANCE) return false
            return parent?.name?.startsWith("layout") ?: false
        }
    }

}