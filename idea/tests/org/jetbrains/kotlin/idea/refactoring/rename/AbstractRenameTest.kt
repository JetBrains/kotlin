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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.Property
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.rename.*
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.asJava.finder.KtLightPackage
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

enum class RenameType {
    JAVA_CLASS,
    JAVA_METHOD,
    KOTLIN_CLASS,
    KOTLIN_FUNCTION,
    KOTLIN_PROPERTY,
    KOTLIN_PACKAGE,
    MARKED_ELEMENT,
    FILE,
    BUNDLE_PROPERTY,
    AUTO_DETECT
}

abstract class AbstractRenameTest : KotlinLightCodeInsightFixtureTestCase() {
    inner class TestContext(
            val testFile: File,
            val project: Project = getProject(),
            val javaFacade: JavaPsiFacade = myFixture.javaFacade,
            val module: Module = myFixture.module)

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.getProjectDescriptor()

        val testConfigurationFile = File(testDataPath, fileName())
        val renameObject = loadTestConfiguration(testConfigurationFile)
        val withRuntime = renameObject.getNullableString("withRuntime")
        val libraryInfos = renameObject.getAsJsonArray("libraries")?.map { it.asString!! }
        if (libraryInfos != null) {
            val jarPaths = listOf(ForTestCompileRuntime.runtimeJarForTests()) + libraryInfos.map {
                File(PlatformTestUtil.getCommunityPath(), it.substringAfter("@"))
            }
            return KotlinWithJdkAndRuntimeLightProjectDescriptor(jarPaths)
        }

