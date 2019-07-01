/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.lightClasses;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration;
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class LightClassEqualsTest extends KotlinLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    public void testEqualsForExplicitDeclaration() throws Exception {
        myFixture.configureByText("a.kt", "class A");

        PsiClass theClass = myFixture.getJavaFacade().findClass("A");
        assertNotNull(theClass);
        assertInstanceOf(theClass, KtLightClassForSourceDeclaration.class);

        doTestEquals(((KtLightClass) theClass).getKotlinOrigin());
    }

    public void testEqualsForDecompiledClass() throws Exception {
        myFixture.configureByText("a.kt", "");

        PsiClass theClass = myFixture.getJavaFacade().findClass("kotlin.Unit");
        assertNotNull(theClass);
        assertInstanceOf(theClass, KtLightClassForDecompiledDeclaration.class);

        doTestEquals(((KtLightClass) theClass).getKotlinOrigin());
    }

    private static void doTestEquals(@Nullable KtClassOrObject origin) {
        assertNotNull(origin);

        PsiClass lightClass1 = KtLightClassForSourceDeclaration.Companion.createNoCache(origin, true);
        PsiClass lightClass2 = KtLightClassForSourceDeclaration.Companion.createNoCache(origin, true);
        assertNotNull(lightClass1);
        assertNotNull(lightClass2);

        // If the same light class is returned twice, it means some caching was introduced and this test no longer makes sense.
        // Any other way of obtaining light classes should be used, which bypasses caches
        assertNotSame(lightClass1, lightClass2);

        assertEquals(lightClass1, lightClass2);
        assertEquals(lightClass1.hashCode(), lightClass2.hashCode());
    }
}
