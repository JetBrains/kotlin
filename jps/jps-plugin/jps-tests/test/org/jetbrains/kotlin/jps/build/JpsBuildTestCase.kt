/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.TimeoutUtil
import org.jetbrains.jps.api.CanceledStatus
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import org.jetbrains.jps.builders.impl.BuildRootIndexImpl
import org.jetbrains.jps.builders.impl.BuildTargetIndexImpl
import org.jetbrains.jps.builders.impl.BuildTargetRegistryImpl
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.jps.cmdline.ClasspathBootstrap
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.BuilderRegistry
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.IncProjectBuilder
import org.jetbrains.jps.incremental.RebuildRequestedException
import org.jetbrains.jps.incremental.fs.BuildFSState
import org.jetbrains.jps.incremental.relativizer.PathRelativizerService
import org.jetbrains.jps.incremental.storage.BuildDataManager
import org.jetbrains.jps.incremental.storage.BuildTargetsState
import org.jetbrains.jps.incremental.storage.ProjectStamps
import org.jetbrains.jps.indices.ModuleExcludeIndex
import org.jetbrains.jps.indices.impl.IgnoredFileIndexImpl
import org.jetbrains.jps.indices.impl.ModuleExcludeIndexImpl
import org.jetbrains.jps.model.*
import org.jetbrains.jps.model.java.*
import org.jetbrains.jps.model.java.compiler.JavaCompilers
import org.jetbrains.jps.model.java.impl.JavaModuleIndexImpl
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.serialization.JpsProjectLoader
import org.jetbrains.jps.model.serialization.PathMacroUtil
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.test.testFramework.disposeRootDisposable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException
import java.nio.file.Paths

@Suppress("UnstableApiUsage")
abstract class JpsBuildTestCase {
    private var myProjectDir: File? = null
    protected lateinit var myProject: JpsProject
    protected lateinit var myModel: JpsModel
    private var myJdk: JpsSdk<JpsDummyElement>? = null
    protected lateinit var myDataStorageRoot: File
    private lateinit var myLogger: TestProjectBuilderLogger

    protected lateinit var testInfo: TestInfo
    protected val testRootDisposable: Disposable = Disposer.newDisposable()

    protected lateinit var myBuildParams: MutableMap<String, String>

