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

package org.jetbrains.kotlin.android.synthetic.idea.res

import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import org.jetbrains.kotlin.android.synthetic.idea.AndroidPsiTreeChangePreprocessor
import org.jetbrains.kotlin.android.synthetic.idea.AndroidXmlVisitor
import org.jetbrains.kotlin.android.synthetic.parseAndroidResource
import org.jetbrains.kotlin.android.synthetic.res.AndroidResource
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.psi.JetFile

class IDESyntheticFileGenerator(val module: Module) : SyntheticFileGenerator(module.project) {

    private val javaPsiFacade: JavaPsiFacade by lazy { JavaPsiFacade.getInstance(module.project) }
    private val moduleScope: GlobalSearchScope by lazy { module.getModuleWithDependenciesAndLibrariesScope(false) }

    private val cachedJetFiles: CachedValue<List<JetFile>> by lazy {
        cachedValue {
            val supportV4 = supportV4Available(javaPsiFacade, moduleScope)
            Result.create(generateSyntheticJetFiles(generateSyntheticFiles(true, moduleScope, supportV4)), psiTreeChangePreprocessor)
        }
    }

    override val layoutXmlFileManager: IDEAndroidLayoutXmlFileManager = IDEAndroidLayoutXmlFileManager(module)

    private val psiTreeChangePreprocessor by lazy {
        module.project.getExtensions(PsiTreeChangePreprocessor.EP_NAME).first { it is AndroidPsiTreeChangePreprocessor }
    }

    public override fun getSyntheticFiles(): List<JetFile> = cachedJetFiles.value

    override fun extractLayoutResources(files: List<PsiFile>, scope: GlobalSearchScope): List<AndroidResource> {
        val widgets = arrayListOf<AndroidResource>()
        val visitor = AndroidXmlVisitor { id, widgetType, attribute ->
            widgets += parseAndroidResource(id, widgetType) {
                resolveFqClassNameForView(javaPsiFacade, scope, it)
            }
        }

        files.forEach { it.accept(visitor) }
        return filterDuplicates(widgets)
    }

}