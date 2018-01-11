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

package org.jetbrains.kotlin.idea.spring.tests

import com.intellij.facet.impl.FacetUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.spring.facet.SpringFacet
import com.intellij.spring.facet.SpringFileSet
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

@Suppress("unused")
class SpringTestFixtureExtension() : TestFixtureExtension {
    private var module: Module? = null

    override fun setUp(module: Module) {
        this.module = module

        val springClasspath = System.getProperty("spring.classpath")
                              ?: throw RuntimeException("Unable to get a valid classpath from 'spring.classpath' property, please set it accordingly");

        ConfigLibraryUtil.addLibrary(module, "spring", null, springClasspath.split(File.pathSeparator).toTypedArray())

        FacetUtil.addFacet(module, SpringFacet.getSpringFacetType())
    }

    fun configureFileSet(fixture: CodeInsightTestFixture, configFiles: Collection<String>): SpringFileSet {
        return configureFileSet(fixture, "default", module!!, configFiles)
    }

    fun configureFileSet(fixture: CodeInsightTestFixture, id: String, module: Module, configFiles: Collection<String>): SpringFileSet {
        return module.getSpringFacetWithAssertion().addFileSet(id, id).apply {
            configFiles.forEach { addFile(fixture.copyFileToProject(it)) }
        }
    }

    fun Module.getSpringFacetWithAssertion() = SpringFacet.getInstance(this) ?: error("No Spring facet in ${this}")

    override fun tearDown() {
        try {
            // clear existing SpringFacet configuration before running next test
            module?.let { module ->
                SpringFacet.getInstance(module)?.let { facet ->
                    facet.removeFileSets()
                    FacetUtil.deleteFacet(facet)
                }
                ConfigLibraryUtil.removeLibrary(module, "spring")
            }
        }
        finally {
            module = null
        }
    }
}

fun SpringTestFixtureExtension.loadConfigByMainFilePath(testPath: String, fixture: JavaCodeInsightTestFixture) {
    val mainFileName = PathUtil.getFileName(testPath)
    val baseName = FileUtil.getNameWithoutExtension(mainFileName)
    val configFileName = if (baseName.endsWith("Xml")) "$baseName-config.xml" else mainFileName
    configureFileSet(fixture, listOf(PathUtil.toSystemIndependentName("${PathUtil.getParentPath(testPath)}/$configFileName")))
}

fun configureSpringFileSetByDirective(module: Module, directives: String, psiFiles: Collection<PsiFile>) {
    if (!InTextDirectivesUtils.isDirectiveDefined(directives, "// CONFIGURE_SPRING_FILE_SET")) return
    val fileSet = SpringFacet.getInstance(module)!!.addFileSet("default", "default")!!
    psiFiles.forEach { fileSet.addFile(it.virtualFile) }
}