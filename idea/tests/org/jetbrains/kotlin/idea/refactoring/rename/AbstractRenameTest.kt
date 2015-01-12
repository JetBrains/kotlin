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

import com.intellij.refactoring.MultiFileTestCase
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.junit.Assert
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.JavaPsiFacade
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.Collections
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.idea.refactoring.move.getString
import org.jetbrains.kotlin.idea.refactoring.move.getNullableString
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import com.intellij.refactoring.util.CommonRefactoringUtil.RefactoringErrorHintException

private enum class RenameType {
    JAVA_CLASS
    JAVA_METHOD
    KOTLIN_CLASS
    KOTLIN_FUNCTION
    KOTLIN_PROPERTY
    KOTLIN_PACKAGE
}

public abstract class AbstractRenameTest : MultiFileTestCase() {
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
            }

            if (hintDirective != null) {
                Assert.fail("""Hint "$hintDirective" was expected""")
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

    private fun renameJavaClassTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classFQN")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments { rootDir, rootAfter ->
            val aClass = context.javaFacade.findClass(classFQN, context.project.allScope())!!
            val substitution = RenamePsiElementProcessor.forElement(aClass).substituteElementToRename(aClass, null)

            RenameProcessor(context.project, substitution, newName, true, true).run()
        }
    }

    private fun renameJavaMethodTest(renameParamsObject: JsonObject, context: TestContext) {
        val classFQN = renameParamsObject.getString("classFQN")
        val methodSignature = renameParamsObject.getString("methodSignature")
        val newName = renameParamsObject.getString("newName")

        doTestCommittingDocuments { rootDir, rootAfter ->
            val aClass = context.javaFacade.findClass(classFQN, GlobalSearchScope.moduleScope(context.module))!!

            val methodText = context.javaFacade.getElementFactory().createMethodFromText(methodSignature + "{}", null)
            val method = aClass.findMethodBySignature(methodText, false)

            if (method == null) throw IllegalStateException("Method with signature '$methodSignature' wasn't found in class $classFQN")

            val substitution = RenamePsiElementProcessor.forElement(method).substituteElementToRename(method, null)
            RenameProcessor(context.project, substitution, newName, false, false).run()
        }
    }

    private fun renameKotlinFunctionTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldMethodName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClass(renameParamsObject, context) { classDescriptor ->
            val scope = classDescriptor.getMemberScope(Collections.emptyList())
            scope.getFunctions(oldMethodName).first()
        }
    }

    private fun renameKotlinPropertyTest(renameParamsObject: JsonObject, context: TestContext) {
        val oldPropertyName = Name.identifier(renameParamsObject.getString("oldName"))

        doRenameInKotlinClass(renameParamsObject, context) { classDescriptor ->
            val scope = classDescriptor.getMemberScope(Collections.emptyList())
            scope.getProperties(oldPropertyName).first()
        }
    }

    private fun renameKotlinClassTest(renameParamsObject: JsonObject, context: TestContext) {
        doRenameInKotlinClass(renameParamsObject, context) { classDescriptor -> classDescriptor }
    }

    private fun renameKotlinPackageTest(renameParamsObject: JsonObject, context: TestContext) {
        val fqn = FqNameUnsafe(renameParamsObject.getString("fqn")).toSafe()
        val newName = renameParamsObject.getString("newName")
        val mainFilePath = renameParamsObject.getNullableString("mainFile") ?: "${getTestDirName(false)}.kt"

        doTestCommittingDocuments { rootDir, rootAfter ->
            val mainFile = rootDir.findChild(mainFilePath)!!
            val document = FileDocumentManager.getInstance()!!.getDocument(mainFile)!!
            val jetFile = PsiDocumentManager.getInstance(context.project).getPsiFile(document) as JetFile

            val fileFqn = jetFile.getPackageFqName()
            Assert.assertTrue("File '${mainFilePath}' should have package containing ${fqn}", fileFqn.isSubpackageOf(fqn))

            val packageSegment = jetFile.getPackageDirective()!!.getPackageNames()[fqn.pathSegments().size - 1]
            val segmentReference = packageSegment.getReference()!!

            val psiElement = segmentReference.resolve()!!

            val substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)
            RenameProcessor(context.project, substitution, newName, true, true).run()
        }
    }

    private fun doRenameInKotlinClass(renameParamsObject: JsonObject, context: TestContext,
                                      findDescriptorToRename: (ClassDescriptor) -> DeclarationDescriptor
    ) {
        val classFqName = FqNameUnsafe(renameParamsObject.getString("classFQN"))
        val newName = renameParamsObject.getString("newName")
        val mainFilePath = renameParamsObject.getNullableString("mainFile") ?: "${getTestDirName(false)}.kt"

        doTestCommittingDocuments { rootDir, rootAfter ->
            val mainFile = rootDir.findChild(mainFilePath)!!
            val document = FileDocumentManager.getInstance()!!.getDocument(mainFile)!!
            val jetFile = PsiDocumentManager.getInstance(context.project).getPsiFile(document) as JetFile

            val bindingContext = jetFile.analyzeFully()
            val classDescriptor = bindingContext.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, classFqName)!!

            val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(findDescriptorToRename(classDescriptor))!!

            val substitution = RenamePsiElementProcessor.forElement(psiElement).substituteElementToRename(psiElement, null)

            RenameProcessor(context.project, substitution, newName, true, true).run()
        }
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.indexOf('_'))
    }

    protected fun doTestCommittingDocuments(action : (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest(
                { rootDir, rootAfter ->
                    action(rootDir, rootAfter)

                    PsiDocumentManager.getInstance(getProject()!!).commitAllDocuments()
                    FileDocumentManager.getInstance()?.saveAllDocuments()
                },
                getTestDirName(true))
    }

    protected override fun getTestRoot() : String {
        return "/refactoring/rename/"
    }

    protected override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase()
    }
}
