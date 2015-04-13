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

package org.jetbrains.kotlin.search;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AnnotatedMembersSearchTest extends AbstractSearcherTest {

    public void testAnnotationsOnClass() throws IOException {
        doTest();
    }

    public void testAnnotationsOnFunction() throws IOException {
        doTest();
    }

    public void testAnnotationsWithParameters() throws IOException {
        doTest();
    }

    private void doTest() throws IOException {
        myFixture.configureByFile(getFileName());
        List<String> directives = InTextDirectivesUtils.findListWithPrefixes(FileUtil.loadFile(new File(getPathToFile()), true),
                                                                             "// ANNOTATION: ");
        assertFalse("Specify ANNOTATION directive in test file", directives.isEmpty());
        String annotationClassName = directives.get(0);
        PsiClass psiClass = getPsiClass(annotationClassName);
        checkResult(AnnotatedMembersSearch.search(psiClass, getProjectScope()));
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/search/annotations").getPath() + File.separator;
    }

}
