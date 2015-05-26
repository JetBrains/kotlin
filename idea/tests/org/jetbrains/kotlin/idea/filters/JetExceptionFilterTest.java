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

package org.jetbrains.kotlin.idea.filters;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.testFramework.PsiTestUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;

import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassFqName;
import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassName;
import static org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils.getPackagePartFqName;

public class JetExceptionFilterTest extends MultiFileTestCase {
    private VirtualFile rootDir;

    @Override
    protected void tearDown() throws Exception {
        rootDir = null;
        super.tearDown();
    }

    @Override
    @NotNull
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    @NotNull
    protected String getTestRoot() {
        return "/filters/exceptionFilter/";
    }

    private void configure() {
        try {
            String path = getTestDataPath() + getTestRoot() + getTestName(true);

            rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false);
            prepareProject(rootDir);
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    @NotNull
    private static String createStackTraceElementLine(@NotNull String fileName, @NotNull String className, int lineNumber) {
        // Method name doesn't matter
        String methodName = "foo";

        // File's last name appears in stack trace
        String fileLastName = new File(fileName).getName();

        StackTraceElement element = new StackTraceElement(className, methodName, fileLastName, lineNumber);
        return "\tat " + element + "\n";
    }

    private void doTest(@NotNull String fileName, int lineNumber, @NotNull Function1<VirtualFile, String> className) {
        if (rootDir == null) {
            configure();
        }
        assert rootDir != null;

        Filter filter = new JetExceptionFilterFactory().create(GlobalSearchScope.allScope(myProject));

        VirtualFile expectedFile = VfsUtilCore.findRelativeFile(fileName, rootDir);
        assertNotNull(expectedFile);

        String line = createStackTraceElementLine(fileName, className.invoke(expectedFile), lineNumber);
        Filter.Result result = filter.applyFilter(line, 0);

        assertNotNull(result);
        HyperlinkInfo info = result.getFirstHyperlinkInfo();
        assertNotNull(info);
        assertInstanceOf(info, OpenFileHyperlinkInfo.class);
        OpenFileDescriptor descriptor = ((OpenFileHyperlinkInfo) info).getDescriptor();
        assertNotNull(descriptor);

        assertEquals(expectedFile, descriptor.getFile());

        Document document = FileDocumentManager.getInstance().getDocument(expectedFile);
        assertNotNull(document);
        int expectedOffset = document.getLineStartOffset(lineNumber - 1);
        assertEquals(expectedOffset, descriptor.getOffset());
    }

    public void testSimple() {
        doTest("simple.kt", 2, new Function1<VirtualFile, String>() {
            @Override
            public String invoke(VirtualFile file) {
                return getPackageClassName(FqName.ROOT);
            }
        });
    }

    public void testKt2489() {
        final FqName packageClassFqName = getPackageClassFqName(FqName.ROOT);
        doTest("a.kt", 3, new Function1<VirtualFile, String>() {
            @Override
            public String invoke(VirtualFile file) {
                return getPackagePartFqName(packageClassFqName, file) + "$a$f$1";
            }
        });
        doTest("main.kt", 3, new Function1<VirtualFile, String>() {
            @Override
            public String invoke(VirtualFile file) {
                return getPackagePartFqName(packageClassFqName, file) + "$main$f$1";
            }
        });
    }

    public void testMultiSameName() {
        final FqName packageClassFqName = getPackageClassFqName(new FqName("multiSameName"));
        // The order and the exact names do matter here
        doTest("1/foo.kt", 4, new Function1<VirtualFile, String>() {
            @Override
            public String invoke(VirtualFile file) {
                return getPackagePartFqName(packageClassFqName, file) + "$foo$f$1";
            }
        });
        doTest("2/foo.kt", 4, new Function1<VirtualFile, String>() {
            @Override
            public String invoke(VirtualFile file) {
                return getPackagePartFqName(packageClassFqName, file) + "$foo$f$1";
            }
        });
    }
}
