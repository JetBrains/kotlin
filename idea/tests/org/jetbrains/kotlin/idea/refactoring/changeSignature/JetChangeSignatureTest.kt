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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtil.ELEMENT_NAME_ACCEPTED
import com.intellij.codeInsight.TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.intellij.refactoring.changeSignature.ThrownExceptionInfo
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ArrayUtil
import com.intellij.util.VisibilityUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinMethodNode
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.dataClassUtils.createComponentName
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.*

class JetChangeSignatureTest : KotlinCodeInsightTestCase() {
    fun testBadSelection() {
        configureByFile(getTestName(false) + "Before.kt")
        TestCase.assertNull(JetChangeSignatureHandler().findTargetMember(file, editor))
    }

    fun testRenameFunction() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "after"
        doTest(changeInfo)
    }

    fun testChangeReturnType() {
        val changeInfo = getChangeInfo()
        changeInfo.newReturnTypeText = "Float"
        doTest(changeInfo)
    }

    fun testAddReturnType() {
        val changeInfo = getChangeInfo()
        changeInfo.newReturnTypeText = "Float"
        doTest(changeInfo)
    }

    fun testRemoveReturnType() {
        val changeInfo = getChangeInfo()
        changeInfo.newReturnTypeText = "Unit"
        doTest(changeInfo)
    }

    fun testChangeConstructorVisibility() {
        val changeInfo = getChangeInfo()

        changeInfo.newVisibility = Visibilities.PROTECTED
        doTest(changeInfo)
    }

    fun testSynthesized() {
        try {
            getChangeInfo()
        }
        catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
            TestCase.assertEquals(JetRefactoringBundle.message("cannot.refactor.synthesized.function", createComponentName(1).asString()), e.message)
            return
        }

        TestCase.fail()
    }

    fun testPreferContainedInClass() {
        val changeInfo = getChangeInfo()
        TestCase.assertEquals("param", changeInfo.newParameters[0].name)
    }

    fun testAddConstructorVisibility() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PROTECTED
        val psiFactory = KtPsiFactory(project)
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "x", BUILT_INS.anyType,
                                            null, psiFactory.createExpression("12"), JetValVar.Val, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testConstructor() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PUBLIC
        changeInfo.newParameters[0].valOrVar = JetValVar.Var
        changeInfo.newParameters[1].valOrVar = JetValVar.None
        changeInfo.newParameters[2].valOrVar = JetValVar.Val
        changeInfo.newParameters[0].name = "_x1"
        changeInfo.newParameters[1].name = "_x2"
        changeInfo.newParameters[2].name = "_x3"
        changeInfo.newParameters[1].currentTypeText = "Float?"
        doTest(changeInfo)
    }

    fun testGenericConstructor() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PUBLIC
        changeInfo.newParameters[0].valOrVar = JetValVar.Var
        changeInfo.newParameters[1].valOrVar = JetValVar.None
        changeInfo.newParameters[2].valOrVar = JetValVar.Val
        changeInfo.newParameters[0].name = "_x1"
        changeInfo.newParameters[1].name = "_x2"
        changeInfo.newParameters[2].name = "_x3"
        changeInfo.newParameters[1].currentTypeText = "Double?"
        doTest(changeInfo)
    }

    fun testConstructorSwapArguments() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[0].name = "_x1"
        changeInfo.newParameters[1].name = "_x2"
        val param = changeInfo.newParameters[0]
        changeInfo.setNewParameter(0, changeInfo.newParameters[2])
        changeInfo.setNewParameter(2, param)
        doTest(changeInfo)
    }

    fun testFunctions() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PUBLIC
        changeInfo.newParameters[0].name = "_x1"
        changeInfo.newParameters[1].name = "_x2"
        changeInfo.newParameters[2].name = "_x3"
        changeInfo.newParameters[1].currentTypeText = "Float?"
        doTest(changeInfo)
    }

    fun testGenericFunctions() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PUBLIC
        changeInfo.newParameters[0].name = "_x1"
        changeInfo.newParameters[1].name = "_x2"
        changeInfo.newParameters[2].name = "_x3"
        changeInfo.newParameters[1].currentTypeText = "Double?"
        doTest(changeInfo)
    }

    fun testExpressionFunction() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[0].name = "x1"
        changeInfo.addParameter(JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                                 -1, "y1", BUILT_INS.intType, null, null, JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testFunctionsAddRemoveArguments() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.INTERNAL
        changeInfo.setNewParameter(2, changeInfo.newParameters[1])
        changeInfo.setNewParameter(1, changeInfo.newParameters[0])
        val psiFactory = KtPsiFactory(project)
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "x0", BUILT_INS.nullableAnyType, null, psiFactory.createExpression("null"), JetValVar.None, null)
        changeInfo.setNewParameter(0, newParameter)
        doTest(changeInfo)
    }

    fun testFakeOverride() {
        val changeInfo = getChangeInfo()
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "i", BUILT_INS.intType, null, null, JetValVar.None, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testFunctionLiteral() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[1].name = "y1"
        changeInfo.addParameter(JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                                 -1, "x", BUILT_INS.anyType, null, null, JetValVar.None, null))
        changeInfo.newReturnTypeText = "Int"
        doTest(changeInfo)
    }

    fun testVarargs() {
        try {
            getChangeInfo()
        }
        catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
            TestCase.assertEquals("Can't refactor the function with variable arguments", e.message)
            return
        }

        TestCase.fail("Exception expected")
    }

    fun testUnmodifiableFromLibrary() {
        doTestUnmodifiableCheck()
    }


    fun testUnmodifiableFromBuiltins() {
        doTestUnmodifiableCheck()
    }

    private fun doTestUnmodifiableCheck() {
        try {
            val changeInfo = getChangeInfo()
            val method = changeInfo.method as KtElement
            val empty = object : JetChangeSignatureConfiguration {
                override fun configure(originalDescriptor: JetMethodDescriptor) = originalDescriptor

                override fun performSilently(affectedFunctions: Collection<PsiElement>) = true

                override fun forcePerformForSelectedFunctionOnly() = false
            }

            runChangeSignature(project, changeInfo.originalBaseFunctionDescriptor, empty, method, "test")
        }
        catch (e: RuntimeException) {
            TestCase.assertTrue(e.message!!.startsWith("Refactoring cannot be"))
            return
        }

        TestCase.fail("Exception expected")
    }

    fun testInnerFunctionsConflict() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "inner2"
        changeInfo.newParameters[0].name = "y"
        doTestConflict(changeInfo)
    }

    fun testMemberFunctionsConflict() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "inner2"
        changeInfo.newParameters[0].name = "y"
        doTestConflict(changeInfo)
    }

    fun testTopLevelFunctionsConflict() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "fun2"
        doTestConflict(changeInfo)
    }

    fun testConstructorsConflict() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[0].name = "_x"
        changeInfo.newParameters[1].name = "_y"
        changeInfo.newParameters[2].name = "_z"
        doTestConflict(changeInfo)
    }

    fun testNoDefaultValuesInOverrides() {
        val changeInfo = getChangeInfo()
        val newParameters = changeInfo.newParameters
        changeInfo.setNewParameter(0, newParameters[1])
        changeInfo.setNewParameter(1, newParameters[0])
        doTest(changeInfo)
    }

    fun testOverridesInEnumEntries() {
        val changeInfo = getChangeInfo()
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "s", BUILT_INS.stringType, null, null, JetValVar.None, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testEnumEntriesWithoutSuperCalls() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null, psiFactory.createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testParameterChangeInOverrides() {
        val changeInfo = getChangeInfo()
        val parameterInfo = changeInfo.newParameters[0]
        parameterInfo.name = "n"
        parameterInfo.currentTypeText = "Int"
        doTest(changeInfo)
    }

    fun testConstructorJavaUsages() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"abc\""), JetValVar.None, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testFunctionJavaUsagesAndOverridesAddParam() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"abc\""),
                                 JetValVar.None, null))
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "o", BUILT_INS.nullableAnyType, null,
                                 psiFactory.createExpression("\"def\""), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testFunctionJavaUsagesAndOverridesChangeNullability() {
        val changeInfo = getChangeInfo()

        val newParameters = changeInfo.newParameters
        newParameters[1].currentTypeText = "String?"
        newParameters[2].currentTypeText = "Any"

        changeInfo.newReturnTypeText = "String?"

        doTest(changeInfo)
    }

    fun testFunctionJavaUsagesAndOverridesChangeTypes() {
        val changeInfo = getChangeInfo()

        val newParameters = changeInfo.newParameters
        newParameters[0].currentTypeText = "String?"
        newParameters[1].currentTypeText = "Int"
        newParameters[2].currentTypeText = "Long?"

        changeInfo.newReturnTypeText = "Any?"

        doTest(changeInfo)
    }

    fun testGenericsWithOverrides() {
        val changeInfo = getChangeInfo()

        val newParameters = changeInfo.newParameters
        newParameters[0].currentTypeText = "List<C>"
        newParameters[1].currentTypeText = "A?"
        newParameters[2].currentTypeText = "U<B>"

        changeInfo.newReturnTypeText = "U<C>?"

        doTest(changeInfo)
    }

    fun testAddReceiverToGenericsWithOverrides() {
        val changeInfo = getChangeInfo()

        val parameterInfo = changeInfo.newParameters[0]
        parameterInfo.currentTypeText = "U<A>"
        changeInfo.receiverParameterInfo = parameterInfo

        doTest(changeInfo)
    }

    fun testJavaMethodKotlinUsages() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewName(method: PsiMethod): String {
                        return "bar"
                    }

                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        return ArrayUtil.remove(super.getNewParameters(method), 1)
                    }
                })
    }

    fun testJavaConstructorKotlinUsages() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        return ArrayUtil.remove(super.getNewParameters(method), 1)
                    }
                })
    }

    fun testSAMAddToEmptyParamList() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val paramType = PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))
                        return arrayOf(ParameterInfoImpl(-1, "s", paramType))
                    }
                })
    }

    fun testSAMAddToSingletonParamList() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val parameter = method.parameterList.parameters[0]
                        val originalParameter = ParameterInfoImpl(0, parameter.name, parameter.type)
                        val newParameter = ParameterInfoImpl(-1, "n", PsiType.INT)

                        return arrayOf(newParameter, originalParameter)
                    }
                })
    }

    fun testSAMAddToNonEmptyParamList() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val originalParameters = super.getNewParameters(method)
                        val newParameters = Arrays.copyOf(originalParameters, originalParameters.size + 1)

                        val paramType = PsiType.getJavaLangObject(psiManager, GlobalSearchScope.allScope(project))
                        newParameters[originalParameters.size] = ParameterInfoImpl(-1, "o", paramType)

                        return newParameters
                    }
                })
    }

    fun testSAMRemoveSingletonParamList() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> = arrayOf()
                }
        )
    }

    fun testSAMRemoveParam() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        return ArrayUtil.remove(super.getNewParameters(method), 0)
                    }
                }
        )
    }

    fun testSAMRenameParam() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val newParameters = super.getNewParameters(method)
                        newParameters[0].name = "p"
                        return newParameters
                    }
                })
    }

    fun testSAMChangeParamType() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val newParameters = super.getNewParameters(method)
                        newParameters[0].setType(PsiType.getJavaLangObject(psiManager, GlobalSearchScope.allScope(project)))
                        return newParameters
                    }
                })
    }

    fun testSAMRenameMethod() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewName(method: PsiMethod): String {
                        return "bar"
                    }
                })
    }

    fun testSAMChangeMethodReturnType() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewReturnType(method: PsiMethod): PsiType {
                        return PsiType.getJavaLangObject(psiManager, GlobalSearchScope.allScope(project))
                    }
                })
    }

    fun testGenericsWithSAMConstructors() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    internal val factory = JavaPsiFacade.getInstance(project).elementFactory

                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val newParameters = super.getNewParameters(method)
                        newParameters[0].setType(factory.createTypeFromText("java.util.List<X<B>>", method.parameterList))
                        newParameters[1].setType(factory.createTypeFromText("X<java.util.Set<A>>", method.parameterList))
                        return newParameters
                    }

                    override fun getNewReturnType(method: PsiMethod): PsiType {
                        return factory.createTypeFromText("X<java.util.List<A>>", method)
                    }
                })
    }

    fun testFunctionRenameJavaUsages() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "bar"
        doTest(changeInfo)
    }

    fun testParameterModifiers() {
        val changeInfo = getChangeInfo()
        changeInfo.addParameter(JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                                 -1, "n", BUILT_INS.intType, null, null, JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testFqNameShortening() {
        val changeInfo = getChangeInfo()
        val parameterInfo = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "s", BUILT_INS.anyType, null, null, JetValVar.None, null)
        parameterInfo.currentTypeText = "kotlin.String"
        changeInfo.addParameter(parameterInfo)
        doTest(changeInfo)
    }

    fun testObjectMember() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(0)
        doTest(changeInfo)
    }

    fun testParameterListAddParam() {
        val changeInfo = getChangeInfo()
        changeInfo.addParameter(JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                                 -1, "l", BUILT_INS.longType, null, null, JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testParameterListRemoveParam() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(changeInfo.getNewParametersCount() - 1)
        doTest(changeInfo)
    }

    fun testParameterListRemoveAllParams() {
        val changeInfo = getChangeInfo()
        for (i in changeInfo.getNewParametersCount() - 1 downTo 0) {
            changeInfo.removeParameter(i)
        }
        doTest(changeInfo)
    }

    fun testAddNewReceiver() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val parameterInfo = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "_", BUILT_INS.anyType, null, psiFactory.createExpression("X(0)"), JetValVar.None, null)
        parameterInfo.currentTypeText = "X"
        changeInfo.receiverParameterInfo = parameterInfo
        doTest(changeInfo)
    }

    fun testAddNewReceiverForMember() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val parameterInfo = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "_", BUILT_INS.anyType, null, psiFactory.createExpression("X(0)"), JetValVar.None, null)
        parameterInfo.currentTypeText = "X"
        changeInfo.receiverParameterInfo = parameterInfo
        doTest(changeInfo)
    }

    fun testAddNewReceiverForMemberConflict() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val parameterInfo = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "_", BUILT_INS.anyType, null, psiFactory.createExpression("X(0)"), JetValVar.None, null)
        parameterInfo.currentTypeText = "X"
        changeInfo.receiverParameterInfo = parameterInfo
        doTestConflict(changeInfo)
    }

    fun testAddNewReceiverConflict() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val parameterInfo = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "_", BUILT_INS.anyType, null, psiFactory.createExpression("X(0)"), JetValVar.None, null)
        parameterInfo.currentTypeText = "X"
        changeInfo.receiverParameterInfo = parameterInfo
        doTestConflict(changeInfo)
    }

    fun testRemoveReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(0)
        doTest(changeInfo)
    }

    fun testRemoveReceiverForMember() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(0)
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiver1() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiver2() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[1]
        doTest(changeInfo)
    }

    fun testConvertReceiverToParameter1() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testConvertReceiverToParameter2() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        val parameters = changeInfo.newParameters
        changeInfo.setNewParameter(0, parameters[1])
        changeInfo.setNewParameter(1, parameters[0])
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiverForMember1() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiverForMember2() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[1]
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiverForMemberConflict() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTestConflict(changeInfo)
    }

    fun testConvertReceiverToParameterForMember1() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testConvertReceiverToParameterForMember2() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        val parameters = changeInfo.newParameters
        changeInfo.setNewParameter(0, parameters[1])
        changeInfo.setNewParameter(1, parameters[0])
        doTest(changeInfo)
    }

    fun testConvertReceiverToParameterWithNameClash() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testConvertReceiverToParameterAndChangeName() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        changeInfo.newParameters[0].name = "abc"
        doTest(changeInfo)
    }

    fun testChangeReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[1]
        doTest(changeInfo)
    }

    fun testChangeReceiverForMember() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[1]
        doTest(changeInfo)
    }

    fun testChangeParameterTypeWithImport() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[0].currentTypeText = "a.Bar"
        doTest(changeInfo)
    }

    fun testSecondaryConstructor() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"foo\""),
                                 JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testJavaConstructorInDelegationCall() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        var newParameters = super.getNewParameters(method)
                        newParameters = Arrays.copyOf(newParameters, newParameters.size + 1)

                        val paramType = PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))
                        newParameters[newParameters.size - 1] = ParameterInfoImpl(-1, "s", paramType, "\"foo\"")

                        return newParameters
                    }
                })
    }

    fun testPrimaryConstructorByThisRef() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"foo\""), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testPrimaryConstructorBySuperRef() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"foo\""), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testSecondaryConstructorByThisRef() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"foo\""), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testSecondaryConstructorBySuperRef() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "s", BUILT_INS.stringType, null, psiFactory.createExpression("\"foo\""), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testJavaConstructorBySuperRef() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        var newParameters = super.getNewParameters(method)
                        newParameters = Arrays.copyOf(newParameters, newParameters.size + 1)

                        val paramType = PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))
                        newParameters[newParameters.size - 1] = ParameterInfoImpl(-1, "s", paramType, "\"foo\"")

                        return newParameters
                    }
                })
    }

    fun testNoConflictWithReceiverName() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        changeInfo.addParameter(
                JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                 -1, "i", BUILT_INS.intType, null, psiFactory.createExpression("0"), JetValVar.None, null))
        doTest(changeInfo)
    }

    fun testRemoveParameterBeforeLambda() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(1)
        doTest(changeInfo)
    }

    fun testMoveLambdaParameter() {
        val changeInfo = getChangeInfo()
        val newParameters = changeInfo.newParameters
        changeInfo.setNewParameter(1, newParameters[2])
        changeInfo.setNewParameter(2, newParameters[1])
        doTest(changeInfo)
    }

    fun testConvertLambdaParameterToReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[2]
        doTest(changeInfo)
    }

    fun testRemoveLambdaParameter() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(2)
        doTest(changeInfo)
    }

    fun testRemoveEnumConstructorParameter() {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(1)
        doTest(changeInfo)
    }

    fun testRemoveAllEnumConstructorParameters() {
        val changeInfo = getChangeInfo()
        for (i in changeInfo.getNewParametersCount() - 1 downTo 0) {
            changeInfo.removeParameter(i)
        }
        doTest(changeInfo)
    }

    fun testDoNotApplyPrimarySignatureToSecondaryCalls() {
        val changeInfo = getChangeInfo()
        val newParameters = changeInfo.newParameters
        changeInfo.setNewParameter(0, newParameters[1])
        changeInfo.setNewParameter(1, newParameters[0])
        doTest(changeInfo)
    }

    fun testConvertToExtensionAndRename() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        changeInfo.newName = "foo1"
        doTest(changeInfo)
    }

    fun testRenameExtensionParameter() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[1].name = "b"
        doTest(changeInfo)
    }

    fun testConvertParameterToReceiverAddParens() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTest(changeInfo)
    }

    fun testThisReplacement() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testPrimaryConstructorByRef() {
        val changeInfo = getChangeInfo()
        val psiFactory = KtPsiFactory(project)
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            psiFactory.createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)
        doTest(changeInfo)
    }

    fun testReceiverToParameterExplicitReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testReceiverToParameterImplicitReceivers() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testParameterToReceiverExplicitReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTest(changeInfo)
    }

    fun testParameterToReceiverImplicitReceivers() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = changeInfo.newParameters[0]
        doTest(changeInfo)
    }

    fun testJavaMethodOverridesReplaceParam() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewReturnType(method: PsiMethod): PsiType {
                        return PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))
                    }

                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val newParameters = super.getNewParameters(method)
                        newParameters[0] = ParameterInfoImpl(-1, "x", PsiType.INT, "1")
                        return newParameters
                    }
                })
    }

    fun testJavaMethodOverridesChangeParam() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewReturnType(method: PsiMethod): PsiType {
                        return PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project))
                    }

                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        val newParameters = super.getNewParameters(method)
                        newParameters[0].name = "x"
                        newParameters[0].setType(PsiType.INT)
                        return newParameters
                    }
                })
    }

    fun testChangeProperty() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "s"
        changeInfo.newReturnTypeText = "String"
        doTest(changeInfo)
    }

    fun testAddPropertyReceiverConflict() {
        val changeInfo = getChangeInfo()
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "receiver", BUILT_INS.stringType, null,
                                            KtPsiFactory(project).createExpression("\"\""), JetValVar.None, null)
        changeInfo.receiverParameterInfo = newParameter
        doTestConflict(changeInfo)
    }

    fun testAddPropertyReceiver() {
        val changeInfo = getChangeInfo()
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "receiver", BUILT_INS.stringType, null,
                                            KtPsiFactory(project).createExpression("\"\""), JetValVar.None, null)
        changeInfo.receiverParameterInfo = newParameter
        doTest(changeInfo)
    }

    fun testChangePropertyReceiver() {
        val changeInfo = getChangeInfo()
        //noinspection ConstantConditions
        changeInfo.receiverParameterInfo!!.currentTypeText = "Int"
        doTest(changeInfo)
    }

    fun testRemovePropertyReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testAddTopLevelPropertyReceiver() {
        val changeInfo = getChangeInfo()
        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "receiver", null, null,
                                            KtPsiFactory(project).createExpression("A()"), JetValVar.None, null)
        newParameter.currentTypeText = "test.A"
        changeInfo.receiverParameterInfo = newParameter
        doTest(changeInfo)
    }

    fun testChangeTopLevelPropertyReceiver() {
        val changeInfo = getChangeInfo()
        //noinspection ConstantConditions
        changeInfo.receiverParameterInfo!!.currentTypeText = "String"
        doTest(changeInfo)
    }

    fun testRemoveTopLevelPropertyReceiver() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testChangeClassParameter() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "s"
        changeInfo.newReturnTypeText = "String"
        doTest(changeInfo)
    }

    fun testParameterPropagation() {
        val changeInfo = getChangeInfo()

        val newParameter1 = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "n", null, null,
                                             KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        newParameter1.currentTypeText = "kotlin.Int"
        changeInfo.addParameter(newParameter1)

        val newParameter2 = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                             -1, "s", null, null,
                                             KtPsiFactory(project).createExpression("\"abc\""), JetValVar.None, null)
        newParameter2.currentTypeText = "kotlin.String"
        changeInfo.addParameter(newParameter2)

        val classA = KotlinFullClassNameIndex.getInstance().get("A", project, GlobalSearchScope.allScope(project)).iterator().next()
        val functionBar = classA.declarations.first { it is KtNamedFunction && it.name == "bar" }
        val functionTest = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("test", project, GlobalSearchScope.allScope(project)).iterator().next()

        changeInfo.primaryPropagationTargets = Arrays.asList<KtDeclaration>(functionBar, functionTest)

        doTest(changeInfo)
    }

    fun testJavaParameterPropagation() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
                        return arrayOf(
                                ParameterInfoImpl(-1, "n", PsiType.INT, "1"),
                                ParameterInfoImpl(-1, "s", PsiType.getJavaLangString(psiManager, GlobalSearchScope.allScope(project)), "\"abc\"")
                        )
                    }

                    override fun getParameterPropagationTargets(method: PsiMethod): Set<PsiMethod> {
                        val classA = JavaFullClassNameIndex.getInstance()
                                .get("A".hashCode(), project, GlobalSearchScope.allScope(project))
                                .first { it.name == "A" }
                        val methodBar = classA.methods.first { it.name == "bar" }
                        val functionTest = KotlinTopLevelFunctionFqnNameIndex.getInstance()
                                .get("test", project, GlobalSearchScope.allScope(project))
                                .first()
                        return setOf(methodBar, functionTest.getRepresentativeLightMethod()!!)
                    }
                })
    }

    fun testPropagateWithParameterDuplication() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val functionBar = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, GlobalSearchScope.allScope(project)).iterator().next()

        changeInfo.primaryPropagationTargets = listOf(functionBar)

        doTestConflict(changeInfo)
    }

    fun testPropagateWithVariableDuplication() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val functionBar = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, GlobalSearchScope.allScope(project)).iterator().next()

        changeInfo.primaryPropagationTargets = listOf(functionBar)

        doTestConflict(changeInfo)
    }

    fun testPropagateWithThisQualificationInClassMember() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val classA = KotlinFullClassNameIndex.getInstance().get("A", project, GlobalSearchScope.allScope(project)).iterator().next()
        val functionBar = classA.declarations.first { it is KtNamedFunction && it.name == "bar" }
        changeInfo.primaryPropagationTargets = listOf<KtDeclaration>(functionBar)

        doTest(changeInfo)
    }

    fun testPropagateWithThisQualificationInExtension() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val functionBar = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, GlobalSearchScope.allScope(project)).iterator().next()

        changeInfo.primaryPropagationTargets = listOf(functionBar)

        doTest(changeInfo)
    }

    fun testJavaConstructorParameterPropagation() {
        doJavaTest(
                object : JavaRefactoringProvider() {
                    override fun getNewParameters(method: PsiMethod) = arrayOf(ParameterInfoImpl(-1, "n", PsiType.INT, "1"))

                    override fun getParameterPropagationTargets(method: PsiMethod) = findCallers(method)
                }
        )
    }

    fun testPrimaryConstructorParameterPropagation() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val constructor = changeInfo.method.getRepresentativeLightMethod()
        assert(constructor != null)
        changeInfo.primaryPropagationTargets = findCallers(constructor!!)

        doTest(changeInfo)
    }

    fun testSecondaryConstructorParameterPropagation() {
        val changeInfo = getChangeInfo()

        val newParameter = JetParameterInfo(changeInfo.methodDescriptor.baseDescriptor,
                                            -1, "n", BUILT_INS.intType, null,
                                            KtPsiFactory(project).createExpression("1"), JetValVar.None, null)
        changeInfo.addParameter(newParameter)

        val constructor = changeInfo.method.getRepresentativeLightMethod()
        assert(constructor != null)
        changeInfo.primaryPropagationTargets = findCallers(constructor!!)

        doTest(changeInfo)
    }

    fun testJavaMethodOverridesOmitUnitType() {
        doJavaTest(JavaRefactoringProvider())
    }

    fun testOverrideInAnonymousObjectWithTypeParameters() {
        val changeInfo = getChangeInfo()
        changeInfo.newName = "bar"
        doTest(changeInfo)
    }

    fun testMakePrimaryConstructorPrivateNoParams() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PRIVATE
        doTest(changeInfo)
    }

    fun testMakePrimaryConstructorPublic() {
        val changeInfo = getChangeInfo()
        changeInfo.newVisibility = Visibilities.PUBLIC
        doTest(changeInfo)
    }

    fun testRenameExtensionParameterWithNamedArgs() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[2].name = "bb"
        doTest(changeInfo)
    }

    fun testImplicitThisToParameterWithChangedType() {
        val changeInfo = getChangeInfo()
        changeInfo.receiverParameterInfo!!.currentTypeText = "Older"
        changeInfo.receiverParameterInfo = null
        doTest(changeInfo)
    }

    fun testJvmOverloadedRenameParameter() {
        val changeInfo = getChangeInfo()
        changeInfo.newParameters[0].name = "aa"
        doTest(changeInfo)
    }

    fun testJvmOverloadedSwapParams1() {
        val changeInfo = getChangeInfo()
        val param = changeInfo.newParameters[1]
        changeInfo.setNewParameter(1, changeInfo.newParameters[2])
        changeInfo.setNewParameter(2, param)
        doTest(changeInfo)
    }

    fun testJvmOverloadedSwapParams2() {
        val changeInfo = getChangeInfo()
        val param = changeInfo.newParameters[0]
        changeInfo.setNewParameter(0, changeInfo.newParameters[2])
        changeInfo.setNewParameter(2, param)
        doTest(changeInfo)
    }

    private fun doTestJvmOverloadedAddDefault(index: Int) {
        val changeInfo = getChangeInfo()
        val defaultValue = KtPsiFactory(project).createExpression("2")
        val descriptor = changeInfo.methodDescriptor.baseDescriptor
        changeInfo.addParameter(JetParameterInfo(descriptor, -1, "n", BUILT_INS.intType, defaultValue, defaultValue), index)
        doTest(changeInfo)
    }

    private fun doTestJvmOverloadedAddNonDefault(index: Int) {
        val changeInfo = getChangeInfo()
        val defaultValue = KtPsiFactory(project).createExpression("2")
        val descriptor = changeInfo.methodDescriptor.baseDescriptor
        changeInfo.addParameter(JetParameterInfo(descriptor, -1, "n", BUILT_INS.intType, null, defaultValue), index)
        doTest(changeInfo)
    }

    private fun doTestRemoveAt(index: Int) {
        val changeInfo = getChangeInfo()
        changeInfo.removeParameter(if (index >= 0) index else changeInfo.getNewParametersCount() - 1)
        doTest(changeInfo)
    }

    fun testJvmOverloadedAddDefault1() {
        doTestJvmOverloadedAddDefault(0)
    }

    fun testJvmOverloadedAddDefault2() {
        doTestJvmOverloadedAddDefault(1)
    }

    fun testJvmOverloadedAddDefault3() {
        doTestJvmOverloadedAddDefault(-1)
    }

    fun testJvmOverloadedAddNonDefault1() {
        doTestJvmOverloadedAddNonDefault(0)
    }

    fun testJvmOverloadedAddNonDefault2() {
        doTestJvmOverloadedAddNonDefault(1)
    }

    fun testJvmOverloadedAddNonDefault3() {
        doTestJvmOverloadedAddNonDefault(-1)
    }

    fun testJvmOverloadedRemoveDefault1() {
        doTestRemoveAt(0)
    }

    fun testJvmOverloadedRemoveDefault2() {
        doTestRemoveAt(1)
    }

    fun testJvmOverloadedRemoveDefault3() {
        doTestRemoveAt(-1)
    }

    fun testJvmOverloadedRemoveNonDefault1() {
        doTestRemoveAt(0)
    }

    fun testJvmOverloadedRemoveNonDefault2() {
        doTestRemoveAt(1)
    }

    fun testJvmOverloadedRemoveNonDefault3() {
        doTestRemoveAt(-1)
    }

    fun testJvmOverloadedConstructorSwapParams() {
        testJvmOverloadedSwapParams1()
    }

    private var editors: MutableList<Editor>? = null

    override fun setUp() {
        super.setUp()
        editors = ArrayList<Editor>()
        ConfigLibraryUtil.configureKotlinRuntime(module)
    }

    override fun tearDown() {
        ConfigLibraryUtil.unConfigureKotlinRuntime(module)
        editors!!.clear()
        editors = null
        super.tearDown()
    }

    private fun findCallers(method: PsiMethod): LinkedHashSet<PsiMethod> {
        val rootNode = KotlinMethodNode(method,
                                        HashSet<PsiElement>(),
                                        project,
                                        object : Runnable {
                                            override fun run() {

                                            }
                                        })
        val callers = LinkedHashSet<PsiMethod>()
        for (i in 0..rootNode.childCount - 1) {
            val element = (rootNode.getChildAt(i) as KotlinMethodNode).method
            callers.addAll(element.toLightMethods())
        }
        return callers
    }

    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").path + File.separator
    }

    private fun configureFiles() {
        editors!!.clear()

        var i = 0
        indexLoop@ while (true) {
            for (extension in EXTENSIONS) {
                val extraFileName = getTestName(false) + "Before" + (if (i > 0) "." + i else "") + extension
                val extraFile = File(testDataPath + extraFileName)
                if (extraFile.exists()) {
                    configureByFile(extraFileName)
                    editors!!.add(editor)
                    i++
                    continue@indexLoop
                }
            }
            break
        }

        setActiveEditor(editors!![0])
    }

    private fun getChangeInfo(): JetChangeInfo {
        configureFiles()

        val editor = editor
        val file = file
        val project = project

        val element = JetChangeSignatureHandler().findTargetMember(file, editor) as KtElement?
        TestCase.assertNotNull("Target element is null", element)

        val bindingContext = element!!.analyze(BodyResolveMode.FULL)
        val context = file.findElementAt(editor.caretModel.offset)
        TestCase.assertNotNull(context)

        val callableDescriptor = JetChangeSignatureHandler.findDescriptor(element, project, editor, bindingContext)
        TestCase.assertNotNull(callableDescriptor)

        return createChangeInfo(project, callableDescriptor!!, JetChangeSignatureConfiguration.Empty, context!!)!!
    }

    private open inner class JavaRefactoringProvider {
        open fun getNewName(method: PsiMethod) = method.name

        open fun getNewReturnType(method: PsiMethod) = method.returnType ?: PsiType.VOID

        open fun getNewParameters(method: PsiMethod): Array<ParameterInfoImpl> {
            val parameters = method.parameterList.parameters
            return Array(parameters.size) { i ->
                val parameter = parameters[i]
                ParameterInfoImpl(i, parameter.name, parameter.type)
            }
        }

        open fun getParameterPropagationTargets(method: PsiMethod) = emptySet<PsiMethod>()

        fun getProcessor(method: PsiMethod): ChangeSignatureProcessor {
            return ChangeSignatureProcessor(
                    project,
                    method,
                    false,
                    VisibilityUtil.getVisibilityModifier(method.modifierList),
                    getNewName(method),
                    CanonicalTypes.createTypeWrapper(getNewReturnType(method)),
                    getNewParameters(method),
                    arrayOfNulls<ThrownExceptionInfo>(0),
                    getParameterPropagationTargets(method),
                    emptySet<PsiMethod>()
            )
        }
    }

    private fun doJavaTest(provider: JavaRefactoringProvider) {
        configureFiles()

        val targetElement = TargetElementUtil.findTargetElement(editor, ELEMENT_NAME_ACCEPTED or REFERENCED_ELEMENT_ACCEPTED)
        TestCase.assertTrue("<caret> is not on method name", targetElement is PsiMethod)

        provider.getProcessor(targetElement as PsiMethod).run()

        compareEditorsWithExpectedData()
    }

    private fun doTest(changeInfo: JetChangeInfo) {
        JetChangeSignatureProcessor(project, changeInfo, "Change signature").run()
        compareEditorsWithExpectedData()
    }

    private fun compareEditorsWithExpectedData() {
        //noinspection ConstantConditions
        val checkErrorsAfter = InTextDirectivesUtils.isDirectiveDefined(getPsiFile(editors!![0].document)!!.text,
                                                                        "// CHECK_ERRORS_AFTER")
        for (editor in editors!!) {
            setActiveEditor(editor)
            val currentFile = file
            val afterFilePath = currentFile.name.replace("Before.", "After.")
            try {
                checkResultByFile(afterFilePath)
            }
            catch (e: ComparisonFailure) {
                KotlinTestUtils.assertEqualsToFile(File(testDataPath + afterFilePath), getEditor())
            }

            if (checkErrorsAfter && currentFile is KtFile) {
                DirectiveBasedActionUtils.checkForUnexpectedErrors(currentFile)
            }
        }
    }

    private fun doTestConflict(changeInfo: JetChangeInfo) {
        try {
            JetChangeSignatureProcessor(project, changeInfo, "Change signature").run()
        }
        catch (e: BaseRefactoringProcessor.ConflictsInTestsException) {
            val messages = ArrayList(e.messages)
            Collections.sort(messages)
            val conflictsFile = File(testDataPath + getTestName(false) + "Messages.txt")
            UsefulTestCase.assertSameLinesWithFile(conflictsFile.absolutePath, StringUtil.join(messages, "\n"))
            return
        }

        TestCase.fail("No conflicts found")
    }

    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()

    companion object {
        private val BUILT_INS = JvmPlatform.builtIns
        private val EXTENSIONS = arrayOf(".kt", ".java")
    }
}
