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

package org.jetbrains.kotlin.idea.lightClasses;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration;
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtLightClassForDecompiledDeclaration;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.psi.KtClassOrObject;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

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

        PsiClass lightClass1 = toLightClass(origin);
        PsiClass lightClass2 = toLightClass(origin);
        assertNotNull(lightClass1);
        assertNotNull(lightClass2);

        // If the same light class is returned twice, it means some caching was introduced and this test no longer makes sense.
        // Any other way of obtaining light classes should be used, which bypasses caches
        assertNotSame(lightClass1, lightClass2);

        assertEquals(lightClass1, lightClass2);
        assertEquals(lightClass1.hashCode(), lightClass2.hashCode());
    }
}