        if (withRuntime != null) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        return KotlinLightProjectDescriptor.INSTANCE
    }

    open fun doTest(path: String) {
        val testFile = File(path)
        val renameObject = loadTestConfiguration(testFile)

        val renameTypeStr = renameObject.getString("type")

        val hintDirective = renameObject.getNullableString("hint")

        val fixtureClasses = renameObject.getAsJsonArray("fixtureClasses")?.map { it.asString } ?: emptyList()

        try {
            fixtureClasses.forEach { TestFixtureExtension.loadFixture(it, module) }

            val context = TestContext(testFile)

            when (RenameType.valueOf(renameTypeStr)) {
                RenameType.JAVA_CLASS -> renameJavaClassTest(renameObject, context)
                RenameType.JAVA_METHOD -> renameJavaMethodTest(renameObject, context)
                RenameType.KOTLIN_CLASS -> renameKotlinClassTest(renameObject, context)
                RenameType.KOTLIN_FUNCTION -> renameKotlinFunctionTest(renameObject, context)
                RenameType.KOTLIN_PROPERTY -> renameKotlinPropertyTest(renameObject, context)
                RenameType.KOTLIN_PACKAGE -> renameKotlinPackageTest(renameObject, context)
                RenameType.MARKED_ELEMENT -> renameMarkedElement(renameObject, context)
                RenameType.FILE -> renameFile(renameObject, context)
                RenameType.BUNDLE_PROPERTY -> renameBundleProperty(renameObject, context)
                RenameType.AUTO_DETECT -> renameWithAutoDetection(renameObject, context)
            }

            if (hintDirective != null) {
                Assert.fail("""Hint "$hintDirective" was expected""")
            }

            if (renameObject["checkErrorsAfter"]?.asBoolean ?: false) {
                val psiManager = myFixture.psiManager
                val visitor = object : VirtualFileVisitor<Any>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        (psiManager.findFile(file) as? KtFile)?.let { DirectiveBasedActionUtils.checkForUnexpectedErrors(it) }
                        return true
                    }
                }

                for (sourceRoot in ModuleRootManager.getInstance(myModule).sourceRoots) {
                    VfsUtilCore.visitChildrenRecursively(sourceRoot, visitor)
                }
            }
        }
        catch (e : Exception) {
            if (e !is RefactoringErrorHintException && e !is ConflictsInTestsException) throw e

            val hintExceptionUnquoted = StringUtil.unquoteString(e.message!!)
            if (hintDirective != null) {
                Assert.assertEquals(hintDirective, hintExceptionUnquoted)
            }
            else {
                Assert.fail("""Unexpected "hint: $hintExceptionUnquoted" """)
            }
        }
        finally {
            fixtureClasses.forEach { TestFixtureExtension.unloadFixture(it) }
        }
    }

    protected open fun configExtra(rootDir: VirtualFile, renameParamsObject: JsonObject) {

    }

    private fun renameMarkedElement(renameParamsObject: JsonObject, context: TestContext) {
        val mainFilePath = renameParamsObject.getString("mainFile")

        doTestCommittingDocuments(context) { rootDir ->
            configExtra(rootDir, renameParamsObject)
            val psiFile = myFixture.configureFromTempProjectFile(mainFilePath)

            doRenameMarkedElement(renameParamsObject, psiFile)
        }
    }

    private fun renameJavaClassTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classId").toClassId().asSingleFqName().asString()
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments(context) { _ ->
            val aClass = context.javaFacade.findClass(classFQN, context.project.allScope())!!
            val substitution = RenamePsiElementProcessor.forElement(aClass).substituteElementToRename(aClass, null)

            runRenameProcessor(context.project, newName, substitution, renameParamsObject, true, true)
        }
    }

    private fun renameJavaMethodTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classId").toClassId().asSingleFqName().asString()
        val methodSignature = renameParamsObject.getString("methodSignature")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments(context) {
            val aClass = context.javaFacade.findClass(classFQN, GlobalSearchScope.moduleScope(context.module))!!

            val methodText = context.javaFacade.elementFactory.createMethodFromText(methodSignature + "{}", null)
            val method = aClass.findMethodBySignature(methodText, false)

            if (method == null) throw IllegalStateException("Method with signature '$methodSignature' wasn't found in class $classFQN")

            val substitution = RenamePsiElementProcessor.forElement(method).substituteElementToRename(method, null)
            runRenameProcessor(context.project, newName, substitution, renameParamsObject, false, false)
        }
    }

    private fun renameKotlinFunctionTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldMethodName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClassOrPackage(renameParamsObject, context) {
            _, scope -> scope.getContributedFunctions(oldMethodName, NoLookupLocation.FROM_TEST).first() }
    }

    private fun renameKotlinPropertyTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldPropertyName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClassOrPackage(renameParamsObject, context) {
            _, scope -> scope.getContributedVariables(oldPropertyName, NoLookupLocation.FROM_TEST).first() }
    }

    private fun renameKotlinClassTest(renameParamsObject: JsonObject, context: TestContext) {
        renameParamsObject.getString("classId") //assertion

        doRenameInKotlinClassOrPackage(renameParamsObject, context) { declaration, _ -> declaration as ClassDescriptor }
    }

    private fun renameKotlinPackageTest(renameParamsObject: JsonObject, context: TestContext) {
        val fqn = FqNameUnsafe(renameParamsObject.getString("fqn")).toSafe()
        val newName = renameParamsObject.getString("newName")
        val mainFilePath = renameParamsObject.getNullableString("mainFile") ?: "${getTestDirName(false)}.kt"

        doTestCommittingDocuments(context) {
            val mainFile = myFixture.configureFromTempProjectFile(mainFilePath) as KtFile

            val fileFqn = mainFile.packageFqName
            Assert.assertTrue("File '${mainFilePath}' should have package containing ${fqn}", fileFqn.isSubpackageOf(fqn))

            val packageSegment = mainFile.packageDirective!!.packageNames[fqn.pathSegments().size - 1]
            val segmentReference = packageSegment.mainReference

            val psiElement = segmentReference.resolve()!!

            val substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)
            runRenameProcessor(context.project, newName, substitution, renameParamsObject, true, true)
        }
    }

    private fun renameFile(renameParamsObject: JsonObject, context: TestContext) {
        val file = renameParamsObject.getString("file")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments(context) {
            val psiFile = myFixture.configureFromTempProjectFile(file)

            runRenameProcessor(context.project, newName, psiFile, renameParamsObject, true, true)
        }
    }

    private fun renameBundleProperty(renameParamsObject: JsonObject, context: TestContext) {
        val file = renameParamsObject.getString("file")
        val oldName = renameParamsObject.getString("oldName")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments(context) {
            val mainFile = myFixture.configureFromTempProjectFile(file) as PropertiesFile
            val property = mainFile.findPropertyByKey(oldName) as Property

            runRenameProcessor(context.project, newName, property, renameParamsObject, true, true)
        }
    }

    private fun doRenameInKotlinClassOrPackage(
            renameParamsObject: JsonObject, context: TestContext, findDescriptorToRename: (DeclarationDescriptor, MemberScope) -> DeclarationDescriptor
    ) {
        val classIdStr = renameParamsObject.getNullableString("classId")
        val packageFqnStr = renameParamsObject.getNullableString("packageFqn")
        if (classIdStr != null && packageFqnStr != null) {
            throw AssertionError("Both classId and packageFqn are defined. Where should I search: in class or in package?")
        }
        else if (classIdStr == null && packageFqnStr == null) {
            throw AssertionError("Define classId or packageFqn")
        }

        val newName = renameParamsObject.getString("newName")
        val mainFilePath = renameParamsObject.getNullableString("mainFile") ?: "${getTestDirName(false)}.kt"

        doTestCommittingDocuments(context) {
            val ktFile = myFixture.configureFromTempProjectFile(mainFilePath) as KtFile

            val module = ktFile.analyzeFullyAndGetResult().moduleDescriptor

            val (declaration, scopeToSearch)  = if (classIdStr != null) {
                module.findClassAcrossModuleDependencies(classIdStr.toClassId())!!.let { it to it.defaultType.memberScope }
            } else {
                module.getPackage(FqName(packageFqnStr!!)).let { it to it.memberScope }
            }

            val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(findDescriptorToRename(declaration, scopeToSearch))!!

            // The Java processor always chooses renaming the base element when running in unit test mode,
            // so if we want to rename only the inherited element, we need to skip the substitutor.
            val skipSubstitute = renameParamsObject["skipSubstitute"]?.asBoolean ?: false
            val substitution = if (skipSubstitute)
                psiElement
            else
                RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)

            runRenameProcessor(context.project, newName, substitution, renameParamsObject, true, true)
        }
    }

    private fun renameWithAutoDetection(renameParamsObject: JsonObject, context: TestContext) {
        val mainFilePath = renameParamsObject.getString("mainFile")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments(context) { rootDir ->
            configExtra(rootDir, renameParamsObject)

            val psiFile = myFixture.configureFromTempProjectFile(mainFilePath)

            val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
            val marker = doc.extractMarkerOffset(project, "/*rename*/")
            assert(marker != -1)

            editor.caretModel.moveToOffset(marker)
            val currentCaret = editor.caretModel.currentCaret

            val textEditorPsiDataProvider = TextEditorPsiDataProvider()

            val dataContext = DataContext { dataId ->
                when (dataId) {
                    CommonDataKeys.PROJECT.name -> project
                    CommonDataKeys.EDITOR.name -> editor
                    CommonDataKeys.CARET.name,
                    CommonDataKeys.PSI_ELEMENT.name,
                    CommonDataKeys.PSI_FILE.name -> textEditorPsiDataProvider.getData(dataId, editor, currentCaret)
                    PsiElementRenameHandler.DEFAULT_NAME.name -> newName
                    else -> null
                }
            }
            val handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext) ?: return@doTestCommittingDocuments
            Assert.assertTrue(handler.isAvailableOnDataContext(dataContext))
            handler.invoke(project, editor, psiFile, dataContext)
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.indexOf('_'))
    }

    protected fun doTestCommittingDocuments(context: TestContext, action: (VirtualFile) -> Unit) {
        val beforeDir = context.testFile.parentFile.name + "/before"
        val beforeVFile = myFixture.copyDirectoryToProject(beforeDir, "")
        PsiDocumentManager.getInstance(myFixture.project).commitAllDocuments()

        val afterDir = File(context.testFile.parentFile, "after")
        val afterVFile = LocalFileSystem.getInstance().findFileByIoFile(afterDir)?.apply {
            UsefulTestCase.refreshRecursively(this)
        }

        action(beforeVFile)

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        FileDocumentManager.getInstance().saveAllDocuments()
        PlatformTestUtil.assertDirectoriesEqual(afterVFile, beforeVFile)
    }
}

