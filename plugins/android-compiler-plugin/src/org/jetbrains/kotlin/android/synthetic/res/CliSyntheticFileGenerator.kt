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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import java.io.ByteArrayInputStream
import org.jetbrains.kotlin.android.synthetic.AndroidXmlHandler
import org.jetbrains.kotlin.android.synthetic.parseAndroidResource
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.JetFile
import kotlin.properties.Delegates

public open class CliSyntheticFileGenerator(
        project: Project,
        private val manifestPath: String,
        private val resDirectories: List<String>
) : SyntheticFileGenerator(project) {

    private val javaPsiFacade: JavaPsiFacade by lazy { JavaPsiFacade.getInstance(project) }
    private val projectScope: GlobalSearchScope by lazy { GlobalSearchScope.allScope(project) }

    private val cachedJetFiles by lazy {
        val supportV4 = supportV4Available(javaPsiFacade, projectScope)
        generateSyntheticJetFiles(generateSyntheticFiles(true, projectScope, supportV4))
    }

    override val layoutXmlFileManager: CliAndroidLayoutXmlFileManager by Delegates.lazy {
        CliAndroidLayoutXmlFileManager(project, manifestPath, resDirectories)
    }

    public override fun getSyntheticFiles(): List<JetFile> = cachedJetFiles

    override fun extractLayoutResources(files: List<PsiFile>, scope: GlobalSearchScope): List<AndroidResource> {
        val resources = arrayListOf<AndroidResource>()

        val handler = AndroidXmlHandler { id, tag ->
            resources += parseAndroidResource(id, tag) { tag ->
                resolveFqClassNameForView(javaPsiFacade, scope, tag)
            }
        }

        for (file in files) {
            try {
                val inputStream = ByteArrayInputStream(file.getVirtualFile().contentsToByteArray())
                layoutXmlFileManager.saxParser.parse(inputStream, handler)
            } catch (e: Throwable) {
                LOG.error(e)
            }
        }

        return filterDuplicates(resources)
    }

    private companion object {
        private val LOG: Logger = Logger.getInstance(javaClass)
    }
}

