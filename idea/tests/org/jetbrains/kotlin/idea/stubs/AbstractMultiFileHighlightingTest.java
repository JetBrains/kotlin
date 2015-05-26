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

package org.jetbrains.kotlin.idea.stubs;

import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.impl.cache.CacheManager;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.testFramework.ExpectedHighlightingData;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.AstAccessControl;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

import java.io.File;
import java.util.Collection;

public abstract class AbstractMultiFileHighlightingTest extends CompletionTestCase {

    public void doTest(@NotNull String filePath) throws Exception {
        configureByFile(new File(filePath).getName(), "");
        boolean shouldFail = getName().contains("UnspecifiedType");
        AstAccessControl.INSTANCE$.testWithControlledAccessToAst(
                shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        checkHighlighting(myEditor, true, false);
                        return Unit.INSTANCE$;
                    }
                }
        );
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/multiFileHighlighting/";
    }

    //NOTE: partially copied from DaemonAnalyzerTestCase#checkHighlighting
    @Override
    @NotNull
    protected Collection<HighlightInfo> checkHighlighting(@NotNull ExpectedHighlightingData data) {
        data.init();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        //to load text
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                TreeUtil.clearCaches((TreeElement) myFile.getNode());
            }
        });

        //to initialize caches
        if (!DumbService.isDumb(getProject())) {
            CacheManager.SERVICE.getInstance(myProject)
                    .getFilesWithWord("XXX", UsageSearchContext.IN_COMMENTS, GlobalSearchScope.allScope(myProject), true);
        }

        Collection<HighlightInfo> infos = doHighlighting();

        String text = myEditor.getDocument().getText();
        data.checkLineMarkers(DaemonCodeAnalyzerImpl.getLineMarkers(getDocument(getFile()), getProject()), text);
        data.checkResult(infos, text);
        return infos;
    }
}
