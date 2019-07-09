/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.javaFacade;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class KotlinJavaFacadeTest extends KotlinLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/javaFacade";
    }

    public void testDoNotWrapFunFromLocalClass() {
        doTestWrapMethod(true);
    }

    public void testObjectSubclassing() {
        doTestWrapMethod(true);
    }

    public void testDoNotWrapFunInAnonymousObject() {
        doTestWrapMethod(true);
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

    public void testEa68569() {
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
        doTestWrapProperty(true, true);
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

        String defaultCompanionObjectName = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString();
        PsiClass classObjectClass = theClass.findInnerClassByName(defaultCompanionObjectName, false);
        assertNotNull(classObjectClass);
        assertEquals("foo.TheClass." + defaultCompanionObjectName, classObjectClass.getQualifiedName());
        assertTrue(classObjectClass.hasModifierProperty(PsiModifier.STATIC));

        PsiField instance = theClass.findFieldByName(defaultCompanionObjectName, false);
        assertNotNull(instance);
        assertEquals("foo.TheClass." + defaultCompanionObjectName, instance.getType().getCanonicalText());
        assertTrue(instance.hasModifierProperty(PsiModifier.PUBLIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.STATIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.FINAL));

        PsiMethod[] methods = classObjectClass.findMethodsByName("getOut", false);

        assertEquals("java.io.PrintStream", methods[0].getReturnType().getCanonicalText());
    }

    public void testLightClassIsNotCreatedForBuiltins() throws Exception {
        myFixture.configureByFile(fileName());

        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assert reference != null;
        PsiElement element = reference.resolve();
        assertInstanceOf(element, KtClass.class);
        KtClass aClass = (KtClass) element;

        PsiClass createdByWrapDelegate = toLightClass(aClass);
        assertNull(createdByWrapDelegate);
    }

    private void doTestWrapMethod(boolean shouldBeWrapped) {
        KtNamedFunction jetFunction = getPreparedElement(KtNamedFunction.class);

        // Should not fail!
        PsiMethod psiMethod = LightClassUtil.INSTANCE.getLightClassMethod(jetFunction);

        checkDeclarationMethodWrapped(shouldBeWrapped, jetFunction, psiMethod);
    }

    private void doTestWrapParameter(boolean shouldWrapGetter, boolean shouldWrapSetter) {
        KtParameter jetParameter = getPreparedElement(KtParameter.class);

        // Should not fail!
        LightClassUtil.PropertyAccessorsPsiMethods propertyAccessors = LightClassUtil.INSTANCE.getLightClassPropertyMethods(jetParameter);

        checkDeclarationMethodWrapped(shouldWrapGetter, jetParameter, propertyAccessors.getGetter());
        checkDeclarationMethodWrapped(shouldWrapSetter, jetParameter, propertyAccessors.getSetter());
    }

    private void doTestWrapProperty(boolean shouldWrapGetter, boolean shouldWrapSetter) {
        KtProperty jetProperty = getPreparedElement(KtProperty.class);

        // Should not fail!
        LightClassUtil.PropertyAccessorsPsiMethods propertyAccessors = LightClassUtil.INSTANCE.getLightClassPropertyMethods(jetProperty);

        checkDeclarationMethodWrapped(shouldWrapGetter, jetProperty, propertyAccessors.getGetter());
        checkDeclarationMethodWrapped(shouldWrapSetter, jetProperty, propertyAccessors.getSetter());
    }

    private void doTestWrapPropertyAccessor(boolean shouldWrapAccessor) {
        KtPropertyAccessor jetPropertyAccessor = getPreparedElement(KtPropertyAccessor.class);

        // Should not fail!
        PsiMethod propertyAccessors = LightClassUtil.INSTANCE.getLightClassAccessorMethod(jetPropertyAccessor);
        checkDeclarationMethodWrapped(shouldWrapAccessor,
                                      PsiTreeUtil.getParentOfType(jetPropertyAccessor, KtProperty.class),
                                      propertyAccessors);
    }

    @NotNull
    private <T extends KtElement> T getPreparedElement(Class<T> elementClass) {
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

    private static void checkDeclarationMethodWrapped(boolean shouldBeWrapped, KtDeclaration declaration, PsiMethod psiMethod) {
        if (shouldBeWrapped) {
            assertNotNull(String.format("Failed to wrap declaration '%s' to method", declaration.getText()), psiMethod);
            assertInstanceOf(psiMethod, KtLightMethod.class);
            assertEquals("Invalid original element for generated method", ((KtLightMethod) psiMethod).getKotlinOrigin(), declaration);
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

        KtClass ktClass = PsiTreeUtil.getParentOfType(elementAt, KtClass.class);
        assertNotNull("Caret should be placed to class definition", ktClass);

        // Should not fail!
        KtLightClass lightClass = toLightClass(ktClass);

        assertNotNull(String.format("Failed to wrap jetClass '%s' to class", ktClass.getText()), lightClass);

        if (!(lightClass instanceof KtUltraLightClass)){
            // This invokes codegen with ClassBuilderMode = LIGHT_CLASSES
            // No exception/error should happen here
            lightClass.getClsDelegate();
        }
    }

    @NotNull
    @Override
    protected String fileName() {
        return getTestName(true) + ".kt";
    }
}
