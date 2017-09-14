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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.VisibilityUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinMethodNode
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinChangeSignatureDialog.Companion.getTypeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinChangeSignatureDialog.Companion.getTypeCodeFragmentContext
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.util.*

class KotlinChangeSignatureTest : KotlinLightCodeInsightFixtureTestCase() {
    companion object {
        internal val BUILT_INS = DefaultBuiltIns.Instance
        private val EXTENSIONS = arrayOf(".kt", ".java")
    }

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").path + File.separator

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun tearDown() {
        files = emptyList()
        psiFiles = PsiFile.EMPTY_ARRAY
        super.tearDown()
    }

    lateinit var files: List<String>
    lateinit var psiFiles: Array<PsiFile>

    private fun findCallers(method: PsiMethod): LinkedHashSet<PsiMethod> {
        val root = KotlinMethodNode(method, HashSet(), project, Runnable { })
        return (0..root.childCount - 1).flatMapTo(LinkedHashSet<PsiMethod>()) {
            (root.getChildAt(it) as KotlinMethodNode).method.toLightMethods()
        }
    }

    private fun configureFiles() {
        val fileList = mutableListOf<String>()
        var i = 0
        indexLoop@ while (true) {
            for (extension in EXTENSIONS) {
                val extraFileName = getTestName(false) + "Before" + (if (i > 0) "." + i else "") + extension
                val extraFile = File(testDataPath + extraFileName)
                if (extraFile.exists()) {
                    fileList.add(extraFileName)
                    i++
                    continue@indexLoop
                }
            }
            break
        }

        psiFiles = myFixture.configureByFiles(*fileList.toTypedArray())
        files = fileList
    }

    private fun createChangeInfo(): KotlinChangeInfo {
        configureFiles()

        val element = (KotlinChangeSignatureHandler().findTargetMember(file, editor) as KtElement?).sure { "Target element is null" }
        val context = file
                .findElementAt(editor.caretModel.offset)
                ?.getNonStrictParentOfType<KtElement>()
                .sure { "Context element is null" }
        val bindingContext = element.analyze(BodyResolveMode.FULL)
        val callableDescriptor = KotlinChangeSignatureHandler
                .findDescriptor(element, project, editor, bindingContext)
                .sure { "Target descriptor is null" }
        return createChangeInfo(project, callableDescriptor, KotlinChangeSignatureConfiguration.Empty, context)!!
    }

    private fun doTest(configure: KotlinChangeInfo.() -> Unit = {}) {
        KotlinChangeSignatureProcessor(project, createChangeInfo().apply { configure() }, "Change signature").run()

        compareEditorsWithExpectedData()
    }

    private fun runAndCheckConflicts(testAction: () -> Unit) {
        try {
            testAction()
            TestCase.fail("No conflicts found")
        }
        catch (e: Throwable) {
            val message = when {
                e is BaseRefactoringProcessor.ConflictsInTestsException -> StringUtil.join(e.messages.sorted(), "\n")
                e is CommonRefactoringUtil.RefactoringErrorHintException -> e.message
                e is RuntimeException && e.message!!.startsWith("Refactoring cannot be performed") -> e.message
                else -> throw e
            }
            val conflictsFile = File(testDataPath + getTestName(false) + "Messages.txt")
            UsefulTestCase.assertSameLinesWithFile(conflictsFile.absolutePath, message)
        }
    }

    private fun doTestConflict(configure: KotlinChangeInfo.() -> Unit = {}) = runAndCheckConflicts { doTest(configure) }

    private fun doTestUnmodifiable(configure: KotlinChangeInfo.() -> Unit = {}) {
        try {
            doTest(configure)
            TestCase.fail("No conflicts found")
        }
        catch (e: RuntimeException) {
            if ((e.message ?: "").contains("Cannot modify file")) return

            val message = when {
                e is BaseRefactoringProcessor.ConflictsInTestsException -> StringUtil.join(e.messages.sorted(), "\n")
                e is CommonRefactoringUtil.RefactoringErrorHintException -> e.message
                e.message!!.startsWith("Refactoring cannot be performed") -> e.message
                else -> throw e
            }
            val conflictsFile = File(testDataPath + getTestName(false) + "Messages.txt")
            UsefulTestCase.assertSameLinesWithFile(conflictsFile.absolutePath, message)
        }
    }