private  fun String.toClassId(): ClassId {
    val relativeClassName = FqName(substringAfterLast('/'))
    val packageFqName = FqName(substringBeforeLast('/', "").replace('/', '.'))
    return ClassId(packageFqName, relativeClassName, false)
}

fun loadTestConfiguration(testFile: File): JsonObject {
    val fileText = FileUtil.loadFile(testFile, true)

    val jsonParser = JsonParser()
    val renameObject = jsonParser.parse(fileText) as JsonObject
    return renameObject
}

fun runRenameProcessor(
        project: Project,
        newName: String,
        substitution: PsiElement?,
        renameParamsObject: JsonObject,
        isSearchInComments: Boolean,
        isSearchTextOccurrences: Boolean
) {
    if (substitution == null) return

    fun createProcessor(): BaseRefactoringProcessor {
        if (substitution is PsiPackage && substitution !is KtLightPackage) {
            val oldName = substitution.qualifiedName
            if (StringUtil.getPackageName(oldName) != StringUtil.getPackageName(newName)) {
                return RenamePsiPackageProcessor.createRenameMoveProcessor(newName, substitution, isSearchInComments, isSearchTextOccurrences)
            }
        }

        return RenameProcessor(project, substitution, newName, isSearchInComments, isSearchTextOccurrences)
    }

    val processor = createProcessor()

    if (renameParamsObject["overloadRenamer.onlyPrimaryElement"]?.asBoolean ?: false) {
        with(AutomaticOverloadsRenamer) { substitution.elementFilter = { false } }
    }
    if (processor is RenameProcessor) {
        Extensions.getExtensions(AutomaticRenamerFactory.EP_NAME).forEach { processor.addRenamerFactory(it) }
    }
    processor.run()
}

