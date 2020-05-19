/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard

import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.SimpleJavaSdkType
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PlatformTestCase
import org.jetbrains.kotlin.idea.test.KotlinSdkCreationChecker
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.tools.projectWizard.cli.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.Services
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaServices
import org.jetbrains.kotlin.tools.projectWizard.wizard.service.IdeaWizardService
import org.jetbrains.kotlin.tools.projectWizard.wizard.services.TestWizardServices
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleEnvironment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

//TODO change to HeavyPlatformTestCase when we stop supporting <= 192
abstract class AbstractNewWizardProjectImportTest : PlatformTestCase() {
    abstract fun createWizard(directory: Path, buildSystem: BuildSystem, projectDirectory: Path): Wizard

    lateinit var sdkCreationChecker: KotlinSdkCreationChecker

    override fun setUp() {
        super.setUp()
        runWriteAction {
            val sdk = SimpleJavaSdkType().createJdk(SDK_NAME, IdeaTestUtil.requireRealJdkHome())
            PluginTestCaseBase.addJdk(testRootDisposable, { sdk })
        }
        sdkCreationChecker = KotlinSdkCreationChecker()
    }

    override fun tearDown() {
        sdkCreationChecker.removeNewKotlinSdk()
        super.tearDown()
        runWriteAction {
            ProjectJdkTable.getInstance().findJdk(SDK_NAME)?.let(ProjectJdkTable.getInstance()::removeJdk)
        }
    }

    fun doTestGradleKts(directoryPath: String) {
        doTest(directoryPath, BuildSystem.GRADLE_KOTLIN_DSL)
    }

    fun doTestGradleGroovy(directoryPath: String) {
        doTest(directoryPath, BuildSystem.GRADLE_GROOVY_DSL)
    }

    fun doTestMaven(directoryPath: String) {
        doTest(directoryPath, BuildSystem.MAVEN)
    }

    private fun doTest(directoryPath: String, buildSystem: BuildSystem) {
        val directory = Paths.get(directoryPath)

        val parameters = DefaultTestParameters.fromTestDataOrDefault(directory)
        if (!parameters.runForMaven && buildSystem == BuildSystem.MAVEN) return

        val tempDirectory = Files.createTempDirectory(null)
        if (buildSystem.isGradle) {
            prepareGradleBuildSystem(tempDirectory)
        }

        val wizard = createWizard(directory, buildSystem, tempDirectory)

        val projectDependentServices =
            IdeaServices.createScopeDependent(project) +
                    TestWizardServices.createProjectDependent(project) +
                    TestWizardServices.PROJECT_INDEPENDENT
        wizard.apply(projectDependentServices, GenerationPhase.ALL).assertSuccess()
    }

    private fun prepareGradleBuildSystem(directory: Path) {
        com.intellij.openapi.components.ServiceManager.getService(project, GradleSettings::class.java)?.apply {
            isOfflineWork = GradleEnvironment.Headless.GRADLE_OFFLINE?.toBoolean() ?: isOfflineWork
            serviceDirectoryPath = GradleEnvironment.Headless.GRADLE_SERVICE_DIRECTORY ?: serviceDirectoryPath
        }

        // not needed on 192 (and causes error on 192 ):
        if (!is192()) {
            val settings = GradleProjectSettings().apply {
                externalProjectPath = directory.toString()
                isUseAutoImport = false
                isUseQualifiedModuleNames = true
                gradleJvm = SDK_NAME
                distributionType = DistributionType.WRAPPED
            }
            ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).linkProject(settings)
        }
    }

    private fun is192() =
        ApplicationInfoImpl.getShadowInstance().minorVersionMainPart == "2"
                && ApplicationInfoImpl.getShadowInstance().majorVersion == "2019"

    companion object {
        private const val SDK_NAME = "defaultSdk"

        val IDE_WIZARD_TEST_SERVICES_MANAGER = ServicesManager(
            IdeaServices.PROJECT_INDEPENDENT + Services.IDEA_INDEPENDENT_SERVICES
        ) { services ->
            services.firstIsInstanceOrNull<TestWizardService>()
                ?: services.firstIsInstanceOrNull<IdeaWizardService>()
                ?: services.firstOrNull()
        }
    }
}
