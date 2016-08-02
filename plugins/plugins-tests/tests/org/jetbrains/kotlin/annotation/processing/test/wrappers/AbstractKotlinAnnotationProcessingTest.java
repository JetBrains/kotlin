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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractKotlinAnnotationProcessingTest extends AbstractCompilerLightClassTest {
    private final static Pattern SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^//\\s*(.*)$", Pattern.MULTILINE);
    
    @Override
    protected void doMultiFileTest(File testDataFile, Map<String, ModuleAndDependencies> modules, List<Void> files) throws IOException {
        String text = FileUtil.loadFile(testDataFile, true);
        Matcher matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text);
        assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find());
        String fqName = matcher.group(1);

        PsiClass lightClass = findLightClass(fqName);
        doTest(testDataFile, lightClass);
    }
    
    protected abstract void doTest(File testDataFile, PsiClass lightClass);
    
    private PsiClass findLightClass(String fqName) {
        try {
            return createFinder(getEnvironment()).findClass(fqName, GlobalSearchScope.allScope(getEnvironment().getProject()));
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }
}
