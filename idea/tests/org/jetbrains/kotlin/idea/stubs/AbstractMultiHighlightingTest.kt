/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.stubs

import com.intellij.codeInsight.completion.CompletionTestCase
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.testFramework.ExpectedHighlightingData

abstract class AbstractMultiHighlightingTest : AbstractMultiModuleTest() {

    protected open val shouldCheckLineMarkers = false

    protected open val shouldCheckResult = true

    override fun checkHighlighting(data: ExpectedHighlightingData): Collection<HighlightInfo> {
        data.init()
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()

        //to load text
        ApplicationManager.getApplication().runWriteAction { TreeUtil.clearCaches(myFile.node as TreeElement) }

        //to initialize caches
        if (!DumbService.isDumb(project)) {
            CacheManager.SERVICE.getInstance(myProject)
                    .getFilesWithWord("XXX", UsageSearchContext.IN_COMMENTS, GlobalSearchScope.allScope(myProject), true)
        }

        val infos = doHighlighting()

        val text = myEditor.document.text
        if (shouldCheckLineMarkers) {
            data.checkLineMarkers(DaemonCodeAnalyzerImpl.getLineMarkers(getDocument(file), project), text)
        }
        if (shouldCheckResult) {
            data.checkResult(infos, text)
        }
        return infos
    }

}