/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.MultiplePluginVersionGradleImportingTestCase
import org.jetbrains.kotlin.idea.codeInsight.gradle.facetSettings
import org.jetbrains.kotlin.idea.codeInsight.gradle.legacyMppImportTestMinVersionForMaster
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.Ignore
import org.junit.Test
import org.jetbrains.kotlin.idea.util.application.runReadAction

class MultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCase() {
    private fun legacyMode() = gradleVersion.split(".")[0].toInt() < 4
    private fun getDependencyLibraryUrls(moduleName: String) =
        getRootManager(moduleName)
            .orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .flatMap { it.getUrls(OrderRootType.CLASSES).map { it.replace(projectPath, "") } }

    private fun assertProductionOnTestDependency(moduleName: String, depModuleName: String, expected: Boolean) {
        val depOrderEntry = getModule(moduleName)
            .rootManager
            .orderEntries
            .filterIsInstance<ModuleOrderEntry>()
            .first { it.moduleName == depModuleName }
        assert(depOrderEntry.isProductionOnTestDependency == expected)
    }

    private fun assertFileInModuleScope(file: VirtualFile, moduleName: String) {
        runReadAction {
            assert(getModule(moduleName).getModuleWithDependenciesAndLibrariesScope(true).contains(file))
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testPlatformToCommonDependency() {
        val files = configureByFiles()
        importProject()

        assertModuleModuleDepScope("project.jvm.main", "project.common.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm.test", "project.common.test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js.main", "project.common.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js.test", "project.common.test", DependencyScope.COMPILE)

        assertProductionOnTestDependency("project.jvm.main", "project.common.main", false)
        assertProductionOnTestDependency("project.jvm.test", "project.common.test", true)
        assertProductionOnTestDependency("project.js.main", "project.common.main", false)
        assertProductionOnTestDependency("project.js.test", "project.common.test", true)

        val commonTestFile = files.find { it.path.contains("common") }!!
        assertFileInModuleScope(commonTestFile, "project.jvm.test")
        assertFileInModuleScope(commonTestFile, "project.js.test")
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testPlatformToCommonExpectedByDependency() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("project.jvm.main", "project.common1.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm.main", "project.common2.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm.test", "project.common1.test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm.test", "project.common2.test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js.main", "project.common1.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js.test", "project.common1.test", DependencyScope.COMPILE)
        assertNoModuleDepForModule("project.js.main", "project.common2.main")
        assertNoModuleDepForModule("project.js.test", "project.common2.test")
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testPlatformToCommonDependencyRoot() {
        configureByFiles()
        importProject()
        assertModuleModuleDepScope("foo.jvm.main", "foo.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("foo.jvm.test", "foo.test", DependencyScope.COMPILE)
        assertModuleModuleDepScope("foo.js.main", "foo.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("foo.js.test", "foo.test", DependencyScope.COMPILE)
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testMultiProject() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope("project.jvm-app.main", "project.common-app.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.main", "project.common-lib.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.main", "project.jvm-lib.main", DependencyScope.COMPILE)

        assertModuleModuleDepScope("project.js-app.main", "project.common-app.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js-app.main", "project.common-lib.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.js-app.main", "project.js-lib.main", DependencyScope.COMPILE)
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testDependenciesReachableViaImpl() {
        configureByFiles()
        importProject()

        assertModuleModuleDepScope("project.jvm-app.main", "project.jvm-lib2.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.main", "project.jvm-lib1.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.main", "project.common-lib1.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.main", "project.common-lib2.main", DependencyScope.COMPILE)

        assertModuleModuleDepScope("project.jvm-app.test", "project.jvm-lib2.main", DependencyScope.COMPILE)
        assertModuleModuleDepScope("project.jvm-app.test", "project.jvm-lib1.main", DependencyScope.COMPILE)
        //assertModuleModuleDepScope("project.jvm-app.test", "project.common-lib1.test", DependencyScope.COMPILE)
        //assertModuleModuleDepScope("project.jvm-app.test", "project.common-lib2.test", DependencyScope.COMPILE)
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testTransitiveImplement() {
        configureByFiles()

        val isResolveModulePerSourceSet = currentExternalProjectSettings.isResolveModulePerSourceSet

        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project.project1.test", "project.project1.main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project.project2.main", "project.project1.main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project.project2.test", "project.project2.main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project2.test", "project.project1.test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project2.test", "project.project1.main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project.project2.custom", "project.project1.custom", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project.project3.main", "project.project2.main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.main", "project.project1.main", DependencyScope.COMPILE)

            assertModuleModuleDepScope("project.project3.test", "project.project3.main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.test", "project.project2.test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.test", "project.project2.main", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.test", "project.project1.test", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.test", "project.project1.main", DependencyScope.COMPILE)

            //assertModuleModuleDepScope("project.project3.custom", "project.project1.custom", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3.custom", "project.project2.main", DependencyScope.COMPILE)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("project.project2", "project.project1", DependencyScope.COMPILE)
            if (legacyMode()) {
                // This data is obtained from Gradle model. Actually RUNTIME+TEST+PROVIDED == COMPILE, thus this difference does not matter for user
                assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.RUNTIME, DependencyScope.TEST, DependencyScope.PROVIDED)
            } else {
                assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.COMPILE)
            }
            assertModuleModuleDepScope("project.project3", "project.project1", DependencyScope.COMPILE)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testTransitiveImplementWithNonDefaultConfig() {
        configureByFiles()

        val isResolveModulePerSourceSet = currentExternalProjectSettings.isResolveModulePerSourceSet

        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project.project2.main", "project.project1.main", DependencyScope.COMPILE)
            //assertModuleModuleDepScope("project.project3.main", "project.project2.main", DependencyScope.COMPILE)
            assertNoModuleDepForModule("project.project3.main", "project.project1.main")

            TestCase.assertEquals(
                listOf("jar:///project2/build/libs/project2-jar.jar!/"),
                getDependencyLibraryUrls("project.project3.main")
            )

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            /*
             * Note that currently such dependencies can't be imported correctly in "No separate module per source set" mode
             * due to IDEA importer limitations
             */
            assertModuleModuleDepScope("project.project2", "project.project1", DependencyScope.COMPILE)
            if (legacyMode()) {
                assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.TEST, DependencyScope.PROVIDED, DependencyScope.RUNTIME)
            } else {
                assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.COMPILE)
            }

            assertModuleModuleDepScope("project.project3", "project.project1", DependencyScope.COMPILE)

            TestCase.assertEquals(
                emptyList<String>(),
                getDependencyLibraryUrls("project.project3")
            )
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    @Ignore // android.sdk needed
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testTransitiveImplementWithAndroid() {
        configureByFiles()

        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        val isResolveModulePerSourceSet = getCurrentExternalProjectSettings().isResolveModulePerSourceSet
        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3", "project.project1", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("project.project1"), facetSettings("project.project2").implementedModuleNames)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("project.project3", "project.project2", DependencyScope.COMPILE)
            assertModuleModuleDepScope("project.project3", "project.project1", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("project.project1"), facetSettings("project.project2").implementedModuleNames)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    @Ignore // android.sdk needed
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun simpleAndroidAppWithCommonModule() {
        configureByFiles()

        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        val isResolveModulePerSourceSet = currentExternalProjectSettings.isResolveModulePerSourceSet
        try {
            currentExternalProjectSettings.isResolveModulePerSourceSet = true
            importProject()

            assertModuleModuleDepScope("app", "cmn", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("cmn"), facetSettings("jvm").implementedModuleNames)

            currentExternalProjectSettings.isResolveModulePerSourceSet = false
            importProject()

            assertModuleModuleDepScope("app", "cmn", DependencyScope.COMPILE)
            TestCase.assertEquals(listOf("cmn"), facetSettings("jvm").implementedModuleNames)
        } finally {
            currentExternalProjectSettings.isResolveModulePerSourceSet = isResolveModulePerSourceSet
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testJsTestOutputFile() {
        configureByFiles()

        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project.project2.main"))!!.configuration.settings.testOutputPath)
        )
        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project.project2.test"))!!.configuration.settings.testOutputPath)
        )
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testJsProductionOutputFile() {
        configureByFiles()
        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}main/project2.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project.project2.main"))!!.configuration.settings.productionOutputPath)
        )
        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}main/project2.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project.project2.test"))!!.configuration.settings.productionOutputPath)
        )
    }

    @Test
    @Ignore // android.sdk needed
    @PluginTargetVersions(gradleVersionForLatestPlugin = legacyMppImportTestMinVersionForMaster)
    fun testJsTestOutputFileInProjectWithAndroid() {
        configureByFiles()
        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )

        importProject()

        TestCase.assertEquals(
            projectPath + "/project2/build/classes/${if (legacyMode()) "" else "kotlin/"}test/project2_test.js",
            PathUtil.toSystemIndependentName(KotlinFacet.get(getModule("project.project2"))!!.configuration.settings.testOutputPath)
        )
    }

    override fun testDataDirName(): String {
        return "multiplatform"
    }
}
