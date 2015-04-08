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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.google.common.collect.Maps;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JdkAndMockLibraryProjectDescriptor;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.decompiler.JetClsFile;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;

import java.util.Map;

public class NavigateToDecompiledLibraryTest extends LightCodeInsightFixtureTestCase {
    protected static final String PACKAGE = "testData.libraries";
    protected static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation";

    private VirtualFile classFile;

    public void testAbstractClass() {
        doTest();
    }

    public void testClassWithAbstractAndOpenMembers() {
        doTest();
    }

    public void testColor() {
        doTest();
    }

    public void testLibrariesPackage() {
        doTest();
    }

    public void testSimpleClass() {
        doTest();
    }

    public void testSimpleTrait() {
        doTest();
    }

    public void testSimpleTraitImpl() {
        doTest();
    }

    public void testWithInnerAndObject() {
        doTest();
    }

    public void testWithTraitClassObject() {
        doTest();
    }

    public void testClassWithConstructor() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    private void doTest() {
        classFile = getClassFile(PACKAGE, getTestName(false), myModule);
        PsiFile decompiledPsiFile = PsiManager.getInstance(getProject()).findFile(classFile);
        assertNotNull(decompiledPsiFile);
        assertTrue("Expecting kotlin class file, was: " + decompiledPsiFile.getClass(), decompiledPsiFile instanceof JetClsFile);
        Map<String, JetDeclaration> map = getRenderedDescriptorToKotlinPsiMap(
                (JetFile) decompiledPsiFile, ((JetClsFile) decompiledPsiFile).getRenderedDescriptorsToRange()
        );
        String decompiledTextWithMarks = getDecompiledTextWithMarks(map);

        assertSameLinesWithFile(TEST_DATA_PATH + "/decompiled/" + getTestName(false) + ".kt", decompiledTextWithMarks);
    }

    private String getDecompiledTextWithMarks(Map<String, JetDeclaration> map) {
        String decompiledText = getDecompiledText();

        int[] openings = new int[decompiledText.length() + 1];
        int[] closings = new int[decompiledText.length() + 1];
        for (JetDeclaration jetDeclaration : map.values()) {
            TextRange textRange = jetDeclaration.getTextRange();
            openings[textRange.getStartOffset()]++;
            closings[textRange.getEndOffset()]++;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= decompiledText.length(); i++) {
            result.append(StringUtil.repeat("]", closings[i]));
            result.append(StringUtil.repeat("[", openings[i]));
            if (i < decompiledText.length()) {
                result.append(decompiledText.charAt(i));
            }
        }
        return result.toString();
    }

    private String getDecompiledText() {
        Document document = FileDocumentManager.getInstance().getDocument(classFile);
        assertNotNull(document);
        return document.getText();
    }

    @NotNull
    private static Map<String, JetDeclaration> getRenderedDescriptorToKotlinPsiMap(
            @NotNull JetFile file, @NotNull Map<String, TextRange> renderedDescriptorsToRanges
    ) {
        Map<String, JetDeclaration> renderedDescriptorsToJetDeclarations = Maps.newHashMap();
        for (Map.Entry<String, TextRange> renderedDescriptorToRange : renderedDescriptorsToRanges.entrySet()) {
            String renderedDescriptor = renderedDescriptorToRange.getKey();
            TextRange range = renderedDescriptorToRange.getValue();
            JetDeclaration jetDeclaration = PsiTreeUtil.findElementOfClassAtRange(file, range.getStartOffset(), range.getEndOffset(),
                                                                                  JetDeclaration.class);
            assert jetDeclaration != null : "Can't find declaration at " + range + ": "
                                            + file.getText().substring(range.getStartOffset(), range.getEndOffset());
            renderedDescriptorsToJetDeclarations.put(renderedDescriptor, jetDeclaration);
        }
        return renderedDescriptorsToJetDeclarations;
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/library", false);
    }

    @NotNull
    public static VirtualFile getClassFile(
            @NotNull String packageName,
            @NotNull String className,
            @NotNull Module module
    ) {
        VirtualFile root = findTestLibraryRoot(module);
        assertNotNull(root);
        VirtualFile packageDir = root.findFileByRelativePath(packageName.replace(".", "/"));
        assertNotNull(packageDir);
        VirtualFile classFile = packageDir.findChild(className + ".class");
        assertNotNull(classFile);
        return classFile;
    }

    @Nullable
    public static VirtualFile findTestLibraryRoot(@NotNull Module module) {
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (orderEntry instanceof LibraryOrderEntry) {
                return findTestLibraryRoot((LibraryOrderEntry) orderEntry);
            }
        }
        return null;
    }

    @NotNull
    private static VirtualFile findTestLibraryRoot(@NotNull LibraryOrderEntry library) {
        return library.getFiles(OrderRootType.CLASSES)[0];
    }
}
