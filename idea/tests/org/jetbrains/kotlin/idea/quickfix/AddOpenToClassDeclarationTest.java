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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.LocalFileSystemUtils;

public class AddOpenToClassDeclarationTest extends AbstractQuickFixMultiFileTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        LocalFileSystemUtils.refreshPath(getTestDataPath());
        String path = getTestDataPath() + "javaCode/";
        VirtualFile rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false);
        addSourceContentToRoots(myModule, rootDir);
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    }

    public void testFinalJavaSupertype() throws Exception {
        doTestWithoutExtraFile("FinalJavaSupertype.before.kt");
    }

    public void testFinalJavaUpperBound() throws Exception {
        doTestWithoutExtraFile("FinalJavaUpperBound.before.kt");
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/quickfix/modifiers/addOpenToClassDeclaration/finalJavaClass/";
    }
}
