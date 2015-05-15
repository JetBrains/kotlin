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

package org.jetbrains.kotlin.idea.refactoring.changeSignature;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.changeSignature.ThrownExceptionInfo;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.VisibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.Visibilities;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsPackage;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.io.File;
import java.util.*;

public class JetChangeSignatureTest extends KotlinCodeInsightTestCase {
    public void testBadSelection() throws Exception {
        configureByFile(getTestName(false) + "Before.kt");
        Editor editor = getEditor();
        PsiFile file = getFile();
        assertNull(new JetChangeSignatureHandler().findTargetMember(file, editor));
    }

    public void testRenameFunction() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("after");
        doTest(changeInfo);
    }

    public void testChangeReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Float");
        doTest(changeInfo);
    }

    public void testAddReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Float");
        doTest(changeInfo);
    }

    public void testRemoveReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Unit");
        doTest(changeInfo);
    }

    public void testChangeConstructorVisibility() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PROTECTED);
        doTest(changeInfo);
    }

    public void testSynthesized() throws Exception {
        try {
            getChangeInfo();
        }
        catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertEquals(JetRefactoringBundle.message("cannot.refactor.synthesized.function", DataClassUtilsPackage.createComponentName(1).asString()), e.getMessage());
            return;
        }
        fail();
    }

    public void testPreferContainedInClass() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        assertEquals("param", changeInfo.getNewParameters()[0].getName());
    }

    public void testAddConstructorVisibility() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PROTECTED);
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo newParameter = new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                             -1, "x", KotlinBuiltIns.getInstance().getAnyType(),
                                                             null, psiFactory.createExpression("12"), JetValVar.Val, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testConstructor() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setValOrVar(JetValVar.Var);
        changeInfo.getNewParameters()[1].setValOrVar(JetValVar.None);
        changeInfo.getNewParameters()[2].setValOrVar(JetValVar.Val);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Float?");
        doTest(changeInfo);
    }

    public void testGenericConstructor() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setValOrVar(JetValVar.Var);
        changeInfo.getNewParameters()[1].setValOrVar(JetValVar.None);
        changeInfo.getNewParameters()[2].setValOrVar(JetValVar.Val);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Double?");
        doTest(changeInfo);
    }

    public void testConstructorSwapArguments() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        JetParameterInfo param = changeInfo.getNewParameters()[0];
        changeInfo.setNewParameter(0, changeInfo.getNewParameters()[2]);
        changeInfo.setNewParameter(2, param);
        doTest(changeInfo);
    }

    public void testFunctions() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Float?");
        doTest(changeInfo);
    }

    public void testGenericFunctions() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Double?");
        doTest(changeInfo);
    }

    public void testExpressionFunction() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("x1");
        changeInfo.addParameter(new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                     -1, "y1", KotlinBuiltIns.getInstance().getIntType(), null, null, JetValVar.None, null));
        doTest(changeInfo);
    }

    public void testFunctionsAddRemoveArguments() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.INTERNAL);
        changeInfo.setNewParameter(2, changeInfo.getNewParameters()[1]);
        changeInfo.setNewParameter(1, changeInfo.getNewParameters()[0]);
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo newParameter =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "x0", KotlinBuiltIns.getInstance().getNullableAnyType(), null, psiFactory.createExpression("null"), JetValVar.None, null);
        changeInfo.setNewParameter(0, newParameter);
        doTest(changeInfo);
    }

    public void testFakeOverride() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                             -1, "i", KotlinBuiltIns.getInstance().getIntType(), null, null, JetValVar.None, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testFunctionLiteral() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[1].setName("y1");
        changeInfo.addParameter(new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                     -1, "x", KotlinBuiltIns.getInstance().getAnyType(), null, null, JetValVar.None, null));
        changeInfo.setNewReturnTypeText("Int");
        doTest(changeInfo);
    }

    public void testVarargs() throws Exception {
        try {
            getChangeInfo();
        }
        catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertEquals("Can't refactor the function with variable arguments", e.getMessage());
            return;
        }

        fail("Exception expected");
    }

    public void testUnmodifiableFromLibrary() throws Exception {
        doTestUnmodifiableCheck();
    }


    public void testUnmodifiableFromBuiltins() throws Exception {
        doTestUnmodifiableCheck();
    }

    private void doTestUnmodifiableCheck() throws Exception {
        try {
            JetChangeInfo changeInfo = getChangeInfo();
            JetElement method = (JetElement) changeInfo.getMethod();
            JetChangeSignatureConfiguration empty = new JetChangeSignatureConfiguration() {
                @NotNull
                @Override
                public JetMethodDescriptor configure(@NotNull JetMethodDescriptor originalDescriptor, @NotNull BindingContext bindingContext) {
                    return originalDescriptor;
                }

                @Override
                public boolean performSilently(@NotNull Collection<? extends PsiElement> elements) {
                    return true;
                }
            };
            BindingContext context = ResolvePackage.analyze(method, BodyResolveMode.FULL);

            ChangeSignaturePackage
                    .runChangeSignature(getProject(), ChangeSignaturePackage.getOriginalBaseFunctionDescriptor(changeInfo), empty, context, method, "test");
        }
        catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Refactoring cannot be"));
            return;
        }
        fail("Exception expected");
    }

    public void testInnerFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("inner2");
        changeInfo.getNewParameters()[0].setName("y");
        doTestConflict(changeInfo);
    }

    public void testMemberFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("inner2");
        changeInfo.getNewParameters()[0].setName("y");
        doTestConflict(changeInfo);
    }

    public void testTopLevelFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("fun2");
        doTestConflict(changeInfo);
    }

    public void testConstructorsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("_x");
        changeInfo.getNewParameters()[1].setName("_y");
        changeInfo.getNewParameters()[2].setName("_z");
        doTestConflict(changeInfo);
    }

    public void testNoDefaultValuesInOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(0, newParameters[1]);
        changeInfo.setNewParameter(1, newParameters[0]);
        doTest(changeInfo);
    }

    public void testOverridesInEnumEntries() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                             -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, null, JetValVar.None, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testEnumEntriesWithoutSuperCalls() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo newParameter =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "n", KotlinBuiltIns.getInstance().getIntType(), null, psiFactory.createExpression("1"), JetValVar.None, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testParameterChangeInOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo parameterInfo = changeInfo.getNewParameters()[0];
        parameterInfo.setName("n");
        parameterInfo.setCurrentTypeText("Int");
        doTest(changeInfo);
    }

    public void testConstructorJavaUsages() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo newParameter =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"abc\""), JetValVar.None, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesAddParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"abc\""), JetValVar.None, null)
        );
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "o", KotlinBuiltIns.getInstance().getNullableAnyType(), null, psiFactory.createExpression("\"def\""), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesChangeNullability() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[1].setCurrentTypeText("String?");
        newParameters[2].setCurrentTypeText("Any");

        changeInfo.setNewReturnTypeText("String?");

        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesChangeTypes() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[0].setCurrentTypeText("String?");
        newParameters[1].setCurrentTypeText("Int");
        newParameters[2].setCurrentTypeText("Long?");

        changeInfo.setNewReturnTypeText("Any?");

        doTest(changeInfo);
    }

    public void testGenericsWithOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[0].setCurrentTypeText("List<C>");
        newParameters[1].setCurrentTypeText("A?");
        newParameters[2].setCurrentTypeText("U<B>");

        changeInfo.setNewReturnTypeText("U<C>?");

        doTest(changeInfo);
    }

    public void testAddReceiverToGenericsWithOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo parameterInfo = changeInfo.getNewParameters()[0];
        parameterInfo.setCurrentTypeText("U<A>");
        changeInfo.setReceiverParameterInfo(parameterInfo);

        doTest(changeInfo);
    }

    public void testJavaMethodKotlinUsages() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    String getNewName(@NotNull PsiMethod method) {
                        return "bar";
                    }

                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 1);
                    }
                }
        );
    }

    public void testJavaConstructorKotlinUsages() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 1);
                    }
                }
        );
    }

    public void testSAMAddToEmptyParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        PsiType paramType = PsiType.getJavaLangString(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        return new ParameterInfoImpl[] {new ParameterInfoImpl(-1, "s", paramType)};
                    }
                }
        );
    }

    public void testSAMAddToSingletonParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        PsiParameter parameter = method.getParameterList().getParameters()[0];
                        ParameterInfoImpl originalParameter = new ParameterInfoImpl(0, parameter.getName(), parameter.getType());
                        ParameterInfoImpl newParameter = new ParameterInfoImpl(-1, "n", PsiType.INT);

                        return new ParameterInfoImpl[] {newParameter, originalParameter};
                    }
                }
        );
    }

    public void testSAMAddToNonEmptyParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] originalParameters = super.getNewParameters(method);
                        ParameterInfoImpl[] newParameters = Arrays.copyOf(originalParameters, originalParameters.length + 1);

                        PsiType paramType = PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        newParameters[originalParameters.length] = new ParameterInfoImpl(-1, "o", paramType);

                        return newParameters;
                    }
                }
        );
    }

    public void testSAMRemoveSingletonParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return new ParameterInfoImpl[0];
                    }
                }
        );
    }

    public void testSAMRemoveParam() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 0);
                    }
                }
        );
    }

    public void testSAMRenameParam() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setName("p");
                        return newParameters;
                    }
                }
        );
    }

    public void testSAMChangeParamType() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setType(PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject())));
                        return newParameters;
                    }
                }
        );
    }

    public void testSAMRenameMethod() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    String getNewName(@NotNull PsiMethod method) {
                        return "bar";
                    }
                }
        );
    }

    public void testSAMChangeMethodReturnType() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @Nullable
                    @Override
                    PsiType getNewReturnType(@NotNull PsiMethod method) {
                        return PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                    }
                }
        );
    }

    public void testGenericsWithSAMConstructors() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    final PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();

                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setType(factory.createTypeFromText("java.util.List<X<B>>", method.getParameterList()));
                        newParameters[1].setType(factory.createTypeFromText("X<java.util.Set<A>>", method.getParameterList()));
                        return newParameters;
                    }

                    @Nullable
                    @Override
                    PsiType getNewReturnType(@NotNull PsiMethod method) {
                        return factory.createTypeFromText("X<java.util.List<A>>", method);
                    }
                }
        );
    }

    public void testFunctionRenameJavaUsages() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("bar");
        doTest(changeInfo);
    }

    public void testParameterModifiers() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.addParameter(new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                     -1, "n", KotlinBuiltIns.getInstance().getIntType(), null, null, JetValVar.None, null));
        doTest(changeInfo);
    }

    public void testFqNameShortening() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo parameterInfo = new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                              -1, "s", KotlinBuiltIns.getInstance().getAnyType(), null, null, JetValVar.None, null);
        parameterInfo.setCurrentTypeText("kotlin.String");
        changeInfo.addParameter(parameterInfo);
        doTest(changeInfo);
    }

    public void testObjectMember() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(0);
        doTest(changeInfo);
    }

    public void testParameterListAddParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.addParameter(new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                                     -1, "l", KotlinBuiltIns.getInstance().getLongType(), null, null, JetValVar.None, null));
        doTest(changeInfo);
    }

    public void testParameterListRemoveParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(changeInfo.getNewParametersCount() - 1);
        doTest(changeInfo);
    }

    public void testParameterListRemoveAllParams() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        for (int i = changeInfo.getNewParametersCount() - 1; i >= 0; i--) {
            changeInfo.removeParameter(i);
        }
        doTest(changeInfo);
    }

    public void testAddNewReceiver() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo parameterInfo =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "_", KotlinBuiltIns.getInstance().getAnyType(), null, psiFactory.createExpression("X(0)"), JetValVar.None, null);
        parameterInfo.setCurrentTypeText("X");
        changeInfo.setReceiverParameterInfo(parameterInfo);
        doTest(changeInfo);
    }

    public void testAddNewReceiverForMember() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo parameterInfo =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "_", KotlinBuiltIns.getInstance().getAnyType(), null, psiFactory.createExpression("X(0)"), JetValVar.None, null);
        parameterInfo.setCurrentTypeText("X");
        changeInfo.setReceiverParameterInfo(parameterInfo);
        doTest(changeInfo);
    }

    public void testAddNewReceiverForMemberConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo parameterInfo =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "_", KotlinBuiltIns.getInstance().getAnyType(), null, psiFactory.createExpression("X(0)"), JetValVar.None, null);
        parameterInfo.setCurrentTypeText("X");
        changeInfo.setReceiverParameterInfo(parameterInfo);
        doTestConflict(changeInfo);
    }

    public void testAddNewReceiverConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetParameterInfo parameterInfo =
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "_", KotlinBuiltIns.getInstance().getAnyType(), null, psiFactory.createExpression("X(0)"), JetValVar.None, null);
        parameterInfo.setCurrentTypeText("X");
        changeInfo.setReceiverParameterInfo(parameterInfo);
        doTestConflict(changeInfo);
    }

    public void testRemoveReceiver() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(0);
        doTest(changeInfo);
    }

    public void testRemoveReceiverForMember() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(0);
        doTest(changeInfo);
    }

    public void testConvertParameterToReceiver1() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[0]);
        doTest(changeInfo);
    }

    public void testConvertParameterToReceiver2() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[1]);
        doTest(changeInfo);
    }

    public void testConvertReceiverToParameter1() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        doTest(changeInfo);
    }

    public void testConvertReceiverToParameter2() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        JetParameterInfo[] parameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(0, parameters[1]);
        changeInfo.setNewParameter(1, parameters[0]);
        doTest(changeInfo);
    }

    public void testConvertParameterToReceiverForMember1() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[0]);
        doTest(changeInfo);
    }

    public void testConvertParameterToReceiverForMember2() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[1]);
        doTest(changeInfo);
    }

    public void testConvertParameterToReceiverForMemberConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[0]);
        doTestConflict(changeInfo);
    }

    public void testConvertReceiverToParameterForMember1() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        doTest(changeInfo);
    }

    public void testConvertReceiverToParameterForMember2() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        JetParameterInfo[] parameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(0, parameters[1]);
        changeInfo.setNewParameter(1, parameters[0]);
        doTest(changeInfo);
    }

    public void testConvertReceiverToParameterWithNameClash() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        doTest(changeInfo);
    }

    public void testConvertReceiverToParameterAndChangeName() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(null);
        changeInfo.getNewParameters()[0].setName("abc");
        doTest(changeInfo);
    }

    public void testChangeReceiver() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[1]);
        doTest(changeInfo);
    }

    public void testChangeReceiverForMember() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[1]);
        doTest(changeInfo);
    }

    public void testChangeParameterTypeWithImport() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setCurrentTypeText("a.Bar");
        doTest(changeInfo);
    }

    public void testSecondaryConstructor() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"foo\""), JetValVar.None, null));
        doTest(changeInfo);
    }

    public void testJavaConstructorInDelegationCall() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters = Arrays.copyOf(newParameters, newParameters.length + 1);

                        PsiType paramType = PsiType.getJavaLangString(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        newParameters[newParameters.length - 1] = new ParameterInfoImpl(-1, "s", paramType, "\"foo\"");

                        return newParameters;
                    }
                }
        );
    }

    public void testPrimaryConstructorByThisRef() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"foo\""), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testPrimaryConstructorBySuperRef() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"foo\""), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testSecondaryConstructorByThisRef() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"foo\""), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testSecondaryConstructorBySuperRef() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "s", KotlinBuiltIns.getInstance().getStringType(), null, psiFactory.createExpression("\"foo\""), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testJavaConstructorBySuperRef() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters = Arrays.copyOf(newParameters, newParameters.length + 1);

                        PsiType paramType = PsiType.getJavaLangString(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        newParameters[newParameters.length - 1] = new ParameterInfoImpl(-1, "s", paramType, "\"foo\"");

                        return newParameters;
                    }
                }
        );
    }

    public void testNoConflictWithReceiverName() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        changeInfo.addParameter(
                new JetParameterInfo(changeInfo.getMethodDescriptor().getBaseDescriptor(),
                                     -1, "i", KotlinBuiltIns.getInstance().getIntType(), null, psiFactory.createExpression("0"), JetValVar.None, null)
        );
        doTest(changeInfo);
    }

    public void testRemoveParameterBeforeLambda() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(1);
        doTest(changeInfo);
    }

    public void testMoveLambdaParameter() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(1, newParameters[2]);
        changeInfo.setNewParameter(2, newParameters[1]);
        doTest(changeInfo);
    }

    public void testConvertLambdaParameterToReceiver() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setReceiverParameterInfo(changeInfo.getNewParameters()[2]);
        doTest(changeInfo);
    }

    public void testRemoveLambdaParameter() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(2);
        doTest(changeInfo);
    }

    public void testRemoveEnumConstructorParameter() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(1);
        doTest(changeInfo);
    }

    public void testRemoveAllEnumConstructorParameters() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        for (int i = changeInfo.getNewParametersCount() - 1; i >= 0; i--) {
            changeInfo.removeParameter(i);
        }
        doTest(changeInfo);
    }

    public void testDoNotApplyPrimarySignatureToSecondaryCalls() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(0, newParameters[1]);
        changeInfo.setNewParameter(1, newParameters[0]);
        doTest(changeInfo);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").getPath() + File.separator;
    }

    private final List<Editor> editors = new ArrayList<Editor>();

    private static final String[] EXTENSIONS = {".kt", ".java"};

    private void configureFiles() throws Exception {
        editors.clear();

        indexLoop:
        for (int i = 0; ; i++) {
            for (String extension : EXTENSIONS) {
                String extraFileName = getTestName(false) + "Before" + (i > 0 ? "." + i : "") + extension;
                File extraFile = new File(getTestDataPath() + extraFileName);
                if (extraFile.exists()) {
                    configureByFile(extraFileName);
                    editors.add(getEditor());
                    continue indexLoop;
                }
            }
            break;
        }

        setActiveEditor(editors.get(0));
    }

    private JetChangeInfo getChangeInfo() throws Exception {
        configureFiles();

        Editor editor = getEditor();
        PsiFile file = getFile();
        Project project = getProject();

        JetElement element = (JetElement) new JetChangeSignatureHandler().findTargetMember(file, editor);
        assertNotNull("Target element is null", element);

        BindingContext bindingContext = ResolvePackage.analyze(element, BodyResolveMode.FULL);
        PsiElement context = file.findElementAt(editor.getCaretModel().getOffset());
        assertNotNull(context);

        FunctionDescriptor functionDescriptor = JetChangeSignatureHandler.findDescriptor(element, project, editor, bindingContext);
        assertNotNull(functionDescriptor);

        return ChangeSignaturePackage.createChangeInfo(
                project, functionDescriptor, JetChangeSignatureHandler.getConfiguration(), bindingContext, context);
    }

    private class JavaRefactoringProvider {
        @NotNull
        String getNewName(@NotNull PsiMethod method) {
            return method.getName();
        }

        @Nullable
        PsiType getNewReturnType(@NotNull PsiMethod method) {
            return method.getReturnType();
        }

        @NotNull
        ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
            PsiParameter[] parameters = method.getParameterList().getParameters();
            ParameterInfoImpl[] parameterInfos = new ParameterInfoImpl[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter parameter = parameters[i];
                parameterInfos[i] = new ParameterInfoImpl(i, parameter.getName(), parameter.getType());
            }
            return parameterInfos;
        }

        @NotNull
        final ChangeSignatureProcessor getProcessor(@NotNull PsiMethod method) {
            return new ChangeSignatureProcessor(
                    getProject(),
                    method,
                    false,
                    VisibilityUtil.getVisibilityModifier(method.getModifierList()),
                    getNewName(method),
                    getNewReturnType(method),
                    getNewParameters(method),
                    new ThrownExceptionInfo[0]);
        }
    }

    private void doJavaTest(JavaRefactoringProvider provider) throws Exception {
        configureFiles();

        PsiElement targetElement = TargetElementUtilBase.findTargetElement(
                getEditor(),
                TargetElementUtilBase.ELEMENT_NAME_ACCEPTED | TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED
        );
        assertTrue("<caret> is not on method name", targetElement instanceof PsiMethod);

        provider.getProcessor((PsiMethod)targetElement).run();

        compareEditorsWithExpectedData();
    }

    private void doTest(JetChangeInfo changeInfo) throws Exception {
        new JetChangeSignatureProcessor(getProject(), changeInfo, "Change signature").run();
        compareEditorsWithExpectedData();
    }

    private void compareEditorsWithExpectedData() throws Exception {
        for (Editor editor : editors) {
            setActiveEditor(editor);
            checkResultByFile(getFile().getName().replace("Before.", "After."));
        }
    }

    private void doTestConflict(JetChangeInfo changeInfo) throws Exception {
        try {
            new JetChangeSignatureProcessor(getProject(), changeInfo, "Change signature").run();
        }
        catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
            List<String> messages = new ArrayList<String>(e.getMessages());
            Collections.sort(messages);
            File conflictsFile = new File(getTestDataPath() + getTestName(false) + "Messages.txt");
            assertSameLinesWithFile(conflictsFile.getAbsolutePath(), StringUtil.join(messages, "\n"));
            return;
        }

        fail("No conflicts found");
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }
}
