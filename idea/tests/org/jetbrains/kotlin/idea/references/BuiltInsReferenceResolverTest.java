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

package org.jetbrains.kotlin.idea.references;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.ReferenceUtils;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.test.ReferenceUtils.getFileWithDir;
import static org.jetbrains.kotlin.test.ReferenceUtils.renderAsGotoImplementation;

public class BuiltInsReferenceResolverTest extends KotlinLightCodeInsightFixtureTestCase {
    public void testAny() throws Exception {
        doTest();
    }

    public void testInt() throws Exception {
        doTest();
    }

    public void testNothing() throws Exception {
        doTest();
    }

    public void testEquals() throws Exception {
        doTest();
    }

    public void testToString() throws Exception {
        doTest();
    }

    public void testTimes() throws Exception {
        doTest();
    }

    public void testUnit() throws Exception {
        doTest();
    }

    public void testEmptyRange() throws Exception {
        doTest();
    }

    public void testIntArrayConstructor() throws Exception {
        doTest();
    }

    public void testAllReferencesResolved() {
        for (DeclarationDescriptor descriptor : getAllStandardDescriptors()) {
            assertNotNull("Can't resolve " + descriptor, DescriptorToSourceUtilsIde.INSTANCE.getAnyDeclaration(getProject(), descriptor));
        }
    }

    private static Collection<DeclarationDescriptor> getAllStandardDescriptors() {
        final List<DeclarationDescriptor> descriptors = new ArrayList<DeclarationDescriptor>();

        PackageFragmentDescriptor builtinsPackageFragment = DefaultBuiltIns.getInstance().getBuiltInsPackageFragment();

        for (DeclarationDescriptor packageMember : DescriptorUtils.getAllDescriptors(builtinsPackageFragment.getMemberScope())) {
            packageMember.acceptVoid(new DeclarationDescriptorVisitorEmptyBodies<Void, Void>() {
                @Override
                public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
                    descriptors.add(descriptor);
                    for (DeclarationDescriptor classMember : DescriptorUtils
                            .getAllDescriptors(descriptor.getDefaultType().getMemberScope())) {
                        classMember.acceptVoid(this);
                    }
                    return null;
                }

                @Override
                public Void visitDeclarationDescriptor(DeclarationDescriptor descriptor, Void data) {
                    descriptors.add(descriptor);
                    return null;
                }
            });
        }

        return descriptors;
    }

    private void doTest() throws Exception {
        PsiPolyVariantReference reference = (PsiPolyVariantReference) myFixture.getReferenceAtCaretPosition(getTestName(true) + ".kt");
        assert reference != null;
        PsiElement resolved = reference.resolve();
        assertNotNull(resolved);
        assertEquals(1, reference.multiResolve(false).length);

        String text = myFixture.getFile().getText();
        String expectedBinaryFile = InTextDirectivesUtils.findStringWithPrefixes(text, "// BINARY:");
        String expectedSourceFile = InTextDirectivesUtils.findStringWithPrefixes(text, "// SRC:");
        String expectedTarget = InTextDirectivesUtils.findStringWithPrefixes(text, "// TARGET:");

        assertEquals(expectedBinaryFile, getFileWithDir(resolved));
        assertEquals(expectedTarget, renderAsGotoImplementation(resolved));
        PsiElement srcElement = resolved.getNavigationElement();
        Assert.assertNotEquals(srcElement, resolved);
        assertEquals(expectedSourceFile, getFileWithDir(srcElement));
        assertEquals(expectedTarget, renderAsGotoImplementation(srcElement));
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/resolve/builtins/";
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
