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
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.VisibilityUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinMethodNode
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.util.*

class JetChangeSignatureTest : KotlinCodeInsightTestCase() {
    companion object {
        private val BUILT_INS = JvmPlatform.builtIns
        private val EXTENSIONS = arrayOf(".kt", ".java")
    }

    private var editors: MutableList<Editor>? = null

    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()

    override fun getTestDataPath() = File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").path + File.separator

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
        val root = KotlinMethodNode(method, HashSet(), project, Runnable { })
        return (0..root.childCount - 1).flatMapTo(LinkedHashSet<PsiMethod>()) {
            (root.getChildAt(it) as KotlinMethodNode).method.toLightMethods()
        }
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

    private fun createChangeInfo(): JetChangeInfo {
        configureFiles()

        val element = (JetChangeSignatureHandler().findTargetMember(file, editor) as KtElement?).sure { "Target element is null" }
        val context = file.findElementAt(editor.caretModel.offset).sure { "Context element is null" }
        val bindingContext = element.analyze(BodyResolveMode.FULL)
        val callableDescriptor = JetChangeSignatureHandler
                .findDescriptor(element, project, editor, bindingContext)
                .sure { "Target descriptor is null" }
        return createChangeInfo(project, callableDescriptor, JetChangeSignatureConfiguration.Empty, context)!!
    }

    private fun doTest(configure: JetChangeInfo.() -> Unit = {}) {
        JetChangeSignatureProcessor(project, createChangeInfo().apply { configure() }, "Change signature").run()

        compareEditorsWithExpectedData()
    }

    private fun doTestConflict(configure: JetChangeInfo.() -> Unit = {}) {
        try {
            doTest(configure)
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

    private fun doTestUnmodifiable(configure: JetChangeInfo.() -> Unit = {}) {
        try {
            doTest(configure)
            TestCase.fail("No conflicts found")
        }
        catch (e: RuntimeException) {
            if ((e.message ?: "").contains("Cannot modify file")) return

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

    private fun JetChangeInfo.swapParameters(i: Int, j: Int) {
        val newParameters = newParameters
        val temp = newParameters[i]
        setNewParameter(i, newParameters[j])
        setNewParameter(j, temp)
    }

    // --------------------------------- Tests ---------------------------------

    fun testBadSelection() {
        configureByFile(getTestName(false) + "Before.kt")
        TestCase.assertNull(JetChangeSignatureHandler().findTargetMember(file, editor))
    }

    fun testSynthesized() = doTestConflict()

    fun testPreferContainedInClass() = TestCase.assertEquals("param", createChangeInfo().newParameters[0].name)

    fun testRenameFunction() = doTest { newName = "after" }

    fun testChangeReturnType() = doTest { newReturnTypeText = "Float" }

    fun testAddReturnType() = doTest { newReturnTypeText = "Float" }

    fun testRemoveReturnType() = doTest { newReturnTypeText = "Unit" }

    fun testChangeConstructorVisibility() = doTest { newVisibility = Visibilities.PROTECTED }

    fun testAddConstructorVisibility() = doTest {
        newVisibility = Visibilities.PROTECTED

        val newParameter = JetParameterInfo(
                callableDescriptor = originalBaseFunctionDescriptor,
                name = "x",
                type = BUILT_INS.anyType,
                defaultValueForCall = KtPsiFactory(project).createExpression("12"),
                valOrVar = JetValVar.Val
        )
        addParameter(newParameter)
    }

    fun testConstructor() = doTest {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].valOrVar = JetValVar.Var
        newParameters[1].valOrVar = JetValVar.None
        newParameters[2].valOrVar = JetValVar.Val

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeText = "Float?"
    }

    fun testGenericConstructor() = doTest {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].valOrVar = JetValVar.Var
        newParameters[1].valOrVar = JetValVar.None
        newParameters[2].valOrVar = JetValVar.Val

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeText = "Double?"
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

        newParameters[1].currentTypeText = "Float?"
    }

    fun testGenericFunctions() = doTest() {
        newVisibility = Visibilities.PUBLIC

        newParameters[0].name = "_x1"
        newParameters[1].name = "_x2"
        newParameters[2].name = "_x3"

        newParameters[1].currentTypeText = "Double?"
    }

    fun testExpressionFunction() = doTest {
        newParameters[0].name = "x1"

        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "y1", BUILT_INS.intType))
    }