fun doRenameMarkedElement(renameParamsObject: JsonObject, psiFile: PsiFile) {
    val project = psiFile.project
    val newName = renameParamsObject.getString("newName")

    val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
    val marker = doc.extractMarkerOffset(project, "/*rename*/")
    assert(marker != -1)

    val editorFactory = EditorFactory.getInstance()
    var editor = editorFactory.getEditors(doc).firstOrNull()
    var shouldReleaseEditor = false
    if (editor == null) {
        editor = editorFactory.createEditor(doc)
        shouldReleaseEditor = true
    }

    try {
        val isByRef = renameParamsObject["byRef"]?.asBoolean ?: false
        val isInjected = renameParamsObject["injected"]?.asBoolean ?: false
        var currentEditor = editor!!
        var currentFile: PsiFile = psiFile
        if (isByRef || isInjected) {
            currentEditor.caretModel.moveToOffset(marker)
            if (isInjected) {
                currentFile = InjectedLanguageUtil.findInjectedPsiNoCommit(psiFile, marker)!!
                currentEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, currentFile)
            }
        }
        val toRename = if (isByRef) {
            TargetElementUtil.findTargetElement(currentEditor, TargetElementUtil.getInstance().allAccepted)!!
        }
        else {
            currentFile.findElementAt(marker)!!.getNonStrictParentOfType<PsiNamedElement>()!!
        }

        val substitution = RenamePsiElementProcessor.forElement(toRename).substituteElementToRename(toRename, null)

        val searchInComments = renameParamsObject["searchInComments"]?.asBoolean ?: true
        val searchInTextOccurrences = renameParamsObject["searchInTextOccurrences"]?.asBoolean ?: true
        runRenameProcessor(project, newName, substitution, renameParamsObject, searchInComments, searchInTextOccurrences)
    }
    finally {
        if (shouldReleaseEditor) {
            editorFactory.releaseEditor(editor!!)
        }
    }
}
