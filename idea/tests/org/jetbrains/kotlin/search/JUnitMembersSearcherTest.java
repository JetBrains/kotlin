/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.search;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;

@TestMetadata("idea/testData/search/junit/")
@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class JUnitMembersSearcherTest extends AbstractSearcherTest {
    private static final LightProjectDescriptor junitProjectDescriptor =
            new KotlinJdkAndLibraryProjectDescriptor(new File(PathManager.getHomePath().replace(File.separatorChar, '/') + "/lib/junit-4.12.jar"));

    @TestMetadata("testJunit3.kt")
    public void testJunit3() throws IOException {
        doJUnit3test();
    }

    public void testJunit4() throws IOException {
        doJUnit4test();
    }

    public void testJunit4Alias() throws IOException {
        doJUnit4test();
    }

    public void testJunit4FancyAlias() throws IOException {
        doJUnit4test();
    }

    private void doJUnit3test() throws IOException {
        checkClassWithDirectives("idea/testData/search/junit/testJunit3.kt");
    }

    private void doJUnit4test() throws IOException {
        myFixture.configureByFile(getFileName());
        List<String> directives = InTextDirectivesUtils.findListWithPrefixes(FileUtil.loadFile(new File(getPathToFile()), true),
                                                                             "// ANNOTATION: ");
        assertFalse("Specify ANNOTATION directive in test file", directives.isEmpty());
        String annotationClassName = directives.get(0);
        PsiClass psiClass = getPsiClass(annotationClassName);
        checkResult(getPathToFile(), AnnotatedMembersSearch.search(psiClass, getProjectScope()));
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return junitProjectDescriptor;
    }
}
