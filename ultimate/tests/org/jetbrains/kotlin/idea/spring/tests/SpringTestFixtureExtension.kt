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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.spring.facet.SpringFacet
import com.intellij.spring.facet.SpringFileSet
import com.intellij.spring.settings.SpringGeneralSettings
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.tests.ULTIMATE_TEST_ROOT
import java.util.*

@Suppress("unused")
class SpringTestFixtureExtension() : TestFixtureExtension {
    private var module: Module? = null

    enum class SpringFramework(val version: String, vararg val artifactIds: String) {
        FRAMEWORK_4_2_0(
                "4.2.0.RELEASE",
                "core", "beans", "context", "tx", "web"
        )
    }

    val SPRING_LIBRARY_ROOT = "$ULTIMATE_TEST_ROOT/dependencies/spring"

    override fun setUp(module: Module) {
        this.module = module
        val library = SpringFramework.FRAMEWORK_4_2_0
        val libraryPath = "$SPRING_LIBRARY_ROOT/${library.version}/"
        val jarNames = HashSet<String>(library.artifactIds.size)
        for (id in library.artifactIds) {
            jarNames.add("spring-$id-${library.version}.jar")
        }
        ConfigLibraryUtil.addLibrary(module, "spring" + library.version, libraryPath, jarNames.toTypedArray())

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
                ConfigLibraryUtil.removeLibrary(module, "spring" + SpringFramework.FRAMEWORK_4_2_0.version)
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

fun forbidSpringFileSetAutoConfigureByDirective(project: Project, directives: String) {
    if (!InTextDirectivesUtils.isDirectiveDefined(directives, "// FORBID_SPRING_FILE_SET_AUTOCONFIGURE")) return
    SpringGeneralSettings.getInstance(project).isAllowAutoConfigurationMode = false
}