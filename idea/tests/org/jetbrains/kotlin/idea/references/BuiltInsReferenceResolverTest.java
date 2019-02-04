/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.navigation.GotoCheck;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.test.util.ReferenceUtils.renderAsGotoImplementation;

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
        List<DeclarationDescriptor> descriptors = new ArrayList<>();

        MemberScope builtIns = DefaultBuiltIns.getInstance().getBuiltInsPackageScope();

        for (DeclarationDescriptor packageMember : DescriptorUtils.getAllDescriptors(builtIns)) {
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

                @Override
                public Void visitPackageViewDescriptor(PackageViewDescriptor descriptor, Void data) {
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

        String expectedTarget = InTextDirectivesUtils.findStringWithPrefixes(text, "// TARGET:");
        assertEquals(expectedTarget, renderAsGotoImplementation(resolved));

        GotoCheck.assertNavigationElementMatches(resolved, text);
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