    @BeforeEach
    open fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
        myModel = JpsElementFactory.getInstance().createModel()
        myProject = myModel.project
        myDataStorageRoot = FileUtil.createTempDirectory("compile-server-" + this.projectName, null)
        myLogger = TestProjectBuilderLogger()
        myBuildParams = HashMap()
    }

    @AfterEach
    open fun tearDown() {
        disposeRootDisposable(testRootDisposable)
    }

    protected fun addJdk(name: String): JpsSdk<JpsDummyElement> {
        try {
            val pathToRtJar = ClasspathBootstrap.getResourcePath(Any::class.java)
            val path = if (pathToRtJar == null) null else FileUtil.toSystemIndependentName(File(pathToRtJar).getCanonicalPath())
            return addJdk(name, path)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected open fun addJdk(name: String, jdkClassesRoot: String?): JpsSdk<JpsDummyElement> {
        val homePath = System.getProperty("java.home")
        val versionString = System.getProperty("java.version")
        val jdk = myModel.global.addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE)
        if (jdkClassesRoot != null) {
            jdk.addRoot(JpsPathUtil.pathToUrl(jdkClassesRoot), JpsOrderRootType.COMPILED)
        }
        return jdk.getProperties()
    }

    private val projectName: String
        get() = StringUtil.decapitalize(
            StringUtil.trimStart(
                testInfo.testMethod.get().name,
                "test"
            )
        )

    @Suppress("DEPRECATION")
    protected fun createProjectDescriptor(buildLoggingManager: BuildLoggingManager?): ProjectDescriptor {
        try {
            val targetRegistry = BuildTargetRegistryImpl(myModel)
            val index: ModuleExcludeIndex = ModuleExcludeIndexImpl(myModel)
            val ignoredFileIndex = IgnoredFileIndexImpl(myModel)
            val dataPaths: BuildDataPaths = BuildDataPathsImpl(myDataStorageRoot)
            val buildRootIndex = BuildRootIndexImpl(targetRegistry, myModel, index, dataPaths, ignoredFileIndex)
            val targetIndex = BuildTargetIndexImpl(targetRegistry, buildRootIndex)
            val targetsState = BuildTargetsState(dataPaths, myModel, buildRootIndex)
            val relativizer = PathRelativizerService(myModel.project)
            val projectStamps = ProjectStamps(myDataStorageRoot, targetsState, relativizer)
            val dataManager = BuildDataManager(dataPaths, targetsState, relativizer)
            return ProjectDescriptor(
                myModel, BuildFSState(true), projectStamps, dataManager, buildLoggingManager, index,
                targetIndex, buildRootIndex, ignoredFileIndex
            )
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun loadProject(projectPath: String) {
        loadProject(projectPath, mutableMapOf())
    }

    private fun loadProject(
        projectPath: String,
        pathVariables: MutableMap<String?, String?>
    ) {
        try {
            val fullProjectPath = FileUtil.toSystemDependentName(projectPath)
            val allPathVariables: MutableMap<String?, String?> = HashMap(pathVariables.size + 1)
            allPathVariables.putAll(pathVariables)
            allPathVariables[PathMacroUtil.APPLICATION_HOME_DIR] = PathManager.getHomePath()
            JpsProjectLoader.loadProject(myProject, allPathVariables, Paths.get(fullProjectPath))
            val config = JpsJavaExtensionService.getInstance().getCompilerConfiguration(myProject)
            config.getCompilerOptions(JavaCompilers.JAVAC_ID).PREFER_TARGET_JDK_COMPILER = false
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    protected fun <T : JpsElement> addModule(
        moduleName: String,
        srcPaths: Array<out String>,
        outputPath: String?,
        sdk: JpsSdk<T>?
    ): JpsModule {
        val module = myProject.addModule<JpsDummyElement?, JpsJavaModuleType?>(moduleName, JpsJavaModuleType.INSTANCE)
        if (sdk != null) {
            setupModuleSdk(module, sdk)
        }
        if (srcPaths.isNotEmpty() || outputPath != null) {
            for (srcPath in srcPaths) {
                module.contentRootsList.addUrl(JpsPathUtil.pathToUrl(srcPath))
                module.addSourceRoot<JavaSourceRootProperties?>(JpsPathUtil.pathToUrl(srcPath), JavaSourceRootType.SOURCE)
            }
            val extension = JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module)
            if (outputPath != null) {
                extension.outputUrl = JpsPathUtil.pathToUrl(outputPath)
                extension.testOutputUrl = extension.outputUrl
            } else {
                extension.isInheritOutput = true
            }
        }
        return module
    }

    protected fun rebuildAllModules() {
        doBuild(CompileScopeTestBuilder.rebuild().allModules()).assertSuccessful()
    }

    /**
     * Invoked forced rebuild for all targets in the project. May lead to unpredictable results if some plugins add targets your test doesn't expect.
     * 
     */
    @Deprecated("use {@link #rebuildAllModules()} instead or directly add required target types to the scope via {@link CompileScopeTestBuilder#targetTypes}")
    protected fun rebuildAll() {
        @Suppress("DEPRECATION")
        doBuild(CompileScopeTestBuilder.rebuild().all()).assertSuccessful()
    }

    protected fun buildAllModules(): BuildResult {
        return doBuild(CompileScopeTestBuilder.make().allModules())
    }

    /**
     * Invoked incremental build for all targets in the project. May lead to unpredictable results if some plugins add targets your test doesn't expect.
     * 
     */
    @Deprecated("use {@link #buildAllModules()} instead or directly add required target types to the scope via {@link CompileScopeTestBuilder#targetTypes}")
    protected fun makeAll(): BuildResult {
        @Suppress("DEPRECATION")
        return doBuild(CompileScopeTestBuilder.make().all())
    }

    private fun doBuild(scope: CompileScopeTestBuilder): BuildResult {
        val descriptor = createProjectDescriptor(BuildLoggingManager(myLogger))
        try {
            myLogger.clearFilesData()
            return doBuild(descriptor, scope)
        } finally {
            descriptor.release()
        }
    }

    fun assertCompiled(builderName: String?, vararg paths: String?) {
        myLogger.assertCompiled(builderName, arrayOf(myProjectDir, myDataStorageRoot), *paths)
    }

    protected fun assertDeleted(vararg paths: String?) {
        myLogger.assertDeleted(arrayOf(myProjectDir, myDataStorageRoot), *paths)
    }

    protected open fun doBuild(descriptor: ProjectDescriptor, scopeBuilder: CompileScopeTestBuilder): BuildResult {
        val builder =
            IncProjectBuilder(descriptor, BuilderRegistry.getInstance(), myBuildParams, CanceledStatus.NULL, true)
        val result = BuildResult()
        builder.addMessageHandler(result)
        try {
            builder.build(scopeBuilder.build(), false)
            result.storeMappingsDump(descriptor)
        } catch (e: RebuildRequestedException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        } finally {
            // the following code models module index reload after each make session
            val moduleIndex = JpsJavaExtensionService.getInstance().getJavaModuleIndex(descriptor.project)
            if (moduleIndex is JavaModuleIndexImpl) {
                moduleIndex.dropCache()
            }
        }
        return result
    }

    fun createFile(relativePath: String, text: String): String {
        try {
            val file = File(this.orCreateProjectDir, relativePath)
            FileUtil.writeToFile(file, text)
            return FileUtil.toSystemIndependentName(file.absolutePath)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    val orCreateProjectDir: File?
        get() {
            if (myProjectDir == null) {
                try {
                    myProjectDir = doGetProjectDir()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
            return myProjectDir
        }

    @Throws(IOException::class)
    protected open fun doGetProjectDir(): File? {
        return FileUtil.createTempDirectory("prj", null)
    }

    fun getAbsolutePath(pathRelativeToProjectRoot: String): String {
        return FileUtil.toSystemIndependentName(File(this.orCreateProjectDir, pathRelativeToProjectRoot).absolutePath)
    }

    fun addModule(moduleName: String, vararg srcPaths: String): JpsModule {
        return addModule(
            moduleName,
            srcPaths = srcPaths,
            outputPath = getAbsolutePath(getModuleOutputRelativePath(moduleName)),
            sdk = this.jdk
        )
    }

    private val jdk: JpsSdk<JpsDummyElement>
        get() {
            if (myJdk == null) {
                myJdk = addJdk("1.6")
            }
            return myJdk!!
        }

    companion object {
        fun rename(path: String, newName: String) {
            try {
                val file = File(FileUtil.toSystemDependentName(path))
                assertTrue(file.exists(), "File ${file.absolutePath} doesn't exist")
                val tempFile = File(file.getParentFile(), "__$newName")
                FileUtil.rename(file, tempFile)
                val newFile = File(file.getParentFile(), newName)
                FileUtil.copyContent(tempFile, newFile)
                FileUtil.delete(tempFile)
                change(newFile.path)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        fun change(filePath: String, newContent: String? = null) {
            try {
                val file = File(FileUtil.toSystemDependentName(filePath))
                assertTrue(file.exists(), "File ${file.absolutePath} doesn't exist")
                if (newContent != null) {
                    FileUtil.writeToFile(file, newContent)
                }
                val oldTimestamp = FSOperations.lastModified(file)
                val time = System.currentTimeMillis()
                setLastModified(file, time)
                if (FSOperations.lastModified(file) <= oldTimestamp) {
                    setLastModified(file, time + 1)
                    var newTimeStamp = FSOperations.lastModified(file)
                    if (newTimeStamp <= oldTimestamp) {
                        // macOS and some versions of Linux truncates timestamp to nearest second
                        setLastModified(file, time + 1000)
                        newTimeStamp = FSOperations.lastModified(file)
                        assertTrue(newTimeStamp > oldTimestamp, "Failed to change timestamp for ${file.absolutePath}")
                    }
                    sleepUntil(newTimeStamp)
                }
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        private fun sleepUntil(time: Long) {
            //we need this to ensure that the file won't be treated as changed by user during compilation and therefore marked for recompilation
            var delta: Long
            while (((time - System.currentTimeMillis()).also { delta = it }) > 0) {
                TimeoutUtil.sleep(delta)
            }
        }

        private fun setLastModified(file: File, time: Long) {
            val updated = file.setLastModified(time)
            assertTrue(updated, "Cannot modify timestamp for ${file.absolutePath}")
        }

        protected fun delete(filePath: String) {
            val file = File(FileUtil.toSystemDependentName(filePath))
            assertTrue(file.exists(), "File ${file.absolutePath} doesn't exist")
            val deleted = FileUtil.delete(file)
            assertTrue(deleted, "Cannot delete file ${file.absolutePath}")
        }

        private fun <T : JpsElement?> setupModuleSdk(module: JpsModule, sdk: JpsSdk<T>) {
            val sdkType = sdk.getSdkType()
            val sdkTable = module.sdkReferencesTable
            sdkTable.setSdkReference(sdkType, sdk.createReference())

            if (sdkType is JpsJavaSdkTypeWrapper) {
                val wrapperRef = sdk.createReference()
                sdkTable.setSdkReference<JpsDummyElement?>(
                    JpsJavaSdkType.INSTANCE, JpsJavaExtensionService.getInstance
                        ().createWrappedJavaSdkReference(sdkType as JpsJavaSdkTypeWrapper, wrapperRef)
                )
            }
            // ensure jdk entry is the first one in dependency list
            module.dependenciesList.clear()
            module.dependenciesList.addSdkDependency(sdkType)
            module.dependenciesList.addModuleSourceDependency()
        }

        private fun getModuleOutputRelativePath(moduleName: String): String {
            return "out/production/$moduleName"
        }
    }
}
