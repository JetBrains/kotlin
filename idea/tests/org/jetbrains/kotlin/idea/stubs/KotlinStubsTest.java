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

package org.jetbrains.kotlin.idea.stubs;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.List;

public class KotlinStubsTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinLightProjectDescriptor.INSTANCE;
    }

    public void testSuperclassNames() {
        PsiFile psiFile = myFixture.configureByText("foo.kt", "import java.util.ArrayList as alist\nclass C(): alist() { }");
        List<KtDeclaration> declarations = ((KtFile) psiFile).getDeclarations();
        KtClass ktClass = (KtClass) declarations.get(0);
        KotlinClassStub stub = KtStubElementTypes.CLASS.createStub(ktClass, null);
        List<String> names = stub.getSuperNames();
        assertSameElements(names, "ArrayList", "alist");
    }

    public void testClassIsTrait() {
        PsiFile psiFile = myFixture.configureByText("foo.kt", "interface Test { }");
        List<KtDeclaration> declarations = ((KtFile) psiFile).getDeclarations();
        KtClass ktClass = (KtClass) declarations.get(0);
        KotlinClassStub stub = KtStubElementTypes.CLASS.createStub(ktClass, null);
        assertEquals(true, stub.isInterface());
    }
}
