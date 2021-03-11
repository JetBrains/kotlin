package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.IdeaTestUtil
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.addJdk
import org.jetbrains.kotlin.test.KotlinRoot
import java.io.File
import java.nio.file.Path

abstract class AbstractConfigureKotlinTestBase : HeavyPlatformTestCase() {
    protected lateinit var projectRoot: File

    protected val jvmConfigurator: KotlinJavaModuleConfigurator by lazy {
        object : KotlinJavaModuleConfigurator() {
            override fun getDefaultPathToJarFile(project: Project) = projectRoot.resolve("default_jvm_lib").path
        }
    }

    protected val jsConfigurator: KotlinJsModuleConfigurator by lazy {
        object : KotlinJsModuleConfigurator() {
            override fun getDefaultPathToJarFile(project: Project) = projectRoot.resolve("default_js_lib").path
        }
    }

    protected val modules: Array<Module>
        get() = ModuleManager.getInstance(myProject).modules

    protected val projectName: String
        get() = getTestName(true).substringBefore("_")

    override fun setUp() {
        projectRoot = createProjectRoot()
        super.setUp()
    }

    open fun createProjectRoot(): File = KotlinRoot.DIR.resolve("idea/testData/configuration").resolve(projectName)

    override fun initApplication() {
        super.initApplication()

        KotlinSdkType.setUpIfNeeded(testRootDisposable)

        ApplicationManager.getApplication().runWriteAction {
            addJdk(testRootDisposable, IdeaTestUtil::getMockJdk16)
            addJdk(testRootDisposable, IdeaTestUtil::getMockJdk18)
            addJdk(testRootDisposable, IdeaTestUtil::getMockJdk9)
        }
    }

    override fun getProjectDirOrFile(isDirectoryBasedProject: Boolean): Path {
        val projectFile = projectRoot.resolve("projectFile.ipr")
        return (if (projectFile.exists()) projectFile else projectRoot).toPath()
    }

    override fun setUpModule() {
        val modules = ModuleManager.getInstance(project).modules
        myModule = modules.first()
    }

    protected fun getOppositeConfigurator(configurator: KotlinWithLibraryConfigurator): KotlinWithLibraryConfigurator {
        if (configurator === jvmConfigurator) return jsConfigurator
        if (configurator === jsConfigurator) return jvmConfigurator

        throw IllegalArgumentException("Only JS_CONFIGURATOR and JAVA_CONFIGURATOR are supported")
    }
}