    private class JavaRefactoringConfiguration(val method: PsiMethod) {
        val project = method.project

        var newName = method.name
        var newReturnType = method.returnType ?: PsiType.VOID
        val newParameters = ArrayList<ParameterInfoImpl>()
        val parameterPropagationTargets = LinkedHashSet<PsiMethod>()

        val psiFactory: PsiElementFactory
            get() = JavaPsiFacade.getInstance(project).elementFactory

        val objectPsiType: PsiType
            get() = PsiType.getJavaLangObject(method.manager, project.allScope())

        val stringPsiType: PsiType
            get() = PsiType.getJavaLangString(method.manager, project.allScope())

        init {
            method.parameterList.parameters
                    .withIndex()
                    .mapTo(newParameters) {
                        val (i, param) = it
                        ParameterInfoImpl(i, param.name, param.type)
                    }
        }

        fun createProcessor(): ChangeSignatureProcessor {
            return ChangeSignatureProcessor(
                    project,
                    method,
                    false,
                    VisibilityUtil.getVisibilityModifier(method.modifierList),
                    newName,
                    CanonicalTypes.createTypeWrapper(newReturnType),
                    newParameters.toTypedArray(),
                    arrayOf(),
                    parameterPropagationTargets,
                    emptySet<PsiMethod>()
            )
        }
    }

    private fun doJavaTest(configure: JavaRefactoringConfiguration.() -> Unit) {
        configureFiles()

        val targetElement = TargetElementUtil.findTargetElement(editor, ELEMENT_NAME_ACCEPTED or REFERENCED_ELEMENT_ACCEPTED)
        val targetMethod = (targetElement as? PsiMethod).sure { "<caret> is not on method name" }

        JavaRefactoringConfiguration(targetMethod)
                .apply { configure() }
                .createProcessor()
                .run()

        compareEditorsWithExpectedData()
    }

    private fun doJavaTestConflict(configure: JavaRefactoringConfiguration.() -> Unit) = runAndCheckConflicts { doJavaTest(configure) }

    private fun compareEditorsWithExpectedData() {
        //noinspection ConstantConditions
        val checkErrorsAfter = InTextDirectivesUtils.isDirectiveDefined(file!!.text, "// CHECK_ERRORS_AFTER")
        for ((file, psiFile) in files zip psiFiles) {
            val afterFilePath = file.replace("Before.", "After.")
            try {
                myFixture.checkResultByFile(file, afterFilePath, true)
            }
            catch (e: ComparisonFailure) {
                KotlinTestUtils.assertEqualsToFile(File(testDataPath + afterFilePath), psiFile.text)
            }

            if (checkErrorsAfter && psiFile is KtFile) {
                DirectiveBasedActionUtils.checkForUnexpectedErrors(psiFile)
            }
        }
    }

    private fun KotlinChangeInfo.swapParameters(i: Int, j: Int) {
        val newParameters = newParameters
        val temp = newParameters[i]
        setNewParameter(i, newParameters[j])
        setNewParameter(j, temp)
    }

    private fun KotlinChangeInfo.resolveType(text: String, isCovariant: Boolean, forPreview: Boolean): KotlinTypeInfo {
        val codeFragment = KtPsiFactory(project).createTypeCodeFragment(text, getTypeCodeFragmentContext(context))
        return codeFragment.getTypeInfo(isCovariant, forPreview)
    }

    // --------------------------------- Tests ---------------------------------

    fun testBadSelection() {
        myFixture.configureByFile(getTestName(false) + "Before.kt")
        TestCase.assertNull(KotlinChangeSignatureHandler().findTargetMember(file, editor))
    }

    fun testSynthesized() = doTestConflict()

    fun testPreferContainedInClass() = TestCase.assertEquals("param", createChangeInfo().newParameters[0].name)