    fun testFunctionsAddRemoveArguments() = doTest {
        newVisibility = Visibilities.INTERNAL

        val defaultValueForCall = KtPsiFactory(project).createExpression("null")
        val newParameters = newParameters
        setNewParameter(2, newParameters[1])
        setNewParameter(1, newParameters[0])
        setNewParameter(0, JetParameterInfo(originalBaseFunctionDescriptor, -1, "x0", BUILT_INS.nullableAnyType, null, defaultValueForCall))
    }

    fun testFakeOverride() = doTest {
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "i", BUILT_INS.intType))
    }

    fun testFunctionLiteral() = doTest {
        newParameters[1].name = "y1"
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "x", BUILT_INS.anyType))

        newReturnTypeText = "Int"
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
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType))
    }

    fun testEnumEntriesWithoutSuperCalls() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))
    }

    fun testParameterChangeInOverrides() = doTest {
        newParameters[0].name = "n"
        newParameters[0].currentTypeText = "Int"
    }

    fun testConstructorJavaUsages() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"abc\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testFunctionJavaUsagesAndOverridesAddParam() = doTest {
        val psiFactory = KtPsiFactory(project)
        val defaultValueForCall1 = psiFactory.createExpression("\"abc\"")
        val defaultValueForCall2 = psiFactory.createExpression("\"def\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall1))
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "o", BUILT_INS.nullableAnyType, null, defaultValueForCall2))
    }

    fun testFunctionJavaUsagesAndOverridesChangeNullability() = doTest {
        newParameters[1].currentTypeText = "String?"
        newParameters[2].currentTypeText = "Any"

        newReturnTypeText = "String?"
    }

    fun testFunctionJavaUsagesAndOverridesChangeTypes() = doTest {
        newParameters[0].currentTypeText = "String?"
        newParameters[1].currentTypeText = "Int"
        newParameters[2].currentTypeText = "Long?"

        newReturnTypeText = "Any?"
    }

    fun testGenericsWithOverrides() = doTest {
        newParameters[0].currentTypeText = "List<C>"
        newParameters[1].currentTypeText = "A?"
        newParameters[2].currentTypeText = "U<B>"

        newReturnTypeText = "U<C>?"
    }

    fun testAddReceiverToGenericsWithOverrides() = doTest {
        val parameterInfo = newParameters[0]
        parameterInfo.currentTypeText = "U<A>"
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

    fun testParameterModifiers() = doTest { addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType)) }

    fun testFqNameShortening() = doTest {
        val newParameter = JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.anyType).apply {
            currentTypeText = "kotlin.String"
        }
        addParameter(newParameter)
    }

    fun testObjectMember() = doTest { removeParameter(0) }

    fun testParameterListAddParam() = doTest { addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "l", BUILT_INS.longType)) }

    fun testParameterListRemoveParam() = doTest { removeParameter(getNewParametersCount() - 1) }

    fun testParameterListRemoveAllParams() = doTest { clearParameters() }

    fun testAddNewReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "_", BUILT_INS.anyType, null, defaultValueForCall)
                .apply { currentTypeText = "X" }
    }

    fun testAddNewReceiverForMember() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "_", BUILT_INS.anyType, null, defaultValueForCall)
                .apply { currentTypeText = "X" }
    }

    fun testAddNewReceiverForMemberConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "_", BUILT_INS.anyType, null, defaultValueForCall)
                .apply { currentTypeText = "X" }
    }

    fun testAddNewReceiverConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("X(0)")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "_", BUILT_INS.anyType, null, defaultValueForCall)
                .apply { currentTypeText = "X" }
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

    fun testChangeParameterTypeWithImport() = doTest { newParameters[0].currentTypeText = "a.Bar" }

    fun testSecondaryConstructor() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testJavaConstructorInDelegationCall() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType, "\"foo\"")) }

    fun testPrimaryConstructorByThisRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testPrimaryConstructorBySuperRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testSecondaryConstructorByThisRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testSecondaryConstructorBySuperRef() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"foo\"")
        addParameter(JetParameterInfo(methodDescriptor.baseDescriptor, -1, "s", BUILT_INS.stringType, null, defaultValueForCall))
    }

    fun testJavaConstructorBySuperRef() = doJavaTest { newParameters.add(ParameterInfoImpl(-1, "s", stringPsiType, "\"foo\"")) }

    fun testNoConflictWithReceiverName() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("0")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "i", BUILT_INS.intType, null, defaultValueForCall))
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
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))
    }

    fun testReceiverToParameterExplicitReceiver() = doTest { receiverParameterInfo = null }

    fun testReceiverToParameterImplicitReceivers() = doTest { receiverParameterInfo = null }

    fun testParameterToReceiverExplicitReceiver() = doTest { receiverParameterInfo = newParameters[0] }

    fun testParameterToReceiverImplicitReceivers() = doTest { receiverParameterInfo = newParameters[0] }

    fun testJavaMethodOverridesReplaceParam() = doJavaTest {
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
        newReturnTypeText = "String"
    }

    fun testAddPropertyReceiverConflict() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"\"")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "receiver", BUILT_INS.stringType, null, defaultValueForCall)
    }

    fun testAddPropertyReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("\"\"")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "receiver", BUILT_INS.stringType, null, defaultValueForCall)
    }

    fun testChangePropertyReceiver() = doTest { receiverParameterInfo!!.currentTypeText = "Int" }

    fun testRemovePropertyReceiver() = doTest { receiverParameterInfo = null }

    fun testAddTopLevelPropertyReceiver() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("A()")
        receiverParameterInfo = JetParameterInfo(originalBaseFunctionDescriptor, -1, "receiver", null, null, defaultValueForCall)
                .apply { currentTypeText = "test.A" }
    }

    fun testChangeTopLevelPropertyReceiver() = doTest { receiverParameterInfo!!.currentTypeText = "String" }

    fun testRemoveTopLevelPropertyReceiver() = doTest { receiverParameterInfo = null }

    fun testChangeClassParameter() = doTest {
        newName = "s"
        newReturnTypeText = "String"
    }

    fun testParameterPropagation() = doTest {
        val psiFactory = KtPsiFactory(project)

        val defaultValueForCall1 = psiFactory.createExpression("1")
        val newParameter1 = JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", null, null, defaultValueForCall1)
                .apply { currentTypeText = "kotlin.Int" }
        addParameter(newParameter1)

        val defaultValueForCall2 = psiFactory.createExpression("\"abc\"")
        val newParameter2 = JetParameterInfo(originalBaseFunctionDescriptor, -1, "s", null, null, defaultValueForCall2)
                .apply { currentTypeText = "kotlin.String" }
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
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testPropagateWithVariableDuplication() = doTestConflict {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testPropagateWithThisQualificationInClassMember() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        val classA = KotlinFullClassNameIndex.getInstance().get("A", project, project.allScope()).first()
        val functionBar = classA.declarations.first { it is KtNamedFunction && it.name == "bar" }
        primaryPropagationTargets = listOf(functionBar)
    }

    fun testPropagateWithThisQualificationInExtension() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        primaryPropagationTargets = listOf(KotlinTopLevelFunctionFqnNameIndex.getInstance().get("bar", project, project.allScope()).first())
    }

    fun testJavaConstructorParameterPropagation() = doJavaTest {
        newParameters.add(ParameterInfoImpl(-1, "n", PsiType.INT, "1"))
        parameterPropagationTargets.addAll(findCallers(method))
    }

    fun testPrimaryConstructorParameterPropagation() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        primaryPropagationTargets = findCallers(method.getRepresentativeLightMethod()!!)
    }

    fun testSecondaryConstructorParameterPropagation() = doTest {
        val defaultValueForCall = KtPsiFactory(project).createExpression("1")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValueForCall))

        primaryPropagationTargets = findCallers(method.getRepresentativeLightMethod()!!)
    }

    fun testJavaMethodOverridesOmitUnitType() = doJavaTest {}

    fun testOverrideInAnonymousObjectWithTypeParameters() = doTest { newName = "bar" }

    fun testMakePrimaryConstructorPrivateNoParams() = doTest { newVisibility = Visibilities.PRIVATE }

    fun testMakePrimaryConstructorPublic() = doTest { newVisibility = Visibilities.PUBLIC }

    fun testRenameExtensionParameterWithNamedArgs() = doTest { newParameters[2].name = "bb" }

    fun testImplicitThisToParameterWithChangedType() = doTest {
        receiverParameterInfo!!.currentTypeText = "Older"
        receiverParameterInfo = null
    }

    fun testJvmOverloadedRenameParameter() = doTest { newParameters[0].name = "aa" }

    fun testJvmOverloadedSwapParams1() = doTest { swapParameters(1, 2) }

    fun testJvmOverloadedSwapParams2() = doTest { swapParameters(0, 2) }

    private fun doTestJvmOverloadedAddDefault(index: Int) = doTest {
        val defaultValue = KtPsiFactory(project).createExpression("2")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, defaultValue, defaultValue), index)
    }

    private fun doTestJvmOverloadedAddNonDefault(index: Int) = doTest {
        val defaultValue = KtPsiFactory(project).createExpression("2")
        addParameter(JetParameterInfo(originalBaseFunctionDescriptor, -1, "n", BUILT_INS.intType, null, defaultValue), index)
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
}
