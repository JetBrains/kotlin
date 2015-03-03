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

package org.jetbrains.kotlin.idea.javaFacade;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.KotlinLightClass;
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.JetLightProjectDescriptor;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;

public class JetJavaFacadeTest extends JetLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/javaFacade";
    }

    public void testDoNotWrapFunFromLocalClass() {
        doTestWrapMethod(false);
    }

    public void testObjectSubclassing() {
        doTestWrapMethod(true);
    }

    public void testDoNotWrapFunInAnonymousObject() {
        doTestWrapMethod(false);
    }

    public void testWrapFunInClassWithoutBody() {
        doTestWrapMethod(true);
    }

    public void testLocalClassSubclass() {
        doTestWrapClass();
    }

    public void testClassWithObjectLiteralInClassObjectField() {
        doTestWrapClass();
    }

    public void testClassWithObjectLiteralInConstructorProperty() {
        doTestWrapClass();
    }

    public void testClassWithObjectLiteralInFun() {
        doTestWrapClass();
    }

    public void testClassWithObjectLiteralInField() {
        doTestWrapClass();
    }

    public void testWrapFunInClassObject() {
        doTestWrapMethod(true);
    }

    public void testWrapTopLevelFun() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithDefaultParam() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithImplInTrait() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithoutImplInTrait() {
        doTestWrapMethod(true);
    }

    public void testWrapFunInObject() {
        doTestWrapMethod(true);
    }

    public void testWrapFunInObjectInObject() {
        doTestWrapMethod(true);
    }

    public void testKt2764() {
        doTestWrapClass();
    }

    public void testEa37034() {
        doTestWrapClass();
    }

    public void testEa46019() {
        doTestWrapClass();
    }

    public void testWrapTopLevelFunWithDefaultParams() {
        doTestWrapMethod(true);
    }

    public void testWrapValTopLevelProperty() {
        doTestWrapProperty(true, false);
    }

    public void testWrapVarPropertyInClass() {
        doTestWrapProperty(true, true);
    }

    public void testWrapVarPropertyWithAccessorsInTrait() {
        doTestWrapProperty(true, true);
    }

    public void testWrapVarTopLevelProperty() {
        doTestWrapProperty(true, true);
    }

    public void testWrapVarPropertyInLocalClass() {
        doTestWrapProperty(false, false);
    }

    public void testWrapVarTopLevelAccessor() {
        doTestWrapPropertyAccessor(true);
    }

    public void testWrapConstructorField() {
        doTestWrapParameter(true, true);
    }

    public void testWrapConstructorParameter() {
        doTestWrapParameter(false, false);
    }

    public void testWrapFunctionParameter() {
        doTestWrapParameter(false, false);
    }

    public void testInnerClass() throws Exception {
        myFixture.configureByFile(fileName());

        JavaPsiFacade facade = myFixture.getJavaFacade();
        PsiClass mirrorClass = facade.findClass("foo.Outer.Inner", GlobalSearchScope.allScope(getProject()));

        assertNotNull(mirrorClass);
        PsiMethod[] fun = mirrorClass.findMethodsByName("innerFun", false);

        assertEquals(fun[0].getReturnType(), PsiType.VOID);
    }

    public void testClassObject() throws Exception {
        myFixture.configureByFile(fileName());

        JavaPsiFacade facade = myFixture.getJavaFacade();
        PsiClass theClass = facade.findClass("foo.TheClass", GlobalSearchScope.allScope(getProject()));

        assertNotNull(theClass);

        PsiField classobjField = theClass.findFieldByName("$classobj", false);
        assertNull(classobjField);

        String defaultClassObjectName = SpecialNames.DEFAULT_NAME_FOR_DEFAULT_OBJECT.asString();
        PsiClass classObjectClass = theClass.findInnerClassByName(defaultClassObjectName, false);
        assertNotNull(classObjectClass);
        assertEquals("foo.TheClass." + defaultClassObjectName, classObjectClass.getQualifiedName());
        assertTrue(classObjectClass.hasModifierProperty(PsiModifier.STATIC));

        PsiField instance = theClass.findFieldByName(defaultClassObjectName, false);
        assertNotNull(instance);
        assertEquals("foo.TheClass." + defaultClassObjectName, instance.getType().getCanonicalText());
        assertTrue(instance.hasModifierProperty(PsiModifier.PUBLIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.STATIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.FINAL));

        PsiField deprecatedAccessor = theClass.findFieldByName(JvmAbi.DEPRECATED_DEFAULT_OBJECT_FIELD, false);
        assertNotNull(deprecatedAccessor);
        assertEquals("foo.TheClass." + defaultClassObjectName, deprecatedAccessor.getType().getCanonicalText());
        assertTrue(deprecatedAccessor.hasModifierProperty(PsiModifier.PUBLIC));
        assertTrue(deprecatedAccessor.hasModifierProperty(PsiModifier.STATIC));
        assertTrue(deprecatedAccessor.hasModifierProperty(PsiModifier.FINAL));
        assertTrue(deprecatedAccessor.isDeprecated());

        PsiMethod[] methods = classObjectClass.findMethodsByName("getOut", false);

        assertEquals("java.io.PrintStream", methods[0].getReturnType().getCanonicalText());
    }

    public void testLightClassIsNotCreatedForBuiltins() throws Exception {
        myFixture.configureByFile(fileName());

        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assert reference != null;
        PsiElement element = reference.resolve();
        assertInstanceOf(element, JetClass.class);
        JetClass aClass = (JetClass) element;

        PsiClass createdByWrapDelegate = LightClassUtil.getPsiClass(aClass);
        assertNull(createdByWrapDelegate);
    }

    private void doTestWrapMethod(boolean shouldBeWrapped) {
        JetNamedFunction jetFunction = getPreparedElement(JetNamedFunction.class);

        // Should not fail!
        PsiMethod psiMethod = LightClassUtil.getLightClassMethod(jetFunction);

        checkDeclarationMethodWrapped(shouldBeWrapped, jetFunction, psiMethod);
    }

    private void doTestWrapParameter(boolean shouldWrapGetter, boolean shouldWrapSetter) {
        JetParameter jetParameter = getPreparedElement(JetParameter.class);

        // Should not fail!
        LightClassUtil.PropertyAccessorsPsiMethods propertyAccessors = LightClassUtil.getLightClassPropertyMethods(jetParameter);

        checkDeclarationMethodWrapped(shouldWrapGetter, jetParameter, propertyAccessors.getGetter());
        checkDeclarationMethodWrapped(shouldWrapSetter, jetParameter, propertyAccessors.getSetter());
    }

    private void doTestWrapProperty(boolean shouldWrapGetter, boolean shouldWrapSetter) {
        JetProperty jetProperty = getPreparedElement(JetProperty.class);

        // Should not fail!
        LightClassUtil.PropertyAccessorsPsiMethods propertyAccessors = LightClassUtil.getLightClassPropertyMethods(jetProperty);

        JetPropertyAccessor getter = jetProperty.getGetter();
        JetPropertyAccessor setter = jetProperty.getSetter();

        checkDeclarationMethodWrapped(shouldWrapGetter, getter != null ? getter : jetProperty, propertyAccessors.getGetter());
        checkDeclarationMethodWrapped(shouldWrapSetter, setter != null ? setter : jetProperty, propertyAccessors.getSetter());
    }

    private void doTestWrapPropertyAccessor(boolean shouldWrapAccessor) {
        JetPropertyAccessor jetPropertyAccessor = getPreparedElement(JetPropertyAccessor.class);

        // Should not fail!
        PsiMethod propertyAccessors = LightClassUtil.getLightClassAccessorMethod(jetPropertyAccessor);
        checkDeclarationMethodWrapped(shouldWrapAccessor, jetPropertyAccessor, propertyAccessors);
    }

    @NotNull
    private <T extends JetElement> T getPreparedElement(Class<T> elementClass) {
        myFixture.configureByFile(fileName());

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAt = myFixture.getFile().findElementAt(offset);

        assertNotNull("Caret should be set for tested file", elementAt);

        T caretElement = PsiTreeUtil.getParentOfType(elementAt, elementClass);
        assertNotNull(
                String.format("Caret should be placed to element of type: %s, but was at element '%s' of type %s",
                              elementClass, elementAt, elementAt.getClass()),
                caretElement);

        return caretElement;
    }

    private static void checkDeclarationMethodWrapped(boolean shouldBeWrapped, JetDeclaration declaration, PsiMethod psiMethod) {
        if (shouldBeWrapped) {
            assertNotNull(String.format("Failed to wrap declaration '%s' to method", declaration.getText()), psiMethod);
            assertInstanceOf(psiMethod, KotlinLightMethod.class);
            assertEquals("Invalid original element for generated method", ((KotlinLightMethod) psiMethod).getOrigin(), declaration);
        }
        else {
            assertNull("There should be no wrapper for given method", psiMethod);
        }
    }

    private void doTestWrapClass() {
        myFixture.configureByFile(fileName());

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAt = myFixture.getFile().findElementAt(offset);

        assertNotNull("Caret should be set for tested file", elementAt);

        JetClass jetClass = PsiTreeUtil.getParentOfType(elementAt, JetClass.class);
        assertNotNull("Caret should be placed to class definition", jetClass);

        // Should not fail!
        KotlinLightClass lightClass = (KotlinLightClass) LightClassUtil.getPsiClass(jetClass);

        assertNotNull(String.format("Failed to wrap jetClass '%s' to class", jetClass.getText()), lightClass);

        // This invokes codegen with ClassBuilderMode = LIGHT_CLASSES
        // No exception/error should happen here
        lightClass.getDelegate();
    }

    @NotNull
    @Override
    protected String fileName() {
        return getTestName(true) + ".kt";
    }
}
