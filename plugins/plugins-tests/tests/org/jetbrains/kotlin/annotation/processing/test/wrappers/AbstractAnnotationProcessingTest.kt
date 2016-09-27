/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation.processing.test.wrappers

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.registerServiceInstance
import org.jetbrains.kotlin.codegen.AbstractBytecodeTextTest
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.java.model.elements.DefaultJeElementRenderer
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import org.jetbrains.kotlin.java.model.toJeElement
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils

import java.io.File
import java.util.regex.Pattern

abstract class AbstractAnnotationProcessingTest : AbstractBytecodeTextTest() {
    companion object {
        private val SUBJECT_FQ_NAME_PATTERN = Pattern.compile("^// FQNAME: \\s*(.*)$", Pattern.MULTILINE)
        private val RENDERER = DefaultJeElementRenderer()
    }

    override fun doMultiFileTest(wholeFile: File, files: List<CodegenTestCase.TestFile>, javaFilesDir: File?) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, files, javaFilesDir)
        myEnvironment.project.registerServiceInstance(JeElementRegistry::class.java, JeElementRegistry())

        loadMultiFiles(files)

        val text = FileUtil.loadFile(wholeFile, true)
        val matcher = SUBJECT_FQ_NAME_PATTERN.matcher(text)
        assertTrue("No FqName specified. First line of the form '// f.q.Name' expected", matcher.find())
        val fqName = matcher.group(1)

        CodegenTestUtil.generateFiles(myEnvironment, myFiles)
        val project = myEnvironment.project
        val psiClass = JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.projectScope(project))!!

        val modelFile = File(wholeFile.parent, wholeFile.nameWithoutExtension + ".txt")
        val jeElement = psiClass.toJeElement() ?: error("JeElement is null")
        KotlinTestUtils.assertEqualsToFile(modelFile, RENDERER.render(jeElement))
    }
}