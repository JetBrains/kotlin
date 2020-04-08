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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewBuilderProvider
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile

//TODO: workaround for bug in JavaClsStructureViewBuilderProvider, remove when IDEA api is updated
class KtClsStructureViewBuilderProvider : StructureViewBuilderProvider {
    override fun getStructureViewBuilder(fileType: FileType, file: VirtualFile, project: Project): StructureViewBuilder? {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? KtClsFile ?: return null
        return KotlinStructureViewFactory().getStructureViewBuilder(psiFile)
    }
}
