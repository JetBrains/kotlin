/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.externalSystem.importing.ImportSpec
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.codeInsight.gradle.mppImportTestMinVersionForMaster
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestRunConfigurationProducer
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class NewMultiplatformProjectImportingTest : MultiplePluginVersionGradleImportingTestCaseWithSdkChecker() {
    @Before
    fun saveSdksBeforeTest() {
        val kotlinSdks = sdkCreationChecker.getKotlinSdks()
        if (kotlinSdks.isNotEmpty()) {
            fail("Found Kotlin SDK before importing test. Sdk list: $kotlinSdks")
        }
    }

    @After
    fun checkSdkCreated() {
        if (!sdkCreationChecker.isKotlinSdkCreated()) {
            fail("Kotlin SDK was not created during import of MPP Project.")
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.10+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testProjectDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure {
            allModules {
                languageVersion("1.3")
                apiVersion("1.3")
                when (module.name) {
                    "project", "project.app", "project.lib" -> additionalArguments(null)
                    "project.app.jvmMain", "project.app.jvmTest", "project.lib.jvmMain", "project.lib.jvmTest" ->
                        additionalArguments(
                            if (VersionComparatorUtil.compare(gradleKotlinPluginVersion, "1.3.50") < 0)
                                "-version"
                            else
                                "-Xallow-no-source-files"
                        )
                    else -> additionalArguments("-version")
                }
            }

            module("project")

            module("project.app")

            module("project.app.commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/commonMain/resources", ResourceKotlinRootType)
                inheritProjectOutput()
            }

            module("project.app.commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                moduleDependency("project.app.commonMain", DependencyScope.TEST)
                sourceFolder("app/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/commonTest/resources", TestResourceKotlinRootType)
                inheritProjectOutput()
            }

            module("project.app.jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.lib.jsMain", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.app.commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/jsMain/resources", ResourceKotlinRootType)
                outputPath("app/build/classes/kotlin/js/main", true)
            }

            module("project.app.jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.lib.jsMain", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.app.commonMain", DependencyScope.TEST)
                moduleDependency("project.app.commonTest", DependencyScope.TEST)
                moduleDependency("project.app.jsMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.app.jsMain", DependencyScope.TEST)
                sourceFolder("app/src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/jsTest/resources", TestResourceKotlinRootType)
                outputPath("app/build/classes/kotlin/js/test", false)
            }

            module("project.app.jvmMain") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("project.lib.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.app.main", DependencyScope.COMPILE)
                moduleDependency("project.app.commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/main", true)
            }

            module("project.app.jvmTest") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("project.lib.jvmMain", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                moduleDependency("project.app.test", DependencyScope.TEST)
                moduleDependency("project.app.jvmMain", DependencyScope.TEST)
                moduleDependency("project.app.commonMain", DependencyScope.TEST)
                moduleDependency("project.app.commonTest", DependencyScope.TEST)
                sourceFolder("app/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("app/build/classes/kotlin/jvm/test", false)
            }

            module("project.app.main") {
                platform(JvmPlatforms.jvm18)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/resources", JavaResourceRootType.RESOURCE)
                inheritProjectOutput()
            }

            module("project.app.test") {
                platform(JvmPlatforms.jvm18)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                moduleDependency("project.app.main", DependencyScope.TEST)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/resources", JavaResourceRootType.TEST_RESOURCE)
                inheritProjectOutput()
            }

            module("project.lib")

            module("project.lib.commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                sourceFolder("lib/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("lib/src/commonMain/resources", ResourceKotlinRootType)
                inheritProjectOutput()
            }

            module("project.lib.commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                sourceFolder("lib/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("lib/src/commonTest/resources", TestResourceKotlinRootType)
                inheritProjectOutput()
            }

            module("project.lib.jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jsMain/kotlin", SourceKotlinRootType)
                sourceFolder("lib/src/jsMain/resources", ResourceKotlinRootType)
                outputPath("lib/build/classes/kotlin/js/main", true)
            }

            module("project.lib.jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-js:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                moduleDependency("project.lib.commonTest", DependencyScope.TEST)
                moduleDependency("project.lib.jsMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.lib.jsMain", DependencyScope.TEST)
                sourceFolder("lib/src/jsTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("lib/src/jsTest/resources", TestResourceKotlinRootType)
                outputPath("lib/build/classes/kotlin/js/test", false)
            }

            module("project.lib.jvmMain") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("project.lib.commonMain", DependencyScope.COMPILE)
                sourceFolder("lib/src/jvmMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("lib/src/jvmMain/resources", JavaResourceRootType.RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/main", true)
            }

            module("project.lib.jvmTest") {
                platform(JvmPlatforms.jvm16)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("project.lib.commonTest", DependencyScope.TEST)
                moduleDependency("project.lib.commonMain", DependencyScope.TEST)
                moduleDependency("project.lib.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.lib.jvmMain", DependencyScope.TEST)
                sourceFolder("lib/src/jvmTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("lib/src/jvmTest/resources", JavaResourceRootType.TEST_RESOURCE)
                outputPath("lib/build/classes/kotlin/jvm/test", false)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testFileCollectionDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            exhaustiveModuleList = false,
            exhaustiveSourceSourceRootList = false
        ) {
            module("project.jvmMain") {
                libraryDependencyByUrl("file://$projectPath/a", DependencyScope.COMPILE)
                libraryDependencyByUrl("file://$projectPath/b", DependencyScope.COMPILE)
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.main", DependencyScope.COMPILE)
            }
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.30+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testUnresolvedDependency() {
        configureByFiles()
        importProject()

        checkProjectStructure(
            exhaustiveSourceSourceRootList = false,
            exhaustiveDependencyList = false
        ) {
            module("project")
            module("project.commonMain")
            module("project.commonTest")
            module("project.jvmMain")
            module("project.jvmTest")
            module("project.main")
            module("project.test")
        }
    }

    @Test
    @Ignore // android.sdk needed
    @PluginTargetVersions(gradleVersion = "5.0+", pluginVersion = "1.3.30+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testAndroidDependencyOnMPP() {
        configureByFiles()
        createProjectSubFile(
            "local.properties",
            "sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}"
        )
        importProject()

        checkProjectStructure {
            module("project")

            module("project.app") {
                libraryDependency("Gradle: android.arch.core:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.core:runtime:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:common:1.1.0@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:livedata-core:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:runtime:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: android.arch.lifecycle:viewmodel:1.1.0@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout:1.1.3@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.constraint:constraint-layout-solver:1.1.3@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-core:3.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test.espresso:espresso-idling-resource:3.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:monitor:1.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support.test:runner:1.0.2@aar", DependencyScope.TEST)
                libraryDependency("Gradle: com.android.support:animated-vector-drawable:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:appcompat-v7:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-annotations:27.1.1@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-compat:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-ui:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-core-utils:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-fragment:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.android.support:support-vector-drawable:27.1.1@aar", DependencyScope.COMPILE)
                libraryDependency("Gradle: com.google.code.findbugs:jsr305:2.0.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: com.squareup:javawriter:2.1.1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: javax.inject:javax.inject:1@jar", DependencyScope.TEST)
                libraryDependency("Gradle: junit:junit:4.12@jar", DependencyScope.TEST)
                libraryDependency("Gradle: net.sf.kxml:kxml2:2.3.0@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-core:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-integration:1.3@jar", DependencyScope.TEST)
                libraryDependency("Gradle: org.hamcrest:hamcrest-library:1.3@jar", DependencyScope.TEST)
                if (gradleKotlinPluginVersion != MINIMAL_SUPPORTED_VERSION) {
                    libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-android-extensions-runtime:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                }
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-jdk7:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}@jar", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0@jar", DependencyScope.COMPILE)
                moduleDependency("project.shared", DependencyScope.COMPILE)
                moduleDependency("project.shared.androidMain", DependencyScope.COMPILE)
                moduleDependency("project.shared.androidTest", DependencyScope.TEST)
                moduleDependency("project.shared.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.shared.commonTest", DependencyScope.TEST)
            }

            module("project.shared")

            module("project.shared.commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                sourceFolder("shared/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("shared/src/commonMain/resources", ResourceKotlinRootType)
            }

            module("project.shared.commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.shared.commonMain", DependencyScope.TEST)
                sourceFolder("shared/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("shared/src/commonTest/resources", TestResourceKotlinRootType)
            }

            module("shared.androidMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.COMPILE)
                moduleDependency("project.shared.commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/androidMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("shared/src/androidMain/resources", JavaResourceRootType.RESOURCE)
            }

            module("project.shared.androidTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                libraryDependency("Gradle: org.jetbrains:annotations:13.0", DependencyScope.TEST)
                moduleDependency("project.shared.androidMain", DependencyScope.TEST)
                moduleDependency("project.shared.commonMain", DependencyScope.TEST)
                moduleDependency("project.shared.commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/androidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("shared/src/androidTest/resources", JavaResourceRootType.TEST_RESOURCE)
            }

            val nativeVersion = gradleKotlinPluginVersion

            module("project.shared.iOSMain") {
                libraryDependency("Kotlin/Native $nativeVersion - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.shared.commonMain", DependencyScope.COMPILE)
                sourceFolder("shared/src/iOSMain/kotlin", SourceKotlinRootType)
                sourceFolder("shared/src/iOSMain/resources", ResourceKotlinRootType)
            }

            module("project.shared.iOSTest") {
                libraryDependency("Kotlin/Native $nativeVersion - stdlib", DependencyScope.PROVIDED)
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.shared.iOSMain", DependencyScope.TEST)
                moduleDependency("project.shared.commonMain", DependencyScope.TEST)
                moduleDependency("project.shared.commonTest", DependencyScope.TEST)
                sourceFolder("shared/src/iOSTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("shared/src/iOSTest/resources", TestResourceKotlinRootType)
            }
        }
    }

    @Test
    @PluginTargetVersions(gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testTestTasks() {
        val files = configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")
            module("project.common")
            module("project.jvm")
            module("project.js")

            module("project.commonMain")
            module("project.commonTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
            }

            module("project.jvmMain") {
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
            }

            module("project.jvmTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
                moduleDependency("project.commonTest", DependencyScope.TEST)
                moduleDependency("project.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.jvmMain", DependencyScope.TEST)
            }
        }

        val commonTestFile = files.find { it.path.contains("commonTest") }!!
        val commonTasks = findTasksToRun(commonTestFile)
        if (commonTasks != null) {
            assertEquals(listOf(":cleanJvmTest", ":jvmTest"), commonTasks)
        }

        val jvmTestFile = files.find { it.path.contains("jvmTest") }!!
        val jvmTasks = findTasksToRun(jvmTestFile)
        if (jvmTasks != null) {
            assertEquals(listOf(":cleanJvmTest", ":jvmTest"), jvmTasks)
        }
    }


    @Test
    @PluginTargetVersions(pluginVersion = "1.3.50+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testImportTestsAndTargets() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false, exhaustiveTestsList = true) {
            module("project")
            module("project.commonMain")
            module("project.commonTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")
            }
            module("project.jsMain")
            module("project.jsTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
            }
            module("project.jvmMain")
            module("project.jvmTest") {
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")
            }
        }
    }

    @Test
    @Ignore // Android plugin needed
    @PluginTargetVersions(gradleVersion = "5.0+", pluginVersion = "1.3.50+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testSingleAndroidTarget() {
        configureByFiles()
        importProject()
        checkProjectStructure(exhaustiveDependencyList = false) {
            module("project.app") {
                sourceFolder("app/src/androidAndroidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidAndroidTestDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidDebug/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidDebugAndroidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidDebugUnitTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidMain/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidRelease/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/androidReleaseUnitTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTest/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTest/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestDebug/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/androidTestRelease/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/debug/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/debug/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/main/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/release/java", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/release/kotlin", JavaSourceRootType.SOURCE)
                sourceFolder("app/src/test/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/test/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testDebug/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testDebug/kotlin", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testRelease/java", JavaSourceRootType.TEST_SOURCE)
                sourceFolder("app/src/testRelease/kotlin", JavaSourceRootType.TEST_SOURCE)

                sourceFolder("app/src/androidAndroidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidAndroidTestDebug/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidDebug/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidDebugAndroidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidDebugUnitTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidMain/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidRelease/resources", JavaResourceRootType.RESOURCE)
                sourceFolder("app/src/androidReleaseUnitTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTest/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTestDebug/resources", JavaResourceRootType.TEST_RESOURCE)
                sourceFolder("app/src/androidTestRelease/resources", JavaResourceRootType.TEST_RESOURCE)
            }
            module("project.app.commonMain") {
                sourceFolder("app/src/commonMain/kotlin", SourceKotlinRootType)
                sourceFolder("app/src/commonMain/resources", ResourceKotlinRootType)
            }
            module("project.app.commonTest") {
                sourceFolder("app/src/commonTest/kotlin", TestSourceKotlinRootType)
                sourceFolder("app/src/commonTest/resources", TestResourceKotlinRootType)
            }
            module("project")
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.10+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testDependencyOnRoot() {
        configureByFiles()
        importProject()
        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")

            module("project.commonMain")

            module("project.commonTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
            }

            module("project.jvmMain") {
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
            }

            module("project.jvmTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
                moduleDependency("project.commonTest", DependencyScope.TEST)
                moduleDependency("project.jvmMain", DependencyScope.RUNTIME)
                moduleDependency("project.jvmMain", DependencyScope.TEST)
            }

            module("project.subproject")

            module("project.subproject.commonMain") {
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
            }

            module("project.subproject.commonTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
                moduleDependency("project.subproject.commonMain", DependencyScope.TEST)
            }

            module("project.subproject.jvmMain") {
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.subproject.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.jvmMain", DependencyScope.COMPILE)
            }

            module("project.subproject.jvmTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.jvmMain", DependencyScope.TEST)
                moduleDependency("project.subproject.commonMain", DependencyScope.TEST)
                moduleDependency("project.subproject.commonTest", DependencyScope.TEST)
                moduleDependency("project.subproject.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.subproject.jvmMain", DependencyScope.TEST)
            }
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.10+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testNestedDependencies() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")

            module("project.aaa")

            module("project.aaa.commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.bbb.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.ccc.commonMain", DependencyScope.COMPILE)
            }

            module("project.aaa.commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.aaa.commonMain", DependencyScope.TEST)
                moduleDependency("project.bbb.commonMain", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST)
            }

            module("project.aaa.jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.aaa.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.bbb.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.bbb.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.ccc.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.ccc.jvmMain", DependencyScope.COMPILE)
            }

            module("project.aaa.jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.aaa.commonMain", DependencyScope.TEST)
                moduleDependency("project.aaa.commonTest", DependencyScope.TEST)
                moduleDependency("project.aaa.jvmMain", DependencyScope.RUNTIME)
                moduleDependency("project.aaa.jvmMain", DependencyScope.TEST)
                moduleDependency("project.bbb.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.bbb.jvmMain", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.ccc.jvmMain", DependencyScope.TEST)
            }

            module("project.bbb")

            module("project.bbb.commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.ccc.commonMain", DependencyScope.COMPILE)
            }

            module("project.bbb.commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.bbb.commonMain", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST)
            }

            module("project.bbb.jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.bbb.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.ccc.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.ccc.jvmMain", DependencyScope.COMPILE)
            }

            module("project.bbb.jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.bbb.commonMain", DependencyScope.TEST)
                moduleDependency("project.bbb.commonTest", DependencyScope.TEST)
                moduleDependency("project.bbb.jvmMain", DependencyScope.RUNTIME)
                moduleDependency("project.bbb.jvmMain", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.ccc.jvmMain", DependencyScope.TEST)
            }

            module("project.ccc")

            module("project.ccc.commonMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
            }

            module("project.ccc.commonTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST)
            }

            module("project.ccc.jvmMain") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.COMPILE)
                moduleDependency("project.ccc.commonMain", DependencyScope.COMPILE)
            }

            module("project.ccc.jvmTest") {
                libraryDependency("Gradle: org.jetbrains.kotlin:kotlin-stdlib-common:${gradleKotlinPluginVersion}", DependencyScope.TEST)
                moduleDependency("project.ccc.commonMain", DependencyScope.TEST)
                moduleDependency("project.ccc.commonTest", DependencyScope.TEST)
                moduleDependency("project.ccc.jvmMain", DependencyScope.RUNTIME)
                moduleDependency("project.ccc.jvmMain", DependencyScope.TEST)
            }
        }
    }

    @Test
    @Ignore // android.sdk needed
    @PluginTargetVersions(gradleVersion = "5.0+", pluginVersion = "1.3.20+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testDetectAndroidSources() {
        configureByFiles()
        createProjectSubFile(
            "local.properties",
            "sdk.dir=/${KotlinTestUtils.getAndroidSdkSystemIndependentPath()}"
        )
        importProject(true)
        checkProjectStructure(exhaustiveModuleList = false, exhaustiveDependencyList = false, exhaustiveSourceSourceRootList = false) {
            module("multiplatformb") {
                sourceFolder("multiplatformb/src/androidMain/kotlin", JavaSourceRootType.SOURCE)


            }
        }
    }

    /**
     * This test is inherited form testPlatformToCommonExpectedByInComposite and actually tests
     * dependencies in multiplatform project included in composite build
     */
    @Test
    @PluginTargetVersions(pluginVersion = "1.3.20+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testPlatformToCommonExpByInComposite() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(exhaustiveSourceSourceRootList = false) {
            module("project")

            module("project.commonMain")

            module("project.commonTest") {
                moduleDependency("project.commonMain", DependencyScope.TEST)
            }

            module("project.jvmMain") {
                moduleDependency("project.commonMain", DependencyScope.COMPILE)
            }
            module("project.jvmTest"){
                moduleDependency("project.commonMain", DependencyScope.TEST)
                moduleDependency("project.commonTest", DependencyScope.TEST)
                moduleDependency("project.jvmMain", DependencyScope.TEST)
                moduleDependency("project.jvmMain", DependencyScope.RUNTIME)
            }

            module("toInclude")

            module("toInclude.commonMain")

            module("toInclude.commonTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
            }

            module("toInclude.jsMain") {
                moduleDependency("toInclude.commonMain", DependencyScope.COMPILE)
            }

            module("toInclude.jsTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
                moduleDependency("toInclude.commonTest", DependencyScope.TEST)
                moduleDependency("toInclude.jsMain", DependencyScope.RUNTIME)
                moduleDependency("toInclude.jsMain", DependencyScope.TEST)
                moduleDependency("toInclude.jsMain", DependencyScope.RUNTIME)
            }

            module("toInclude.jvmMain") {
                moduleDependency("toInclude.commonMain", DependencyScope.COMPILE)
            }

            module("toInclude.jvmTest") {
                moduleDependency("toInclude.commonMain", DependencyScope.TEST)
                moduleDependency("toInclude.commonTest", DependencyScope.TEST)
                moduleDependency("toInclude.jvmMain", DependencyScope.RUNTIME)
                moduleDependency("toInclude.jvmMain", DependencyScope.TEST)
                moduleDependency("toInclude.jvmMain", DependencyScope.RUNTIME)
            }
        }
    }

    /**
     * Test case for issue https://youtrack.jetbrains.com/issue/KT-29757
     */
    @Test
    @PluginTargetVersions(pluginVersion = "1.3.40+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testJavaTransitiveOnMPP() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("project")

            module("project.jvm")

            module("project.jvm.main") {
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
            }

            module("project.jvm.test") {
                moduleDependency("project.jvm.main", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.jvmMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
            }

            module("project.mpp")

            module("project.mpp.commonMain") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
            }

            module("project.mpp.commonTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp.commonMain", DependencyScope.TEST)
            }

            module("project.mpp.jvmMain") {
                moduleDependency("project.mpp.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.COMPILE)
            }

            module("project.mpp.jvmTest") {
                moduleDependency("project.mpp.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp.commonTest", DependencyScope.TEST, productionOnTest = true)
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.TEST)
                moduleDependency("project.mpp.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.mpp.jvmMain", DependencyScope.TEST)
            }

            module("project.mpp-base")

            module("project.mpp-base.commonMain")

            module("project.mpp-base.commonTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
            }

            module("project.mpp-base.jvmMain") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.COMPILE)
            }

            module("project.mpp-base.jvmTest") {
                moduleDependency("project.mpp-base.commonMain", DependencyScope.TEST)
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("project.mpp-base.jvmMain", DependencyScope.TEST)
                moduleDependency("project.mpp-base.commonTest", DependencyScope.TEST, productionOnTest = true)
            }
        }
    }

    /**
     * Test case for issue https://youtrack.jetbrains.com/issue/KT-28822
     */
    @Test
    @PluginTargetVersions(pluginVersion = "1.3.41+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testImportBeforeBuild() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("mpp-jardep")

            module("mpp-jardep.java-project")

            module("mpp-jardep.java-project.main") {
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)

            }

            module("mpp-jardep.java-project.test") {
                moduleDependency("mpp-jardep.java-project.main", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)
            }

            module("mpp-jardep.library1")

            module("mpp-jardep.library1.commonMain")

            module("mpp-jardep.library1.commonTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST)

            }

            module("mpp-jardep.library1.jvmMain") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE)

            }

            module("mpp-jardep.library1.jvmTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library1.commonTest", DependencyScope.TEST, productionOnTest = true)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.TEST)
            }

            module("mpp-jardep.library2")

            module("mpp-jardep.library2.commonMain")

            module("mpp-jardep.library2.commonTest") {
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.TEST)
            }

            module("mpp-jardep.library2.jvmMain") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.COMPILE, allowMultiple = true)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.COMPILE)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.COMPILE)
            }

            module("mpp-jardep.library2.jvmTest") {
                moduleDependency("mpp-jardep.library1.commonMain", DependencyScope.TEST, allowMultiple = true)
                moduleDependency("mpp-jardep.library1.jvmMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library2.commonMain", DependencyScope.TEST)
                moduleDependency("mpp-jardep.library2.commonTest", DependencyScope.TEST, productionOnTest = true)
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("mpp-jardep.library2.jvmMain", DependencyScope.TEST)
            }
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.20+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testProductionOnTestFlag() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(false, false, false ) {
            module("project.javaModule.test") {
                moduleDependency("project.mppModule.jvmTest", DependencyScope.COMPILE, productionOnTest = true)
            }
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.30+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testJvmWithJava() {
        configureByFiles()
        importProject(true)

        checkProjectStructure(true, false, true) {
            module("jvm-on-mpp")

            module("jvm-on-mpp.jvm-mod")

            module("jvm-on-mpp.jvm-mod.main") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.COMPILE, productionOnTest = false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, productionOnTest = false)
            }

            module("jvm-on-mpp.jvm-mod.test") {
                moduleDependency("jvm-on-mpp.jvm-mod.main", DependencyScope.COMPILE, productionOnTest = false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.COMPILE, productionOnTest = false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, productionOnTest = false)
            }

            module("jvm-on-mpp.mpp-mod-a")

            module("jvm-on-mpp.mpp-mod-a.commonMain")

            module("jvm-on-mpp.mpp-mod-a.commonTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, productionOnTest = false)
            }

            module("jvm-on-mpp.mpp-mod-a.jsMain") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, productionOnTest = false)
            }

            module("jvm-on-mpp.mpp-mod-a.jsTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, productionOnTest = false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonTest", DependencyScope.TEST, productionOnTest = true)
                moduleDependency("jvm-on-mpp.mpp-mod-a.jsMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("jvm-on-mpp.mpp-mod-a.jsMain", DependencyScope.TEST, productionOnTest = false)
            }

            module("jvm-on-mpp.mpp-mod-a.jvmMain") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.COMPILE, productionOnTest = false)
            }

            module("jvm-on-mpp.mpp-mod-a.jvmTest") {
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonMain", DependencyScope.TEST, productionOnTest = false)
                moduleDependency("jvm-on-mpp.mpp-mod-a.commonTest", DependencyScope.TEST, productionOnTest = true)
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.RUNTIME) // Temporary dependency, need to remove after KT-40551 is solved
                moduleDependency("jvm-on-mpp.mpp-mod-a.jvmMain", DependencyScope.TEST, productionOnTest = false)
            }

            //At the moment this is 'fake' source roots and they have no explicit dependencies.
            module("jvm-on-mpp.mpp-mod-a.main")

            module("jvm-on-mpp.mpp-mod-a.test")
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.30+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testCommonTestTargetPlatform() {
        configureByFiles()
        importProject(true)
        checkProjectStructure(true, false, false) {
            module("KotlinMPPL") {}
            module("KotlinMPPL.commonMain") {
                platform(CommonPlatforms.defaultCommonPlatform)
            }
            module("KotlinMPPL.commonTest") {
                platform(CommonPlatforms.defaultCommonPlatform)
            }
            module("KotlinMPPL.jsMain") {
                platform(JsPlatforms.defaultJsPlatform)
            }
            module("KotlinMPPL.jsTest") {
                platform(JsPlatforms.defaultJsPlatform)
            }
        }
    }

    @Test
    @PluginTargetVersions(pluginVersion = "1.3.60+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testIgnoreIncompatibleNativeTestTasks() {
        configureByFiles()
        importProject()

        checkProjectStructure(exhaustiveSourceSourceRootList = false, exhaustiveDependencyList = false, exhaustiveTestsList = true) {
            module("project")

            module("project.commonMain")

            module("project.commonTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")

                when {
                    HostManager.hostIsMac -> externalSystemTestTask("macosTest", "project:macosTest", "macos")
                    HostManager.hostIsMingw -> externalSystemTestTask("winTest", "project:winTest", "win")
                    HostManager.hostIsLinux -> externalSystemTestTask("linuxTest", "project:linuxTest", "linux")
                }
            }

            module("project.jsMain")

            module("project.jsTest") {
                externalSystemTestTask("jsBrowserTest", "project:jsTest", "js")
                externalSystemTestTask("jsNodeTest", "project:jsTest", "js")
            }

            module("project.jvmMain")

            module("project.jvmTest") {
                externalSystemTestTask("jvmTest", "project:jvmTest", "jvm")
            }

            module("project.macosMain")

            module("project.macosTest") {
                if (HostManager.hostIsMac) externalSystemTestTask("macosTest", "project:macosTest", "macos")
            }

            module("project.winMain")

            module("project.winTest") {
                if (HostManager.hostIsMingw) externalSystemTestTask("winTest", "project:winTest", "win")
            }

            module("project.linuxMain")

            module("project.linuxTest") {
                if (HostManager.hostIsLinux) externalSystemTestTask("linuxTest", "project:linuxTest", "linux")
            }
        }
    }


    @Test
    @PluginTargetVersions(pluginVersion = "1.3.30+", gradleVersionForLatestPlugin = mppImportTestMinVersionForMaster)
    fun testMutableArtifactLists() {
        configureByFiles()
        importProject(true)
        checkProjectStructure(true, false, false) {
            module("KT38037") {}
            module("KT38037.mpp-bottom-actual") {}
            module("KT38037.mpp-bottom-actual.commonMain") {}
            module("KT38037.mpp-bottom-actual.commonTest") {}
            module("KT38037.mpp-bottom-actual.jvm18Main") {}
            module("KT38037.mpp-bottom-actual.jvm18Test") {}
            module("KT38037.mpp-bottom-actual.main") {}
            module("KT38037.mpp-bottom-actual.test") {}
            module("KT38037.mpp-mid-actual") {}
            module("KT38037.mpp-mid-actual.commonMain") {}
            module("KT38037.mpp-mid-actual.commonTest") {}
            module("KT38037.mpp-mid-actual.jvmWithJavaMain") {}
            module("KT38037.mpp-mid-actual.jvmWithJavaTest") {}
            module("KT38037.mpp-mid-actual.main") {}
            module("KT38037.mpp-mid-actual.test") {}
        }
    }

    private fun checkProjectStructure(
        exhaustiveModuleList: Boolean = true,
        exhaustiveSourceSourceRootList: Boolean = true,
        exhaustiveDependencyList: Boolean = true,
        exhaustiveTestsList: Boolean = false,
        body: ProjectInfo.() -> Unit = {}
    ) {
        checkProjectStructure(
            myProject,
            projectPath,
            exhaustiveModuleList,
            exhaustiveSourceSourceRootList,
            exhaustiveDependencyList,
            exhaustiveTestsList,
            body)
    }

    private fun findTasksToRun(file: VirtualFile): List<String>? {
        return GradleTestRunConfigurationProducer.findAllTestsTaskToRun(file, myProject)
            .flatMap { it.tasks }
            .sorted()
    }

    override fun createImportSpec(): ImportSpec {
        return ImportSpecBuilder(super.createImportSpec())
            .createDirectoriesForEmptyContentRoots()
            .build()
    }

    fun importProject(useQualifiedNames: Boolean) {
        val isUseQualifiedModuleNames = currentExternalProjectSettings.isUseQualifiedModuleNames
        currentExternalProjectSettings.isUseQualifiedModuleNames = useQualifiedNames
        try {
            importProject()
        } finally {
            currentExternalProjectSettings.isUseQualifiedModuleNames = isUseQualifiedModuleNames
        }
    }

    override fun testDataDirName(): String {
        return "newMultiplatformImport"
    }
}
