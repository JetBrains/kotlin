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
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.AndroidXmlHandler
import org.jetbrains.kotlin.psi.KtFile
import java.io.ByteArrayInputStream

public open class CliSyntheticFileGenerator(
        project: Project,
        private val manifestPath: String,
        private val variants: List<AndroidVariant>
) : SyntheticFileGenerator(project) {

    private val cachedJetFiles by lazy {
        val supportV4 = supportV4Available()

        generateSyntheticJetFiles(generateSyntheticFiles(true, supportV4))
    }

    override val layoutXmlFileManager: CliAndroidLayoutXmlFileManager by lazy {
        CliAndroidLayoutXmlFileManager(project, manifestPath, variants)
    }

    public override fun getSyntheticFiles(): List<KtFile> = cachedJetFiles

    override fun extractLayoutResources(files: List<PsiFile>): List<AndroidResource> {
        val resources = arrayListOf<AndroidResource>()

        val handler = AndroidXmlHandler { id, tag ->
            resources += parseAndroidResource(id, tag) { tag ->
                resolveFqClassNameForView(tag)
            }
        }

        for (file in files) {
            try {
                val inputStream = ByteArrayInputStream(file.virtualFile.contentsToByteArray())
                layoutXmlFileManager.saxParser.parse(inputStream, handler)
            } catch (e: Throwable) {
                LOG.error(e)
            }
        }

        return filterDuplicates(resources)
    }

    override fun checkIfClassExist(fqName: String): Boolean {
        val scope = GlobalSearchScope.allScope(project)
        val psiElementFinders = project.getExtensions(PsiElementFinder.EP_NAME).filter { it is PsiElementFinderImpl }

        for (finder in psiElementFinders) {
            val clazz = finder.findClass(fqName, scope)
            if (clazz != null) return true
        }
        return false
    }

    override fun parseAndroidWidget(id: String, tag: String, fqNameResolver: (String) -> String?): AndroidResource {
        val fqName = fqNameResolver(tag)
        val invalidType = if (fqName != null) null else tag
        val type = fqName ?: (if ('.' in tag) tag else AndroidConst.VIEW_FQNAME)
        return AndroidWidget(id, type, invalidType)
    }

    private companion object {
        private val LOG: Logger = Logger.getInstance(CliSyntheticFileGenerator::class.java)
    }
}

