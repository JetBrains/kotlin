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

package org.jetbrains.kotlin.plugin.android

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.plugin.android.IDEAndroidResourceManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.plugin.android.AndroidXmlVisitor
import com.intellij.psi.impl.*
import kotlin.properties.*
import org.jetbrains.kotlin.lang.resolve.android.*

class IDEAndroidUIXmlProcessor(val module: Module) : AndroidUIXmlProcessor(module.getProject()) {

    override val resourceManager: IDEAndroidResourceManager = IDEAndroidResourceManager(module)

    override val psiTreeChangePreprocessor by Delegates.lazy {
        module.getProject().getExtensions(PsiTreeChangePreprocessor.EP_NAME).first { it is AndroidPsiTreeChangePreprocessor }
    }

    override fun parseSingleFile(file: PsiFile): List<AndroidWidget> {
        val widgets = arrayListOf<AndroidWidget>()
        file.accept(AndroidXmlVisitor(resourceManager, { id, wClass, valueElement ->
            widgets.add(AndroidWidget(id, wClass))
        }))

        return widgets
    }

}