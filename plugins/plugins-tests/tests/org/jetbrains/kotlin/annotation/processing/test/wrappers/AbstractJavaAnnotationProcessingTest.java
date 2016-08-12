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

package org.jetbrains.kotlin.annotation.processing.test.wrappers;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava;
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractJavaAnnotationProcessingTest extends KotlinMultiFileTestWithJava<Void, Void> {
    private final static Pattern SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE);

    @NotNull
    @Override
    protected ConfigurationKind getConfigurationKind() {
        return ConfigurationKind.ALL;
    }

    @Override
    protected boolean isKotlinSourceRootNeeded() {
        return true;
    }
    
    @Override
    protected void doTest(String testDirPath) throws Exception {
        File testDir = new File(testDirPath);
        File javaFile = new File(testDirPath, testDir.getName() + ".java");

        String text = FileUtil.loadFile(javaFile, true);
        Matcher matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text);
        TestCase.assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find());
        String fqName = matcher.group(1);

        getEnvironment().updateClasspath(Collections.singletonList(testDir));

        super.doTest(new File(testDir.getParentFile(), "common.kt").getCanonicalPath());
        
        KotlinTestUtils.resolveAllKotlinFiles(getEnvironment());
        Project project = getEnvironment().getProject();
        
        PsiClass javaClass = JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project));
        TestCase.assertNotNull("Class $fqName was not found.", javaClass != null);
        doTest(javaFile, javaClass);
    }

    @Override
    protected void doMultiFileTest(File testDataFile, Map<String, ModuleAndDependencies> modules, List<Void> files) throws IOException {}
    
    protected abstract void doTest(File testDataFile, PsiClass lightClass);

    @Override
    protected Void createTestModule(@NotNull String name) {
        return null;
    }

    @Override
    protected Void createTestFile(Void module, String fileName, String text, Map<String, String> directives) {
        return null;
    }
}