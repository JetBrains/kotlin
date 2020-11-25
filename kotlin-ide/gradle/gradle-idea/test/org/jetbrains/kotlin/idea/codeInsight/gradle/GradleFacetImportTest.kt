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

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.importing.ImportSpec
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import junit.framework.TestCase
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.caches.project.testSourceInfo
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootMap
import org.jetbrains.kotlin.idea.configuration.allConfigurators
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.projectStructure.sdk
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Ignore
import org.junit.Test

fun KotlinGradleImportingTestCase.facetSettings(moduleName: String): KotlinFacetSettings {
    val facet = KotlinFacet.get(getModule(moduleName)) ?: error("Kotlin facet not found in module $moduleName")
    return facet.configuration.settings
}

val KotlinGradleImportingTestCase.facetSettings: KotlinFacetSettings
    get() = facetSettings("project.main")

val KotlinGradleImportingTestCase.testFacetSettings: KotlinFacetSettings
    get() = facetSettings("project.test")

class GradleFacetImportTest : KotlinGradleImportingTestCase() {
    private fun assertSameKotlinSdks(vararg moduleNames: String) {
        val sdks = moduleNames.map { getModule(it).sdk!! }
        val refSdk = sdks.firstOrNull() ?: return
        assertTrue(refSdk.sdkType is KotlinSdkType)
        assertTrue(sdks.all { it === refSdk })
    }

