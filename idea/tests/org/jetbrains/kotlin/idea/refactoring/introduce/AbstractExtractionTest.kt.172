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

package org.jetbrains.kotlin.idea.refactoring.introduce

import com.intellij.codeInsight.CodeInsightUtil
import com.intellij.codeInsight.completion.JavaCompletionUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.IntroduceParameterRefactoring
import com.intellij.refactoring.introduceField.ElementToWorkOn
import com.intellij.refactoring.introduceParameter.AbstractJavaInplaceIntroducer
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor
import com.intellij.refactoring.introduceParameter.Util
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.occurrences.ExpressionOccurrenceManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.chooseMembers
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ExtractSuperInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ExtractSuperRefactoring
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.EXTRACT_FUNCTION
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty.INTRODUCE_PROPERTY
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty.KotlinIntroducePropertyHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.IntroduceTypeAliasDescriptor
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.KotlinIntroduceTypeAliasHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeParameter.KotlinIntroduceTypeParameterHandler
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.refactoring.markMembersInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.extractClassMembers
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.findElementByCommentPrefix
import java.io.File
import java.lang.AssertionError
import java.util.*
import kotlin.test.assertEquals

abstract class AbstractExtractionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    val fixture: JavaCodeInsightTestFixture get() = myFixture

    protected fun doIntroduceVariableTest(path: String) {
        doTest(path) { file ->
            file as KtFile

            KotlinIntroduceVariableHandler.invoke(
                    fixture.project,
                    fixture.editor,
                    file,
                    DataManager.getInstance().getDataContext(fixture.editor.component)
            )
        }
    }

    private fun doIntroduceParameterTest(path: String, asLambda: Boolean) {
        doTest(path) { file ->
            val fileText = file.text

            open class HelperImpl: KotlinIntroduceParameterHelper {
                override fun configure(descriptor: IntroduceParameterDescriptor): IntroduceParameterDescriptor {
                    return with (descriptor) {
                        val singleReplace = InTextDirectivesUtils.isDirectiveDefined(fileText, "// SINGLE_REPLACE")
                        val withDefaultValue = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// WITH_DEFAULT_VALUE:") ?: true

                        copy(occurrencesToReplace = if (singleReplace) Collections.singletonList(originalOccurrence) else occurrencesToReplace,
                             withDefaultValue = withDefaultValue)
                    }
                }
            }

            class LambdaHelperImpl: HelperImpl(), KotlinIntroduceLambdaParameterHelper {
                override fun configureExtractLambda(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor {
                    return with(descriptor) {
                        if (name.isNullOrEmpty()) copy(suggestedNames = listOf("__dummyTestFun__")) else this
                    }
                }
            }

            val handler = if (asLambda) {
                KotlinIntroduceLambdaParameterHandler(LambdaHelperImpl())
            } else {
                KotlinIntroduceParameterHandler(HelperImpl())
            }
            with (handler) {
                val target = (file as KtFile).findElementByCommentPrefix("// TARGET:") as? KtNamedDeclaration
                if (target != null) {
                    selectElement(fixture.editor, file, true, listOf(CodeInsightUtils.ElementKind.EXPRESSION)) { element ->
                        invoke(fixture.project, fixture.editor, element as KtExpression, target)
                    }
                }
                else {
                    invoke(fixture.project, fixture.editor, file, null)
                }
            }
        }
    }

    protected fun doIntroduceSimpleParameterTest(path: String) {
        doIntroduceParameterTest(path, false)
    }

    protected fun doIntroduceLambdaParameterTest(path: String) {
        doIntroduceParameterTest(path, true)
    }

    protected fun doIntroduceJavaParameterTest(path: String) {
        // Copied from com.intellij.refactoring.IntroduceParameterTest.perform()
        doTest(path, true) { file ->
            file as PsiJavaFile

            var elementToWorkOn: ElementToWorkOn? = null
            ElementToWorkOn.processElementToWorkOn(
                    editor,
                    file,
                    "Introduce parameter",
                    null,
                    project,
                    object : ElementToWorkOn.ElementsProcessor<ElementToWorkOn> {
                        override fun accept(e: ElementToWorkOn): Boolean {
                            return true
                        }

                        override fun pass(e: ElementToWorkOn?) {
                            if (e != null) {
                                elementToWorkOn = e
                            }
                        }
                    })

            val expr = elementToWorkOn!!.expression
            val localVar = elementToWorkOn!!.localVariable

            val context = expr ?: localVar
            val method = Util.getContainingMethod(context) ?: throw AssertionError("No containing method found")

            val applyToSuper = InTextDirectivesUtils.isDirectiveDefined(file.getText(), "// APPLY_TO_SUPER")
            val methodToSearchFor = if (applyToSuper) method.findDeepestSuperMethods()[0] else method

            val (initializer, occurrences) =
                    if (expr == null) {
                        localVar.initializer!! to CodeInsightUtil.findReferenceExpressions(method, localVar)
                    }
                    else {
                        expr to ExpressionOccurrenceManager(expr, method, null).findExpressionOccurrences()
                    }
            val type = initializer.type

            val parametersToRemove = Util.findParametersToRemove(method, initializer, occurrences)

            val codeStyleManager = JavaCodeStyleManager.getInstance(project)
            val info = codeStyleManager.suggestUniqueVariableName(
                    codeStyleManager.suggestVariableName(VariableKind.PARAMETER, localVar?.name, initializer, type),
                    expr,
                    true
            )
            val suggestedNames = AbstractJavaInplaceIntroducer.appendUnresolvedExprName(
                    JavaCompletionUtil.completeVariableNameForRefactoring(codeStyleManager, type, VariableKind.LOCAL_VARIABLE, info),
                    initializer
            )

            IntroduceParameterProcessor(project,
                                        method,
                                        methodToSearchFor,
                                        initializer,
                                        expr,
                                        localVar,
                                        true,
                                        suggestedNames.first(),
                                        true,
                                        IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_NONE,
                                        false,
                                        false,
                                        null,
                                        parametersToRemove).run()

            editor.selectionModel.removeSelection()
        }
    }

    protected fun doIntroducePropertyTest(path: String) {
        doTest(path) { file ->
            file as KtFile

            val extractionTarget = propertyTargets.single {
                it.targetName == InTextDirectivesUtils.findStringWithPrefixes(file.getText(), "// EXTRACTION_TARGET: ")
            }
            val explicitPreviousSibling = file.findElementByCommentPrefix("// SIBLING:")
            val helper = object : ExtractionEngineHelper(INTRODUCE_PROPERTY) {
                override fun validate(descriptor: ExtractableCodeDescriptor) = descriptor.validate(extractionTarget)

                override fun configureAndRun(
                        project: Project,
                        editor: Editor,
                        descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                        onFinish: (ExtractionResult) -> Unit
                ) {
                    doRefactor(
                            ExtractionGeneratorConfiguration(
                                    descriptorWithConflicts.descriptor,
                                    ExtractionGeneratorOptions.DEFAULT.copy(target = extractionTarget, delayInitialOccurrenceReplacement = true)
                            ),
                            onFinish
                    )
                }
            }
            val handler = KotlinIntroducePropertyHandler(helper)
            val editor = fixture.editor
            handler.selectElements(editor, file) { elements, previousSibling ->
                handler.doInvoke(project, editor, file, elements, explicitPreviousSibling ?: previousSibling)
            }
        }
    }

    protected fun doExtractFunctionTest(path: String) {
        doTest(path) { file -> doExtractFunction(myFixture, file as KtFile) }
    }

    protected fun doIntroduceTypeParameterTest(path: String) {
        doTest(path) { file ->
            file as KtFile

            val explicitPreviousSibling = file.findElementByCommentPrefix("// SIBLING:")
            val editor = fixture.editor
            KotlinIntroduceTypeParameterHandler.selectElements(editor, file) { elements, previousSibling ->
                KotlinIntroduceTypeParameterHandler.doInvoke(project, editor, elements, explicitPreviousSibling ?: previousSibling)
            }
        }
    }

    protected fun doIntroduceTypeAliasTest(path: String) {
        doTest(path) { file ->
            file as KtFile

            val explicitPreviousSibling = file.findElementByCommentPrefix("// SIBLING:")
            val fileText = file.text
            val aliasName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// NAME:")
            val aliasVisibility = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// VISIBILITY:")?.let {
                KtPsiFactory(project).createModifierList(it).firstChild.node.elementType as KtModifierKeywordToken
            }
            val editor = fixture.editor
            object : KotlinIntroduceTypeAliasHandler() {
                override fun doInvoke(
                        project: Project,
                        editor: Editor,
                        elements: List<PsiElement>,
                        targetSibling: PsiElement,
                        descriptorSubstitutor: ((IntroduceTypeAliasDescriptor) -> IntroduceTypeAliasDescriptor)?
                ) {
                    super.doInvoke(project, editor, elements, explicitPreviousSibling ?: targetSibling) {
                        it.copy(name = aliasName ?: it.name, visibility = aliasVisibility ?: it.visibility)
                    }
                }
            }.invoke(project, editor, file, null)
        }
    }

    protected fun doExtractSuperTest(path: String, isInterface: Boolean) {
        doTest(path) { file ->
            file as KtFile

            markMembersInfo(file)

            val targetParent = file.findElementByCommentPrefix("// SIBLING:")?.parent ?: file.parent!!
            val fileText = file.text
            val className = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// NAME:")!!
            val editor = fixture.editor
            val originalClass = file.findElementAt(editor.caretModel.offset)?.getStrictParentOfType<KtClassOrObject>()!!
            val memberInfos = chooseMembers(extractClassMembers(originalClass))
            val conflicts = ExtractSuperRefactoring.collectConflicts(originalClass, memberInfos, targetParent, className, isInterface)
            project.checkConflictsInteractively(conflicts) {
                val extractInfo = ExtractSuperInfo(
                        originalClass,
                        memberInfos,
                        targetParent,
                        "$className.${KotlinFileType.EXTENSION}",
                        className,
                        isInterface,
                        DocCommentPolicy<PsiComment>(DocCommentPolicy.ASIS)
                )
                ExtractSuperRefactoring(extractInfo).performRefactoring()
            }
        }
    }

    protected fun doExtractSuperclassTest(path: String) = doExtractSuperTest(path, false)

    protected fun doExtractInterfaceTest(path: String) = doExtractSuperTest(path, true)

    protected fun doTest(path: String, checkAdditionalAfterdata: Boolean = false, action: (PsiFile) -> Unit) {
        val mainFile = File(path)

        PluginTestCaseBase.addJdk(myFixture.projectDisposable, PluginTestCaseBase::mockJdk)

        fixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${mainFile.parent}"

        val mainFileName = mainFile.name
        val mainFileBaseName = FileUtil.getNameWithoutExtension(mainFileName)
        val extraFiles = mainFile.parentFile.listFiles { _, name ->
            name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java"))
        }
        val extraFilesToPsi = extraFiles.associateBy { fixture.configureByFile(it.name) }
        val file = fixture.configureByFile(mainFileName)

        val addKotlinRuntime = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// WITH_RUNTIME") != null
        if (addKotlinRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        try {
            checkExtract(ExtractTestFiles(path, file, extraFilesToPsi), checkAdditionalAfterdata, action)
        }
        finally {
            if (addKotlinRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }
}

class ExtractTestFiles(
        val mainFile: PsiFile,
        val afterFile: File,
        val conflictFile: File,
        val extraFilesToPsi: Map<PsiFile, File> = emptyMap()) {
    constructor(path: String, mainFile: PsiFile, extraFilesToPsi: Map<PsiFile, File> = emptyMap()) :
            this(mainFile, File("$path.after"), File("$path.conflicts"), extraFilesToPsi)
}

fun checkExtract(files: ExtractTestFiles, checkAdditionalAfterdata: Boolean = false, action: (PsiFile) -> Unit) {
    val conflictFile = files.conflictFile
    val afterFile = files.afterFile

    try {
        action(files.mainFile)

        assert(!conflictFile.exists()) { "Conflict file $conflictFile should not exist" }
        KotlinTestUtils.assertEqualsToFile(afterFile, files.mainFile.text!!)

        if (checkAdditionalAfterdata) {
            for ((extraPsiFile, extraFile) in files.extraFilesToPsi) {
                KotlinTestUtils.assertEqualsToFile(File("${extraFile.path}.after"), extraPsiFile.text)
            }
        }
    }
    catch(e: ConflictsInTestsException) {
        val message = e.messages.sorted().joinToString(" ").replace("\n", " ")
        KotlinTestUtils.assertEqualsToFile(conflictFile, message)
    }
    catch(e: CommonRefactoringUtil.RefactoringErrorHintException) {
        KotlinTestUtils.assertEqualsToFile(conflictFile, e.message!!)
    }
    catch(e: RuntimeException) { // RuntimeException is thrown by IDEA code in CodeInsightUtils.java
        if (e::class.java != RuntimeException::class.java) throw e
        KotlinTestUtils.assertEqualsToFile(conflictFile, e.message!!)
    }
}

fun doExtractFunction(fixture: CodeInsightTestFixture, file: KtFile) {
    val explicitPreviousSibling = file.findElementByCommentPrefix("// SIBLING:")
    val fileText = file.getText() ?: ""
    val expectedNames = InTextDirectivesUtils.findListWithPrefixes(fileText, "// SUGGESTED_NAMES: ")
    val expectedReturnTypes = InTextDirectivesUtils.findListWithPrefixes(fileText, "// SUGGESTED_RETURN_TYPES: ")
    val expectedDescriptors =
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PARAM_DESCRIPTOR: ").joinToString()
    val expectedTypes =
            InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PARAM_TYPES: ").map { "[$it]" }.joinToString()

    val extractionOptions = InTextDirectivesUtils.findListWithPrefixes(fileText, "// OPTIONS: ").let {
        if (it.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val args = it.map { it.toBoolean() }.toTypedArray() as Array<Any?>
            ExtractionOptions::class.java.constructors.first { it.parameterTypes.size == args.size }.newInstance(*args) as ExtractionOptions
        } else ExtractionOptions.DEFAULT
    }

    val renderer = DescriptorRenderer.FQ_NAMES_IN_TYPES

    val editor = fixture.editor
    val handler = ExtractKotlinFunctionHandler(
            helper = object : ExtractionEngineHelper(EXTRACT_FUNCTION) {
                override fun adjustExtractionData(data: ExtractionData): ExtractionData {
                    return data.copy(options = extractionOptions)
                }

                override fun configureAndRun(
                        project: Project,
                        editor: Editor,
                        descriptorWithConflicts: ExtractableCodeDescriptorWithConflicts,
                        onFinish: (ExtractionResult) -> Unit
                ) {
                    val descriptor = descriptorWithConflicts.descriptor
                    val actualNames = descriptor.suggestedNames
                    val actualReturnTypes = descriptor.controlFlow.possibleReturnTypes.map {
                        IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
                    }
                    val allParameters = listOfNotNull(descriptor.receiverParameter) + descriptor.parameters
                    val actualDescriptors = allParameters.map { renderer.render(it.originalDescriptor) }.joinToString()
                    val actualTypes = allParameters.map {
                        it.getParameterTypeCandidates(false).map { renderer.renderType(it) }.joinToString(", ", "[", "]")
                    }.joinToString()

                    if (actualNames.size != 1 || expectedNames.isNotEmpty()) {
                        assertEquals(expectedNames, actualNames, "Expected names mismatch.")
                    }
                    if (actualReturnTypes.size != 1 || expectedReturnTypes.isNotEmpty()) {
                        assertEquals(expectedReturnTypes, actualReturnTypes, "Expected return types mismatch.")
                    }
                    KotlinLightCodeInsightFixtureTestCaseBase.assertEquals("Expected descriptors mismatch.", expectedDescriptors, actualDescriptors)
                    KotlinLightCodeInsightFixtureTestCaseBase.assertEquals("Expected types mismatch.", expectedTypes, actualTypes)

                    val newDescriptor = if (descriptor.name == "") {
                        descriptor.copy(suggestedNames = Collections.singletonList("__dummyTestFun__"))
                    }
                    else {
                        descriptor
                    }

                    doRefactor(ExtractionGeneratorConfiguration(newDescriptor, ExtractionGeneratorOptions.DEFAULT), onFinish)
                }
            }
    )
    handler.selectElements(editor, file) { elements, previousSibling ->
        handler.doInvoke(editor, file, elements, explicitPreviousSibling ?: previousSibling)
    }
}
