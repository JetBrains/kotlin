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
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.Query;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor;
import org.jetbrains.kotlin.idea.test.TestUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSearcherTest extends LightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtilsKt.invalidateLibraryCache(getProject());
    }

    protected PsiClass getPsiClass(String className) {
        PsiClass psiClass = JavaPsiFacade.getInstance(getProject()).findClass(className, getGlobalScope());
        assertNotNull("Couldn't find a psiClass: " + className, psiClass);
        return psiClass;
    }

    private GlobalSearchScope getGlobalScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    protected GlobalSearchScope getProjectScope() {
        return GlobalSearchScope.projectScope(getProject());
    }

    protected static void checkResult(@NotNull String path, Query<?> actual) throws IOException {
        String text = FileUtil.loadFile(new File(path), true);

        List<String> classFqnFilters = InTextDirectivesUtils.findListWithPrefixes(text, "// IGNORE_CLASSES: ");

        List<String> actualModified = new ArrayList<>();
        for (Object member : actual) {
            if (member instanceof PsiClass) {
                String qualifiedName = ((PsiClass) member).getQualifiedName();
                assert qualifiedName != null;

                boolean filterOut = CollectionsKt.any(classFqnFilters, qualifiedName::startsWith);

                if (filterOut) {
                    continue;
                }
            }

            actualModified.add(stringRepresentation(member));
        }
        Collections.sort(actualModified);

        List<String> expected = InTextDirectivesUtils.findListWithPrefixes(text, "// SEARCH: ");
        Collections.sort(expected);

        assertOrderedEquals(actualModified, expected);
    }

    protected void checkClassWithDirectives(@NotNull String path) throws IOException {
        myFixture.configureByFile(path);
        List<String> directives = InTextDirectivesUtils.findListWithPrefixes(
                FileUtil.loadFile(new File(path), true), "// CLASS: ");
        assertFalse("Specify CLASS directive in test file", directives.isEmpty());
        String superClassName = directives.get(0);
        PsiClass psiClass = getPsiClass(superClassName);
        checkResult(path, ClassInheritorsSearch.search(psiClass, getProjectScope(), false));
    }

    private static String stringRepresentation(Object member) {
        if (member instanceof PsiClass) {
            return "class:" + ((PsiClass) member).getName();
        }
        if (member instanceof PsiMethod) {
            return "method:" + ((PsiMethod) member).getName();
        }
        if (member instanceof PsiField) {
            return "field:" + ((PsiField) member).getName();
        }
        throw new IllegalStateException("Do not know how to render member of type: " + member.getClass().getName());
    }

    protected String getPathToFile() {
        return getTestDataPath() + File.separator + getName() + ".kt";
    }

    protected String getFileName() {
        return getName() + ".kt";
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinLightProjectDescriptor.INSTANCE;
    }
}