    @Test
    fun testJvmImport() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertEquals(JvmPlatforms.jvm18, targetPlatform)
            assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            assertEquals(
                "-Xallow-no-source-files -Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }

        with(testFacetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertEquals(JvmPlatforms.jvm16, targetPlatform)
            assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            assertEquals(
                "-Xallow-no-source-files -Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        assertAllModulesConfigured()

        assertEquals(
            listOf(
                "file:///src/main/java" to JavaSourceRootType.SOURCE,
                "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                "file:///src/main/resources" to JavaResourceRootType.RESOURCE
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testJvmImportWithPlugin() {
        configureByFiles()
        importProject()

        assertAllModulesConfigured()
    }

    @Test
    fun testJvmImportWithCustomSourceSets() {
        configureByFiles()
        importProject()

        with(facetSettings("project.myMain")) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertEquals(JvmPlatforms.jvm18, targetPlatform)
            assertEquals("1.7", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            assertEquals(
                "-Xallow-no-source-files -Xdump-declarations-to=tmp -Xsingle-module",
                compilerSettings!!.additionalArguments
            )
        }

        with(facetSettings("project.myTest")) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertEquals(JvmPlatforms.jvm16, targetPlatform)
            assertEquals("1.6", (compilerArguments as K2JVMCompilerArguments).jvmTarget)
            assertEquals(
                "-Xallow-no-source-files -Xdump-declarations-to=tmpTest",
                compilerSettings!!.additionalArguments
            )
        }

        assertAllModulesConfigured()

        assertEquals(
            listOf(
                "file:///src/main/java" to JavaSourceRootType.SOURCE,
                "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                "file:///src/main/resources" to JavaResourceRootType.RESOURCE
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testCoroutineImportByOptions() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testCoroutineImportByProperties() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals(LanguageFeature.State.ENABLED, coroutineSupport)
        }
    }

    @Test
    fun testJsImport() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertTrue(targetPlatform.isJs())

            with(compilerArguments as K2JSCompilerArguments) {
                assertEquals(true, sourceMap)
                assertEquals("plain", moduleKind)
            }

            assertEquals(
                "-main callMain",
                compilerSettings!!.additionalArguments
            )
        }

        with(testFacetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertTrue(targetPlatform.isJs())

            with(compilerArguments as K2JSCompilerArguments) {
                assertEquals(false, sourceMap)
                assertEquals("umd", moduleKind)
            }

            assertEquals(
                "-main callTest",
                compilerSettings!!.additionalArguments
            )
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project.main"))
        val libraryEntries = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>()
        val stdlib = libraryEntries.single { it.libraryName?.contains("js") ?: false }.library

        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)
        assertTrue(stdlib.getFiles(OrderRootType.CLASSES).isNotEmpty())

        assertSameKotlinSdks("project.main", "project.test")

        assertEquals(
            listOf(
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testJsImportTransitive() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertTrue(targetPlatform.isJs())
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project.main"))
        val stdlib = rootManager.orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .map { it.library as LibraryEx }
            .first { "kotlin-stdlib-js" in it.name!! }

        assertEquals(JSLibraryKind, stdlib.kind)

        assertAllModulesConfigured()

        assertEquals(
            listOf(
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testJsImportWithCustomSourceSets() {
        configureByFiles()
        importProject()

        with(facetSettings("project.myMain")) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertTrue(targetPlatform.isJs())

            with(compilerArguments as K2JSCompilerArguments) {
                assertEquals(true, sourceMap)
                assertEquals("plain", moduleKind)
            }

            assertEquals("-main callMain", compilerSettings!!.additionalArguments)
        }

        with(facetSettings("project.myTest")) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertTrue(targetPlatform.isJs())

            with(compilerArguments as K2JSCompilerArguments) {
                assertEquals(false, sourceMap)
                assertEquals("umd", moduleKind)
            }

            assertEquals("-main callTest", compilerSettings!!.additionalArguments)
        }

        assertAllModulesConfigured()

        assertEquals(
            listOf(
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )
        assertEquals(
            listOf(
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testDetectOldJsStdlib() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertTrue(targetPlatform.isJs())
        }
    }

    @Test
    fun testJvmImportByPlatformPlugin() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertEquals(JvmPlatforms.jvm16, targetPlatform)
        }

        assertEquals(
            listOf(
                "file:///src/main/java" to JavaSourceRootType.SOURCE,
                "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                "file:///src/main/resources" to JavaResourceRootType.RESOURCE
            ),
            getSourceRootInfos("project.main")
        )
        assertEquals(
            listOf(
                "file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testJsImportByPlatformPlugin() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertTrue(targetPlatform.isJs())
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project.main"))
        val libraries = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().map { it.library as LibraryEx }
        assertEquals(JSLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-js") == true }.kind)
        assertEquals(CommonLibraryKind, libraries.single { it.name?.contains("kotlin-stdlib-common") == true }.kind)

        assertEquals(
            listOf(
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    @TargetVersions("4.9")
    fun testCommonImportByPlatformPlugin() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.1", languageLevel!!.versionString)
            assertEquals("1.1", apiLevel!!.versionString)
            assertTrue(targetPlatform.isCommon())
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project.main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        assertEquals(
            listOf(
                "file:///src/main/java" to SourceKotlinRootType,
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/java" to TestSourceKotlinRootType,
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testJvmImportByKotlinPlugin() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertEquals(JvmPlatforms.jvm16, targetPlatform)
        }

        assertEquals(
            listOf(
                "file:///src/main/java" to JavaSourceRootType.SOURCE,
                "file:///src/main/kotlin" to JavaSourceRootType.SOURCE,
                "file:///src/main/resources" to JavaResourceRootType.RESOURCE
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/java" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/kotlin" to JavaSourceRootType.TEST_SOURCE,
                "file:///src/test/resources" to JavaResourceRootType.TEST_RESOURCE
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testJsImportByKotlin2JsPlugin() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
            assertTrue(targetPlatform.isJs())
        }

        assertEquals(
            listOf(
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )

        assertEquals(
            listOf(
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testArgumentEscaping() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals(
                listOf("-Xallow-no-source-files", "-Xbuild-file=module with spaces"),
                compilerSettings!!.additionalArgumentsAsList
            )
        }
    }

    @Test
    fun testNoPluginsInAdditionalArgs() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("-version", compilerSettings!!.additionalArguments)
            assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.stereotype.Component",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.transaction.annotation.Transactional",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.scheduling.annotation.Async",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.cache.annotation.Cacheable",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.boot.test.context.SpringBootTest",
                    "plugin:org.jetbrains.kotlin.allopen:annotation=org.springframework.validation.annotation.Validated"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    @Test
    fun testNoArgInvokeInitializers() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals(
                "-version",
                compilerSettings!!.additionalArguments
            )
            assertEquals(
                listOf(
                    "plugin:org.jetbrains.kotlin.noarg:annotation=NoArg",
                    "plugin:org.jetbrains.kotlin.noarg:invokeInitializers=true"
                ),
                compilerArguments!!.pluginOptions!!.toList()
            )
        }
    }

    @Test
    @Ignore // android.sdk needed
    fun testAndroidGradleJsDetection() {
        configureByFiles()
        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )
        importProject()

        with(facetSettings("js-module")) {
            assertTrue(targetPlatform.isJs())
        }

        val rootManager = ModuleRootManager.getInstance(getModule("js-module"))
        val stdlib = rootManager
            .orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .first { it.libraryName?.startsWith("Gradle: kotlin-stdlib-js-") ?: false }
            .library!!

        assertTrue(stdlib.getFiles(OrderRootType.CLASSES).isNotEmpty())
        assertEquals(JSLibraryKind, (stdlib as LibraryEx).kind)
    }

    @Test
    @Ignore // android.sdk needed
    fun testKotlinAndroidPluginDetection() {
        configureByFiles()
        createProjectSubFile(
            "local.properties", """
            sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}
        """
        )
        importProject()

        assertNotNull(KotlinFacet.get(getModule("project")))
    }

    @Test
    fun testNoFacetInModuleWithoutKotlinPlugin() {
        configureByFiles()

        importProject()

        assertNotNull(KotlinFacet.get(getModule("gr01.main")))
        assertNotNull(KotlinFacet.get(getModule("gr01.test")))
        assertNull(KotlinFacet.get(getModule("gr01.m1.main")))
        assertNull(KotlinFacet.get(getModule("gr01.m1.test")))
    }

    @Test
    fun testClasspathWithDependenciesImport() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("tmp.jar", (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    @Test
    fun testDependenciesClasspathImport() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals(null, (compilerArguments as K2JVMCompilerArguments).classpath)
        }
    }

    @Test
    fun testJDKImport() {
        val mockJdkPath = "${PathManager.getHomePath()}/community/java/mockJDK-1.8"
        object : WriteAction<Unit>() {
            override fun run(result: Result<Unit>) {
                val jdk = JavaSdk.getInstance().createJdk("myJDK", mockJdkPath)
                getProjectJdkTableSafe().addJdk(jdk)
                ProjectRootManager.getInstance(myProject).projectSdk = jdk
            }
        }.execute()

        try {
            configureByFiles()
            importProject()

            val moduleSDK = ModuleRootManager.getInstance(getModule("project.main")).sdk!!
            assertTrue(moduleSDK.sdkType is JavaSdk)
            assertEquals("myJDK", moduleSDK.name)
            assertEquals(mockJdkPath, moduleSDK.homePath)
        } finally {
            object : WriteAction<Unit>() {
                override fun run(result: Result<Unit>) {
                    val jdkTable = getProjectJdkTableSafe()
                    jdkTable.removeJdk(jdkTable.findJdk("myJDK")!!)
                    ProjectRootManager.getInstance(myProject).projectSdk = null
                }
            }.execute()
        }
    }

    @Test
    fun testImplementsDependency() {
        configureByFiles()
        importProject()

        assertEquals(listOf("MultiTest.main"), facetSettings("MultiTest.MultiTest-jvm.main").implementedModuleNames)
        assertEquals(listOf("MultiTest.test"), facetSettings("MultiTest.MultiTest-jvm.test").implementedModuleNames)
        assertEquals(listOf("MultiTest.main"), facetSettings("MultiTest.MultiTest-js.main").implementedModuleNames)
        assertEquals(listOf("MultiTest.test"), facetSettings("MultiTest.MultiTest-js.test").implementedModuleNames)
    }

    @Test
    fun testImplementsDependencyWithCustomSourceSets() {
        configureByFiles()

        importProject()

        assertEquals(listOf("MultiTest.myMain"), facetSettings("MultiTest.MultiTest-jvm.myMain").implementedModuleNames)
        assertEquals(listOf("MultiTest.myTest"), facetSettings("MultiTest.MultiTest-jvm.myTest").implementedModuleNames)
        assertEquals(listOf("MultiTest.myMain"), facetSettings("MultiTest.MultiTest-js.myMain").implementedModuleNames)
        assertEquals(listOf("MultiTest.myTest"), facetSettings("MultiTest.MultiTest-js.myTest").implementedModuleNames)
    }

    @Test
    fun testAPIVersionExceedingLanguageVersion() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
        }

        assertAllModulesConfigured()
    }

    @Test
    fun testIgnoreProjectLanguageAndAPIVersion() {
        KotlinCommonCompilerArgumentsHolder.getInstance(myProject).update {
            languageVersion = "1.0"
            apiVersion = "1.0"
        }

        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.3", languageLevel!!.versionString)
            assertEquals("1.3", apiLevel!!.versionString)
        }

        assertAllModulesConfigured()
    }

    @Test
    fun testCommonArgumentsImport() {
        configureByFiles()
        importProject()

        with(facetSettings) {
            assertEquals("1.1", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertTrue(targetPlatform.isCommon())
            assertEquals("my/classpath", (compilerArguments as K2MetadataCompilerArguments).classpath)
            assertEquals("my/destination", (compilerArguments as K2MetadataCompilerArguments).destination)
        }

        with(facetSettings("project.test")) {
            assertEquals("1.1", languageLevel!!.versionString)
            assertEquals("1.0", apiLevel!!.versionString)
            assertFalse(compilerArguments!!.autoAdvanceLanguageVersion)
            assertFalse(compilerArguments!!.autoAdvanceApiVersion)
            assertTrue(targetPlatform.isCommon())
            assertEquals("my/test/classpath", (compilerArguments as K2MetadataCompilerArguments).classpath)
            assertEquals("my/test/destination", (compilerArguments as K2MetadataCompilerArguments).destination)
        }

        val rootManager = ModuleRootManager.getInstance(getModule("project.main"))
        val stdlib = rootManager.orderEntries.filterIsInstance<LibraryOrderEntry>().single().library
        assertEquals(CommonLibraryKind, (stdlib as LibraryEx).kind)

        assertSameKotlinSdks("project.main", "project.test")

        assertEquals(
            listOf(
                "file:///src/main/java" to SourceKotlinRootType,
                "file:///src/main/kotlin" to SourceKotlinRootType,
                "file:///src/main/resources" to ResourceKotlinRootType
            ),
            getSourceRootInfos("project.main")
        )
        assertEquals(
            listOf(
                "file:///src/test/java" to TestSourceKotlinRootType,
                "file:///src/test/kotlin" to TestSourceKotlinRootType,
                "file:///src/test/resources" to TestResourceKotlinRootType
            ),
            getSourceRootInfos("project.test")
        )
    }

    @Test
    fun testInternalArgumentsFacetImporting() {
        configureByFiles()
        importProject()

        // Version is indeed 1.3
        assertEquals(LanguageVersion.KOTLIN_1_3, facetSettings.languageLevel)

        // We haven't lost internal argument during importing to facet
        assertEquals("-Xallow-no-source-files -XXLanguage:+InlineClasses", facetSettings.compilerSettings?.additionalArguments)

        // Inline classes are enabled even though LV = 1.3
        assertEquals(
            LanguageFeature.State.ENABLED,
            getModule("project.main").languageVersionSettings.getFeatureSupport(LanguageFeature.InlineClasses)
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradleJS() {
        configureByFiles()
        importProject()

        checkStableModuleName("project.main", "project", JsPlatforms.defaultJsPlatform, isProduction = true)
        // Note "_test" suffix: this is current behavior of K2JS Compiler
        checkStableModuleName("project.test", "project_test", JsPlatforms.defaultJsPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testStableModuleNameWhileUsingGradleJVM() {
        configureByFiles()
        importProject()

        checkStableModuleName("project.main", "project", JvmPlatforms.unspecifiedJvmPlatform, isProduction = true)
        checkStableModuleName("project.test", "project", JvmPlatforms.unspecifiedJvmPlatform, isProduction = false)

        assertAllModulesConfigured()
    }

    @Test
    fun testNoFriendPathsAreShown() {
        configureByFiles()
        importProject()

        assertEquals(
            "-Xallow-no-source-files",
            testFacetSettings.compilerSettings!!.additionalArguments
        )

        assertAllModulesConfigured()
    }

    @Test
    fun testSharedLanguageVersion() {
        configureByFiles()

        val holder = KotlinCommonCompilerArgumentsHolder.getInstance(myProject)
        holder.update { languageVersion = "1.1" }

        importProject()

        TestCase.assertEquals("1.3", holder.settings.languageVersion)
    }

    @Test
    fun testNonSharedLanguageVersion() {
        configureByFiles()

        val holder = KotlinCommonCompilerArgumentsHolder.getInstance(myProject)
        holder.update { languageVersion = "1.1" }

        importProject()

        TestCase.assertEquals("1.1", holder.settings.languageVersion)
    }

    @Test
    fun testImportCompilerArgumentsWithInvalidDependencies() {
        configureByFiles()
        importProject()

        with(facetSettings("project.main")) {
            assertEquals("1.8", (mergedCompilerArguments as K2JVMCompilerArguments).jvmTarget)
        }
    }

    private fun checkStableModuleName(projectName: String, expectedName: String, platform: TargetPlatform, isProduction: Boolean) {
        val module = getModule(projectName)
        val moduleInfo = if (isProduction) module.productionSourceInfo() else module.testSourceInfo()

        val resolutionFacade = KotlinCacheService.getInstance(myProject).getResolutionFacadeByModuleInfo(moduleInfo!!, platform)!!
        val moduleDescriptor = resolutionFacade.moduleDescriptor

        assertEquals("<$expectedName>", moduleDescriptor.stableName?.asString())
    }

    private fun assertAllModulesConfigured() {
        runReadAction {
            for (moduleGroup in ModuleSourceRootMap(myProject).groupByBaseModules(myProject.allModules())) {
                val configurator = allConfigurators().find {
                    it.getStatus(moduleGroup) == ConfigureKotlinStatus.CAN_BE_CONFIGURED
                }
                assertNull("Configurator $configurator tells that ${moduleGroup.baseModule} can be configured", configurator)
            }
        }
    }

    override fun createImportSpec(): ImportSpec {
        return ImportSpecBuilder(super.createImportSpec())
            .createDirectoriesForEmptyContentRoots()
            .build()
    }

    override fun testDataDirName(): String {
        return "gradleFacetImportTest"
    }
}
