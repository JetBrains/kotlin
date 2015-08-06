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
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.MultiFileTestCase
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.jsonUtils.getNullableString
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.junit.Assert
import java.io.File

private enum class RenameType {
    JAVA_CLASS,
    JAVA_METHOD,
    KOTLIN_CLASS,
    KOTLIN_FUNCTION,
    KOTLIN_PROPERTY,
    KOTLIN_PACKAGE,
    MARKED_ELEMENT
}

public abstract class AbstractRenameTest : KotlinMultiFileTestCase() {
    inner class TestContext(
            val project: Project = getProject()!!,
            val javaFacade: JavaPsiFacade = getJavaFacade()!!,
            val module: Module = getModule()!!)

    public open fun doTest(path : String) {
        val fileText = FileUtil.loadFile(File(path), true)

        val jsonParser = JsonParser()
        val renameObject = jsonParser.parse(fileText) as JsonObject

        val renameTypeStr = renameObject.getString("type")

        val hintDirective = renameObject.getNullableString("hint")

        try {
            val context = TestContext()

            when (RenameType.valueOf(renameTypeStr)) {
                RenameType.JAVA_CLASS -> renameJavaClassTest(renameObject, context)
                RenameType.JAVA_METHOD -> renameJavaMethodTest(renameObject, context)
                RenameType.KOTLIN_CLASS -> renameKotlinClassTest(renameObject, context)
                RenameType.KOTLIN_FUNCTION -> renameKotlinFunctionTest(renameObject, context)
                RenameType.KOTLIN_PROPERTY -> renameKotlinPropertyTest(renameObject, context)
                RenameType.KOTLIN_PACKAGE -> renameKotlinPackageTest(renameObject, context)
                RenameType.MARKED_ELEMENT -> renameMarkedElement(renameObject, context)
            }

            if (hintDirective != null) {
                Assert.fail("""Hint "$hintDirective" was expected""")
            }

            if (renameObject["checkErrorsAfter"]?.getAsBoolean() ?: false) {
                val psiManager = PsiManager.getInstance(myProject)
                val visitor = object : VirtualFileVisitor<Any>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        (psiManager.findFile(file) as? JetFile)?.let { DirectiveBasedActionUtils.checkForUnexpectedErrors(it) }
                        return true
                    }
                }

                for (sourceRoot in ModuleRootManager.getInstance(myModule).getSourceRoots()) {
                    VfsUtilCore.visitChildrenRecursively(sourceRoot, visitor)
                }
            }
        }
        catch (e : Exception) {
            if (e !is RefactoringErrorHintException && e !is ConflictsInTestsException) throw e

            val hintExceptionUnquoted = StringUtil.unquoteString(e.getMessage()!!)
            if (hintDirective != null) {
                Assert.assertEquals(hintDirective, hintExceptionUnquoted)
            }
            else {
                Assert.fail("""Unexpected "hint: $hintExceptionUnquoted" """)
            }
        }
    }

    private fun renameMarkedElement(renameParamsObject: JsonObject, context: TestContext) {
        val mainFilePath = renameParamsObject.getString("mainFile")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments { rootDir, rootAfter ->
            val mainFile = rootDir.findChild(mainFilePath)!!
            val psiFile = PsiManager.getInstance(context.project).findFile(mainFile)!!

            val MARKER_TEXT = "/*rename*/"
            val marker = psiFile.getText().indexOf(MARKER_TEXT)
            assert(marker != -1)

            val toRename = if (renameParamsObject["byRef"]?.getAsBoolean() ?: false) {
                val editor = createEditor(mainFile)
                editor.getCaretModel().moveToOffset(marker + MARKER_TEXT.length())
                TargetElementUtilBase.findTargetElement(editor, TargetElementUtilBase.getInstance().getAllAccepted())!!
            }
            else {
                psiFile.findElementAt(marker + MARKER_TEXT.length())!!.getNonStrictParentOfType<PsiNamedElement>()!!
            }
            val substitution = RenamePsiElementProcessor.forElement(toRename).substituteElementToRename(toRename, null)

            runRenameProcessor(context, newName, substitution, true, true)
        }
    }

    private fun renameJavaClassTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classId").toClassId().asSingleFqName().asString()
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments { rootDir, rootAfter ->
            val aClass = context.javaFacade.findClass(classFQN, context.project.allScope())!!
            val substitution = RenamePsiElementProcessor.forElement(aClass).substituteElementToRename(aClass, null)

            runRenameProcessor(context, newName, substitution, true, true)
        }
    }

    private fun renameJavaMethodTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classId").toClassId().asSingleFqName().asString()
        val methodSignature = renameParamsObject.getString("methodSignature")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments { rootDir, rootAfter ->
            val aClass = context.javaFacade.findClass(classFQN, GlobalSearchScope.moduleScope(context.module))!!

            val methodText = context.javaFacade.getElementFactory().createMethodFromText(methodSignature + "{}", null)
            val method = aClass.findMethodBySignature(methodText, false)

            if (method == null) throw IllegalStateException("Method with signature '$methodSignature' wasn't found in class $classFQN")

            val substitution = RenamePsiElementProcessor.forElement(method).substituteElementToRename(method, null)
            runRenameProcessor(context, newName, substitution, false, false)
        }
    }

    private fun renameKotlinFunctionTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldMethodName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClassOrPackage(renameParamsObject, context) { it.getFunctions(oldMethodName).first() }
    }

    private fun renameKotlinPropertyTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldPropertyName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClassOrPackage(renameParamsObject, context) { it.getProperties(oldPropertyName).first() }
    }

    private fun renameKotlinClassTest(renameParamsObject: JsonObject, context: TestContext) {
        renameParamsObject.getString("classId") //assertion

        doRenameInKotlinClassOrPackage(renameParamsObject, context) { scope -> scope.getContainingDeclaration() as ClassDescriptor }
    }

    private fun renameKotlinPackageTest(renameParamsObject: JsonObject, context: TestContext) {
        val fqn = FqNameUnsafe(renameParamsObject.getString("fqn")).toSafe()
        val newName = renameParamsObject.getString("newName")
        val mainFilePath = renameParamsObject.getNullableString("mainFile") ?: "${getTestDirName(false)}.kt"

        doTestCommittingDocuments { rootDir, rootAfter ->
            val mainFile = rootDir.findChild(mainFilePath)!!
            val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
            val jetFile = PsiDocumentManager.getInstance(context.project).getPsiFile(document) as JetFile

            val fileFqn = jetFile.getPackageFqName()
            Assert.assertTrue("File '${mainFilePath}' should have package containing ${fqn}", fileFqn.isSubpackageOf(fqn))

            val packageSegment = jetFile.getPackageDirective()!!.getPackageNames()[fqn.pathSegments().size() - 1]
            val segmentReference = packageSegment.mainReference

            val psiElement = segmentReference.resolve()!!

            val substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)
            runRenameProcessor(context, newName, substitution, true, true)
        }
    }

    private fun doRenameInKotlinClassOrPackage(
            renameParamsObject: JsonObject, context: TestContext, findDescriptorToRename: (JetScope) -> DeclarationDescriptor
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

        doTestCommittingDocuments { rootDir, rootAfter ->
            val mainFile = rootDir.findChild(mainFilePath)!!
            val document = FileDocumentManager.getInstance().getDocument(mainFile)!!
            val jetFile = PsiDocumentManager.getInstance(context.project).getPsiFile(document) as JetFile

            val module = jetFile.analyzeFullyAndGetResult().moduleDescriptor

            val scopeToSearch = if (classIdStr != null) {
                module.findClassAcrossModuleDependencies(classIdStr.toClassId())!!.getDefaultType().getMemberScope()
            } else {
                module.getPackage(FqName(packageFqnStr!!)).memberScope
            }

            val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(findDescriptorToRename(scopeToSearch))!!

            val substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)

            runRenameProcessor(context, newName, substitution, true, true)
        }
    }

    private fun runRenameProcessor(
            context: TestContext,
            newName: String,
            substitution: PsiElement?,
            isSearchInComments: Boolean,
            isSearchTextOccurrences: Boolean
    ) {
        val renameProcessor = RenameProcessor(context.project, substitution, newName, isSearchInComments, isSearchTextOccurrences)
        Extensions.getExtensions(AutomaticRenamerFactory.EP_NAME).forEach { renameProcessor.addRenamerFactory(it) }
        renameProcessor.run()
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.indexOf('_'))
    }

    protected fun doTestCommittingDocuments(action : (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest(MultiFileTestCase.PerformAction { rootDir, rootAfter ->
            action(rootDir, rootAfter)

            PsiDocumentManager.getInstance(getProject()!!).commitAllDocuments()
            FileDocumentManager.getInstance().saveAllDocuments()
        }, getTestDirName(true))
    }

    protected override fun getTestRoot() : String {
        return "/refactoring/rename/"
    }

    protected override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}


private  fun String.toClassId(): ClassId {
    val relativeClassName = FqName(substringAfterLast('/'))
    val packageFqName = FqName(substringBeforeLast('/', "").replace('/', '.'))
    return ClassId(packageFqName, relativeClassName, false)
}
