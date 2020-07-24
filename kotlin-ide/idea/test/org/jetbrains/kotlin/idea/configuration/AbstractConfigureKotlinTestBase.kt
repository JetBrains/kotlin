package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.testFramework.HeavyPlatformTestCase
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
import org.jetbrains.kotlin.test.KotlinRoot
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.disposeVfsRootAccess
import java.io.File
import java.nio.file.Path

abstract class AbstractConfigureKotlinTestBase : HeavyPlatformTestCase() {
    protected lateinit var projectRoot: File
        private set

    private lateinit var vfsDisposable: Ref<Disposable>

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

    private val projectName: String
        get() = getTestName(true).substringBefore("_")

    override fun setUp() {
        projectRoot = KotlinTestUtils.tmpDirForReusableFolder("configure")
        vfsDisposable = KotlinTestUtils.allowRootAccess(this, projectRoot.path)
        super.setUp()
    }

    override fun tearDown() {
        disposeVfsRootAccess(vfsDisposable)
        super.tearDown()
    }

    override fun initApplication() {
        super.initApplication()

        KotlinSdkType.setUpIfNeeded(testRootDisposable)

        ApplicationManager.getApplication().runWriteAction {
            addJdk(testRootDisposable, ::mockJdk6)
            addJdk(testRootDisposable, ::mockJdk8)
            addJdk(testRootDisposable, ::mockJdk9)
        }
    }

    override fun getProjectDirOrFile(isDirectoryBasedProject: Boolean): Path {
        val originalDir = KotlinRoot.DIR.resolve("idea/testData/configuration").resolve(projectName)
        originalDir.copyRecursively(projectRoot)

        val projectFile = projectRoot.resolve("projectFile.ipr")
        return (if (projectFile.exists()) projectFile else projectRoot).toPath()
    }

    override fun setUpModule() {
        val modules = ModuleManager.getInstance(project).modules
        myModule = modules.singleOrNull() ?: error("Single module expected, got $modules")
    }

    protected fun getOppositeConfigurator(configurator: KotlinWithLibraryConfigurator): KotlinWithLibraryConfigurator {
        if (configurator === jvmConfigurator) return jsConfigurator
        if (configurator === jsConfigurator) return jvmConfigurator

        throw IllegalArgumentException("Only JS_CONFIGURATOR and JAVA_CONFIGURATOR are supported")
    }
}