    fun testRenameFunction() = doTest { newName = "after" }

    fun testChangeReturnType() = doTest { newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.floatType) }

    fun testAddReturnType() = doTest { newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.floatType) }

    fun testRemoveReturnType() = doTest { newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.unitType) }

    fun testChangeConstructorVisibility() = doTest { newVisibility = Visibilities.PROTECTED }

    fun testAddConstructorVisibility() = doTest {
        newVisibility = Visibilities.PROTECTED

        val newParameter = KotlinParameterInfo(
                callableDescriptor = originalBaseFunctionDescriptor,
                name = "x",
                originalTypeInfo = KotlinTypeInfo(false, BUILT_INS.anyType),
                defaultValueForCall = KtPsiFactory(project).createExpression("12"),
                valOrVar = KotlinValVar.Val
        )
        addParameter(newParameter)
    }

    fun testConstructor() = doTest {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].valOrVar = KotlinValVar.Var
        newParameters[1].valOrVar = KotlinValVar.None
        newParameters[2].valOrVar = KotlinValVar.Val

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.floatType.makeNullable())
    }

    fun testGenericConstructor() = doTest {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].valOrVar = KotlinValVar.Var
        newParameters[1].valOrVar = KotlinValVar.None
        newParameters[2].valOrVar = KotlinValVar.Val

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.doubleType.makeNullable())
    }

    fun testConstructorSwapArguments() = doTest {
        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"

        swapParameters(0, 2)
    }

    fun testFunctions() = doTest {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.floatType.makeNullable())
    }

    fun testGenericFunctions() = doTest() {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.doubleType.makeNullable())
    }

    fun testExpressionFunction() = doTest {
        newParameters[0].name = "x1"

        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "y1", KotlinTypeInfo(false, BUILT_INS.intType)))
    }

    fun testFunctionsAddRemoveArguments() = doTest {
        newVisibility = Visibilities.INTERNAL

        val defaultValueForCall = KtPsiFactory(project).createExpression("null")
        val newParameters = newParameters
        setNewParameter(2, newParameters[1])
        setNewParameter(1, newParameters[0])
        setNewParameter(0, KotlinParameterInfo(originalBaseFunctionDescriptor,
                                               -1,
                                               "x0",
                                               KotlinTypeInfo(false, BUILT_INS.nullableAnyType),
                                               null,
                                               defaultValueForCall))
    }

    fun testFakeOverride() = doTest {
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "i", KotlinTypeInfo(false, BUILT_INS.intType)))
    }

    fun testFunctionLiteral() = doTest {
        newParameters[1].name = "y1"
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "x", KotlinTypeInfo(false, BUILT_INS.anyType)))

        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.intType)
    }

    fun testVarargs() = doTestConflict()

    fun testUnmodifiableFromLibrary() = doTestUnmodifiable()

    fun testUnmodifiableFromBuiltins() = doTestUnmodifiable()

    fun testInnerFunctionsConflict() = doTestConflict {
        newName = "inner2"
        newParameters[0].name = "y"
    }

    fun testMemberFunctionsConflict() = doTestConflict {
        newName = "inner2"
        newParameters[0].name = "y"
    }

    fun testTopLevelFunctionsConflict() = doTestConflict { newName = "fun2" }

    fun testConstructorsConflict() = doTestConflict {
        newParameters[0].name = "_x"
        newParameters[1].name = "_y"
        newParameters[2].name = "_z"
    }

    fun testNoDefaultValuesInOverrides() = doTest { swapParameters(0, 1) }

    fun testOverridesInEnumEntries() = doTest {
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType)))
    }

    fun testEnumEntriesWithoutSuperCalls() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor,
                                         -1,
                                         "n",
                                         KotlinTypeInfo(false, BUILT_INS.intType),
                                         null,
                                         defaultValueForCall))
    }

    fun testParameterChangeInOverrides() = doTest {
        newParameters[0].name = "n"
        newParameters[0].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.intType)
    }

    fun testConstructorJavaUsages() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"abc\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor,
                                         -1,
                                         "s",
                                         KotlinTypeInfo(false, BUILT_INS.stringType),
                                         null,
                                         defaultValueForCall))
    }

    fun testFunctionJavaUsagesAndOverridesAddParam() = doTest {
        val psiFactory = KtPsiFactory(project)
        val defaultValueForCall1 = psiFactory.createExpression("\"abc\"")
        val defaultValueForCall2 = psiFactory.createExpression("\"def\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor,
                                         -1,
                                         "s",
                                         KotlinTypeInfo(false, BUILT_INS.stringType),
                                         null,
                                         defaultValueForCall1))
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor,
                                         -1,
                                         "o",
                                         KotlinTypeInfo(false, BUILT_INS.nullableAnyType),
                                         null,
                                         defaultValueForCall2))
    }

    fun testFunctionJavaUsagesAndOverridesChangeNullability() = doTest {
        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.stringType.makeNullable())
        newParameters[2].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.anyType)

        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.stringType.makeNullable())
    }

    fun testFunctionJavaUsagesAndOverridesChangeTypes() = doTest {
        newParameters[0].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.stringType.makeNullable())
        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.intType)
        newParameters[2].currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.longType.makeNullable())

        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.nullableAnyType)
    }

    fun testGenericsWithOverrides() = doTest {
        newParameters[0].currentTypeInfo = KotlinTypeInfo(false, null, "List<C>")
        newParameters[1].currentTypeInfo = KotlinTypeInfo(false, null, "A?")
        newParameters[2].currentTypeInfo = KotlinTypeInfo(false, null, "U<B>")

        newReturnTypeInfo = KotlinTypeInfo(true, null, "U<C>?")
    }

    fun testAddReceiverToGenericsWithOverrides() = doTest {
        val parameterInfo = newParameters[0]
        parameterInfo.currentTypeInfo = KotlinTypeInfo(false, null, "U<A>")
        receiverParameterInfo = parameterInfo
    }

    fun testJavaMethodKotlinUsages() = doJavaTest {
        newName = "bar"
        newParameters.removeAt(1)
    }

    fun testJavaConstructorKotlinUsages() = doJavaTest { newParameters.removeAt(1) }

    fun testSAMAddToEmptyParamList() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType)) }

    fun testSAMAddToSingletonParamList() = doJavaTest { newParameters.add(0, ParameterInfoImpl(-1, "n", PsiType.INT)) }

    fun testSAMAddToNonEmptyParamList() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "o", objectPsiType)) }

    fun testSAMRemoveSingletonParamList() = doJavaTest { newParameters.clear() }

    fun testSAMRemoveParam() = doJavaTest { newParameters.removeAt(0) }

    fun testSAMRenameParam() = doJavaTest { newParameters[0].name = "p" }

    fun testSAMChangeParamType() = doJavaTest { newParameters[0].setType(objectPsiType) }

    fun testSAMRenameMethod() = doJavaTest { newName = "bar" }

    fun testSAMChangeMethodReturnType() = doJavaTest { newReturnType = objectPsiType }

    fun testGenericsWithSAMConstructors() = doJavaTest {
        newParameters[0].setType(psiFactory.createTypeFromText("java.util.List<X<B>>", method.parameterList))
        newParameters[1].setType(psiFactory.createTypeFromText("X<java.util.Set<A>>", method.parameterList))

        newReturnType = psiFactory.createTypeFromText("X<java.util.List<A>>", method)
    }

    fun testFunctionRenameJavaUsages() = doTest { newName = "bar" }

    fun testParameterModifiers() = doTest {
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType)))
    }

    fun testFqNameShortening() = doTest {
        val newParameter = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.anyType)).apply {
            currentTypeInfo = KotlinTypeInfo(false, null, "kotlin.String")
        }
        addParameter(newParameter)
    }

    fun testObjectMember() = doTest { removeParameter(0) }

    fun testParameterListAddParam() = doTest {
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "l", KotlinTypeInfo(false, BUILT_INS.longType)))
    }

    fun testParameterListRemoveParam() = doTest { removeParameter(getNewParametersCount() - 1) }

    fun testParameterListRemoveAllParams() = doTest { clearParameters() }

    fun testAddNewReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "_", KotlinTypeInfo(false, BUILT_INS.anyType), null, defaultValueForCall)
                .apply { currentTypeInfo = KotlinTypeInfo(false, null, "X") }
    }

    fun testAddNewReceiverForMember() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "_", KotlinTypeInfo(false, BUILT_INS.anyType), null, defaultValueForCall)
                .apply { currentTypeInfo = KotlinTypeInfo(false, null, "X") }
    }

    fun testAddNewReceiverForMemberConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "_", KotlinTypeInfo(false, BUILT_INS.anyType), null, defaultValueForCall)
                .apply { currentTypeInfo = KotlinTypeInfo(false, null, "X") }
    }

    fun testAddNewReceiverConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "_", KotlinTypeInfo(false, BUILT_INS.anyType), null, defaultValueForCall)
                .apply { currentTypeInfo = KotlinTypeInfo(false, null, "X") }
    }

    fun testRemoveReceiver() = doTest { removeParameter(0) }

    fun testRemoveReceiverForMember() = doTest { removeParameter(0) }

    fun testConvertParameterToReceiver1() = doTest { receiverParameterInfo = newParameters[0] }

    fun testConvertParameterToReceiver2() = doTest { receiverParameterInfo = newParameters[1] }

    fun testConvertReceiverToParameter1() = doTest { receiverParameterInfo = null }

    fun testConvertReceiverToParameter2() = doTest {
        receiverParameterInfo = null

        val newParameters = newParameters
        setNewParameter(0, newParameters[1])
        setNewParameter(1, newParameters[0])
    }

    fun testConvertParameterToReceiverForMember1() = doTest { receiverParameterInfo = newParameters[0] }

    fun testConvertParameterToReceiverForMember2() = doTest { receiverParameterInfo = newParameters[1] }

    fun testConvertParameterToReceiverForMemberConflict() = doTestConflict { receiverParameterInfo = newParameters[0] }

    fun testConvertReceiverToParameterForMember1() = doTest { receiverParameterInfo = null }

    fun testConvertReceiverToParameterForMember2() = doTest {
        receiverParameterInfo = null

        val newParameters = newParameters
        setNewParameter(0, newParameters[1])
        setNewParameter(1, newParameters[0])
    }

    fun testConvertReceiverToParameterWithNameClash() = doTest { receiverParameterInfo = null }

    fun testConvertReceiverToParameterAndChangeName() = doTest {
        receiverParameterInfo = null
        newParameters[0].name = "abc"
    }

    fun testChangeReceiver() = doTest { receiverParameterInfo = newParameters[1] }

    fun testChangeReceiverForMember() = doTest { receiverParameterInfo = newParameters[1] }

    fun testChangeParameterTypeWithImport() = doTest { newParameters[0].currentTypeInfo = KotlinTypeInfo(false, null, "a.Bar") }

    fun testSecondaryConstructor() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType), null, defaultValueForCall))
    }

    fun testJavaConstructorInDelegationCall() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType, "\"foo\"")) }

    fun testPrimaryConstructorByThisRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType), null, defaultValueForCall))
    }

    fun testPrimaryConstructorBySuperRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType), null, defaultValueForCall))
    }

    fun testSecondaryConstructorByThisRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType), null, defaultValueForCall))
    }

    fun testSecondaryConstructorBySuperRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(KotlinParameterInfo(methodDescriptor.baseDescriptor, -1, "s", KotlinTypeInfo(false, BUILT_INS.stringType), null, defaultValueForCall))
    }

    fun testJavaConstructorBySuperRef() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType, "\"foo\"")) }

    fun testNoConflictWithReceiverName() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("0")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "i", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))
    }

    fun testRemoveParameterBeforeLambda() = doTest { removeParameter(1) }

    fun testMoveLambdaParameter() = doTest {
        val newParameters = newParameters
        setNewParameter(1, newParameters[2])
        setNewParameter(2, newParameters[1])
    }

    fun testConvertLambdaParameterToReceiver() = doTest { receiverParameterInfo = newParameters[2] }

    fun testRemoveLambdaParameter() = doTest { removeParameter(2) }

    fun testRemoveEnumConstructorParameter() = doTest { removeParameter(1) }

    fun testRemoveAllEnumConstructorParameters() = doTest { clearParameters() }

    fun testDoNotApplyPrimarySignatureToSecondaryCalls() = doTest {
        val newParameters = newParameters
        setNewParameter(0, newParameters[1])
        setNewParameter(1, newParameters[0])
    }

    fun testConvertToExtensionAndRename() = doTest {
        receiverParameterInfo = newParameters[0]
        newName = "foo1"
    }

    fun testRenameExtensionParameter() = doTest { newParameters[1].name = "b" }

    fun testConvertParameterToReceiverAddParens() = doTest { receiverParameterInfo = newParameters[0] }

    fun testThisReplacement() = doTest { receiverParameterInfo = null }

    fun testPrimaryConstructorByRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))
    }

    fun testReceiverToParameterExplicitReceiver() = doTest { receiverParameterInfo = null }

    fun testReceiverToParameterImplicitReceivers() = doTest { receiverParameterInfo = null }

    fun testParameterToReceiverExplicitReceiver() = doTest { receiverParameterInfo = newParameters[0] }

    fun testParameterToReceiverImplicitReceivers() = doTest { receiverParameterInfo = newParameters[0] }

    fun testJavaMethodOverridesReplaceParam() = doJavaTestConflict {
        newReturnType = stringPsiType
        newParameters[0] = ParameterInfoImpl(-1, "x", PsiType.INT, "1")
    }

    fun testJavaMethodOverridesChangeParam() = doJavaTest {
        newReturnType = stringPsiType

        newParameters[0].name = "x"
        newParameters[0].setType(PsiType.INT)
    }

    fun testChangeProperty() = doTest {
        newName = "s"
        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.stringType)
    }

    fun testAddPropertyReceiverConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"\"")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor,
                                                    -1,
                                                    "receiver",
                                                    KotlinTypeInfo(false, BUILT_INS.stringType),
                                                    null,
                                                    defaultValueForCall)
    }

    fun testAddPropertyReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"\"")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor,
                                                    -1,
                                                    "receiver",
                                                    KotlinTypeInfo(false, BUILT_INS.stringType),
                                                    null,
                                                    defaultValueForCall)
    }

    fun testChangePropertyReceiver() = doTest { receiverParameterInfo!!.currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.intType) }

    fun testRemovePropertyReceiver() = doTest { receiverParameterInfo = null }

    fun testAddTopLevelPropertyReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("A()")
        receiverParameterInfo = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "receiver", KotlinTypeInfo(false), null, defaultValueForCall)
                .apply { currentTypeInfo = KotlinTypeInfo(false, null, "test.A") }
    }

    fun testChangeTopLevelPropertyReceiver() = doTest { receiverParameterInfo!!.currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.stringType) }

    fun testRemoveTopLevelPropertyReceiver() = doTest { receiverParameterInfo = null }

    fun testChangeClassParameter() = doTest {
        newName = "s"
        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.stringType)
    }

    fun testParameterPropagation() = doTest {
        val psiFactory = KtPsiFactory(project)

        val defaultValueForCall1 = psiFactory.createExpression("1")
        val newParameter1 = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false), null, defaultValueForCall1)
                .apply { currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.intType) }
        addParameter(newParameter1)

        val defaultValueForCall2 = psiFactory.createExpression("\"abc\"")
        val newParameter2 = KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "s", KotlinTypeInfo(false), null, defaultValueForCall2)
                .apply { currentTypeInfo = KotlinTypeInfo(false, BUILT_INS.stringType) }
        addParameter(newParameter2)

        val classA = KotlinFullClassNameIndex.getInstance().get("A", project, project.allScope()).first()
        val functionBar = classA.declarations.first { it is KtNamedFunction && it.name == "bar" }
        val functionTest = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("test", project, project.allScope()).first()

        primaryPropagationTargets = listOf(functionBar, functionTest)
    }

    fun testJavaParameterPropagation() = doJavaTest {
        newParameters.add(ParameterInfoImpl(-1, "n", PsiType.INT, "1"))
        newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType, "\"abc\""))

        val classA = JavaFullClassNameIndex.getInstance().get("A".hashCode(), project, project.allScope()).first { it.name == "A" }
        val methodBar = classA.methods.first { it.name == "bar" }
        parameterPropagationTargets.add(methodBar)

        val functionTest = KotlinTopLevelFunctionFqnNameIndex.getInstance().get("test", project, project.allScope()).first()
        parameterPropagationTargets.add(functionTest.getRepresentativeLightMethod()!!)
    }

    fun testPropagateWithParameterDuplication() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testPropagateWithVariableDuplication() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testPropagateWithThisQualificationInClassMember() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        val classA = KotlinFullClassNameIndex.getInstance().get("A", project, project.allScope()).first()
        val functionBar = classA.declarations.first { it is KtNamedFunction && it.name == "bar" }
        primaryPropagationTargets = listOf(functionBar)
    }

    fun testPropagateWithThisQualificationInExtension() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testJavaConstructorParameterPropagation() = doJavaTest {
        newParameters.add(ParameterInfoImpl(-1, "n", PsiType.INT, "1"))
        parameterPropagationTargets.addAll(findCallers(method))
    }

    fun testPrimaryConstructorParameterPropagation() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        primaryPropagationTargets = findCallers(method.getRepresentativeLightMethod()!!)
    }

    fun testSecondaryConstructorParameterPropagation() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValueForCall))

        primaryPropagationTargets = findCallers(method.getRepresentativeLightMethod()!!)
    }

    fun testJavaMethodOverridesOmitUnitType() = doJavaTest {}

    fun testOverrideInAnonymousObjectWithTypeParameters() = doTest { newName = "bar" }

    fun testMakePrimaryConstructorPrivateNoParams() = doTest { newVisibility = Visibilities.PRIVATE }

    fun testMakePrimaryConstructorPublic() = doTest { newVisibility = Visibilities.PUBLIC }

    fun testRenameExtensionParameterWithNamedArgs() = doTest { newParameters[2].name = "bb" }

    fun testImplicitThisToParameterWithChangedType() = doTest {
        receiverParameterInfo!!.currentTypeInfo = KotlinTypeInfo(false, null, "Older")
        receiverParameterInfo = null
    }

    fun testJvmOverloadedRenameParameter() = doTest { newParameters[0].name = "aa" }

    fun testJvmOverloadedSwapParams1() = doTest { swapParameters(1, 2) }

    fun testJvmOverloadedSwapParams2() = doTest { swapParameters(0, 2) }

    private fun doTestJvmOverloadedAddDefault(index: Int) = doTest {
        val defaultValue = KtPsiFactory(project).createExpression("2")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), defaultValue, defaultValue), index)
    }

    private fun doTestJvmOverloadedAddNonDefault(index: Int) = doTest {
        val defaultValue = KtPsiFactory(project).createExpression("2")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "n", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValue), index)
    }

    fun testJvmOverloadedAddDefault1() = doTestJvmOverloadedAddDefault(0)

    fun testJvmOverloadedAddDefault2() = doTestJvmOverloadedAddDefault(1)

    fun testJvmOverloadedAddDefault3() = doTestJvmOverloadedAddDefault(-1)

    fun testJvmOverloadedAddNonDefault1() = doTestJvmOverloadedAddNonDefault(0)

    fun testJvmOverloadedAddNonDefault2() = doTestJvmOverloadedAddNonDefault(1)

    fun testJvmOverloadedAddNonDefault3() = doTestJvmOverloadedAddNonDefault(-1)

    fun testJvmOverloadedRemoveDefault1() = doTest { removeParameter(0) }

    fun testJvmOverloadedRemoveDefault2() = doTest { removeParameter(1) }

    fun testJvmOverloadedRemoveDefault3() = doTest { removeParameter(getNewParametersCount() - 1) }

    fun testJvmOverloadedRemoveNonDefault1() = doTest { removeParameter(0) }

    fun testJvmOverloadedRemoveNonDefault2() = doTest { removeParameter(1) }

    fun testJvmOverloadedRemoveNonDefault3() = doTest { removeParameter(getNewParametersCount() - 1) }

    fun testJvmOverloadedConstructorSwapParams() = doTest { swapParameters(1, 2) }

    fun testDefaultAfterLambda() = doTest { swapParameters(0, 1) }

    fun testRemoveDefaultParameterBeforeLambda() = doTest { removeParameter(1) }

    fun testAddParameterKeepFormat() = doTest {
        val psiFactory = KtPsiFactory(project)
        val defaultValue1 = psiFactory.createExpression("4")
        val defaultValue2 = psiFactory.createExpression("5")
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "d", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValue1), 2)
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor, -1, "e", KotlinTypeInfo(false, BUILT_INS.intType), null, defaultValue2))
    }

    fun testRemoveParameterKeepFormat1() = doTest { removeParameter(0) }

    fun testRemoveParameterKeepFormat2() = doTest { removeParameter(1) }

    fun testRemoveParameterKeepFormat3() = doTest { removeParameter(2) }

    fun testSwapParametersKeepFormat() = doTest { swapParameters(0, 2) }

    fun testSetErrorReturnType() = doTest { newReturnTypeInfo = KotlinTypeInfo(true, null, "XYZ") }

    fun testSetErrorReceiverType() = doTest { receiverParameterInfo!!.currentTypeInfo = KotlinTypeInfo(true, null, "XYZ") }

    fun testSetErrorParameterType() = doTest { newParameters[1].currentTypeInfo = KotlinTypeInfo(true, null, "XYZ") }

    fun testSwapDataClassParameters() = doTest {
        swapParameters(0, 2)
        swapParameters(1, 2)
    }

    fun testAddDataClassParameter() = doTest {
        addParameter(KotlinParameterInfo(originalBaseFunctionDescriptor,
                                         -1,
                                         "c",
                                         KotlinTypeInfo(false, BUILT_INS.intType),
                                         null,
                                         KtPsiFactory(project).createExpression("3"),
                                         KotlinValVar.Val),
                     1)
    }

    fun testRemoveDataClassParameter() = doTest { removeParameter(1) }

    fun testRemoveAllOriginalDataClassParameters() = doTest {
        val psiFactory = KtPsiFactory(project)

        swapParameters(1, 2)
        setNewParameter(0,
                        KotlinParameterInfo(originalBaseFunctionDescriptor,
                                            -1,
                                            "d",
                                            KotlinTypeInfo(false, BUILT_INS.intType),
                                            null,
                                            psiFactory.createExpression("4"),
                                            KotlinValVar.Val))

        setNewParameter(2,
                        KotlinParameterInfo(originalBaseFunctionDescriptor,
                                            -1,
                                            "e",
                                            KotlinTypeInfo(false, BUILT_INS.intType),
                                            null,
                                            psiFactory.createExpression("5"),
                                            KotlinValVar.Val))
    }

    fun testImplicitReceiverInRecursiveCall() = doTest {
        receiverParameterInfo = null
        newParameters[0].name = "a"
    }

    fun testReceiverInSafeCall() = doTestConflict { receiverParameterInfo = null }

    fun testRemoveParameterKeepOtherComments() = doTest { removeParameter(1) }

    fun testReturnTypeViaCodeFragment() = doTest {
        newName = "bar"
        newReturnTypeInfo = resolveType("A<T, U>", true, true)
    }

    fun testChangeReturnTypeToNonUnit() = doTest {
        newReturnTypeInfo = KotlinTypeInfo(true, BUILT_INS.intType)
    }
}