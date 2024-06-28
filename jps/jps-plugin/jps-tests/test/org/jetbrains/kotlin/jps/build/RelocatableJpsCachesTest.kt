/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.testFramework.RunAll
import com.intellij.util.ThrowableRunnable
import org.jetbrains.jps.builders.*
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.cmdline.ProjectDescriptor
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.incremental.KOTLIN_CACHE_DIRECTORY_NAME
import org.jetbrains.kotlin.incremental.testingUtils.assertEqualDirectories
import org.jetbrains.kotlin.jps.build.fixtures.EnableICFixture
import org.jetbrains.kotlin.jps.incremental.KotlinDataContainerTarget
import org.jetbrains.kotlin.jps.model.JpsKotlinFacetModuleExtension
import org.jetbrains.kotlin.test.MockLibraryUtilExt
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.reflect.KFunction1

class RelocatableJpsCachesTest : BaseKotlinJpsBuildTestCase() {
    private val enableICFixture = EnableICFixture()
    private lateinit var workingDir: File

    @OptIn(ExperimentalPathApi::class)
    override fun setUp() {
        super.setUp()
        enableICFixture.setUp()
        workingDir = createTempDirectory("RelocatableJpsCachesTest-" + getTestName(false)).toFile()
    }

    override fun tearDown() {
        RunAll(
            ThrowableRunnable { workingDir.deleteRecursively() },
            ThrowableRunnable { enableICFixture.tearDown() },
            ThrowableRunnable { super.tearDown() }
        ).run()
    }

    fun testRelocatableCaches() {
        buildTwiceAndCompare(RelocatableCacheTestCase::testRelocatableCaches)
    }


    fun testRelocatablePluginClasspath() {
        buildTwiceAndCompare(RelocatableCacheTestCase::testRelocatablePluginClasspath)
    }

    private fun buildTwiceAndCompare(testMethod: KFunction1<RelocatableCacheTestCase, Unit>) {
        val test1WorkingDir = workingDir.resolve("test1")
        val test1KotlinCachesDir = workingDir.resolve("test1KotlinCaches")
        val test2WorkingDir = workingDir.resolve("test2")
        val test2KotlinCachesDir = workingDir.resolve("test2KotlinCaches")

        runTestAndCopyKotlinCaches(test1WorkingDir, test1KotlinCachesDir, testMethod)
        runTestAndCopyKotlinCaches(test2WorkingDir, test2KotlinCachesDir, testMethod)

        assertEqualDirectories(test1KotlinCachesDir, test2KotlinCachesDir, forgiveExtraFiles = false)
    }

    private fun runTestAndCopyKotlinCaches(
        projectWorkingDir: File,
        dirToCopyKotlinCaches: File,
        testMethod: KFunction1<RelocatableCacheTestCase, Unit>
    ) {
        val testCase = object : RelocatableCacheTestCase(projectWorkingDir, dirToCopyKotlinCaches) {
            override fun getName() = testMethod.name
        }

        testCase.exposedPrivateApi.setUp()

        try {
            testMethod.call(testCase)
        } finally {
            testCase.exposedPrivateApi.tearDown()
        }
    }
}

// the class should not be executed directly (hence it's abstract)
abstract class RelocatableCacheTestCase(
    private val projectWorkingDir: File,
    private val dirToCopyKotlinCaches: File
) : KotlinJpsBuildTestBase() {
    val exposedPrivateApi = ExposedPrivateApi()

    fun testRelocatableCaches() {
        initProject(LibraryDependency.JVM_FULL_RUNTIME)
        buildAllModules().assertSuccessful()

        assertFilesExistInOutput(
            myProject.modules.single(),
            "MainKt.class", "Foo.class", "FooChild.class", "utils/Utils.class"
        )
    }

    @WorkingDir("KotlinProject")
    fun testRelocatablePluginClasspath() {
        initProject(LibraryDependency.JVM_FULL_RUNTIME)

        //create lib
        val libraryName = "module1-1.0-SNAPSHOT"
        val libraryJar = MockLibraryUtilExt.compileJvmLibraryToJar(workDir.resolve("non-existent-folder").absolutePath, libraryName)
        val module1Lib = this.workDir.resolve("lib").resolve("$libraryName.jar")
        Files.createDirectories(module1Lib.parentFile.toPath())
        Files.copy(libraryJar.toPath(), module1Lib.toPath(), StandardCopyOption.REPLACE_EXISTING)
        assert(module1Lib.exists())

        // Add facet
        myProject.modules.forEach {
            val facet = KotlinFacetSettings()
            facet.useProjectSettings = false
            facet.compilerArguments = K2JVMCompilerArguments().apply {
                // Add both libraries inside and outside project
                pluginClasspaths = arrayOf(module1Lib.absolutePath, libraryJar.absolutePath)
            }

            it.container.setChild(
                JpsKotlinFacetModuleExtension.KIND,
                JpsKotlinFacetModuleExtension(facet)
            )
        }
        buildAllModules().assertSuccessful()

        assertFilesExistInOutput(
            myProject.modules.single(),
            "Test1Kt.class"
        )
    }

    override fun copyTestDataToTmpDir(testDataDir: File): File {
        testDataDir.copyRecursively(projectWorkingDir)
        return projectWorkingDir
    }

    override fun doBuild(descriptor: ProjectDescriptor, scopeBuilder: CompileScopeTestBuilder?): BuildResult =
        super.doBuild(descriptor, scopeBuilder).also {
            copyKotlinCaches(descriptor)
        }

    private fun copyKotlinCaches(descriptor: ProjectDescriptor) {
        val kotlinDataPaths = HashSet<File>()
        val dataPaths = descriptor.dataManager.dataPaths
        kotlinDataPaths.add(dataPaths.getTargetDataRoot(KotlinDataContainerTarget))

        for (target in descriptor.buildTargetIndex.allTargets) {
            if (!target.isKotlinTarget(descriptor)) continue

            val targetDataRoot = descriptor.dataManager.dataPaths.getTargetDataRoot(target)
            val kotlinDataRoot = targetDataRoot.resolve(KOTLIN_CACHE_DIRECTORY_NAME)
            assert(kotlinDataRoot.isDirectory) { "Kotlin data root '$kotlinDataRoot' is not a directory" }
            kotlinDataPaths.add(kotlinDataRoot)
        }

        findFileInDirectory(descriptor.dataManager.dataPaths.dataStorageRoot, "jvm-build-meta-info.txt")!!.also { kotlinDataPaths.add(it) }

        dirToCopyKotlinCaches.deleteRecursively()
        val originalStorageRoot = descriptor.dataManager.dataPaths.dataStorageRoot
        for (kotlinCacheRoot in kotlinDataPaths) {
            val relativePath = kotlinCacheRoot.relativeTo(originalStorageRoot).path
            val targetDir = dirToCopyKotlinCaches.resolve(relativePath)
            targetDir.parentFile.mkdirs()
            kotlinCacheRoot.copyRecursively(targetDir)
        }
    }

    private fun findFileInDirectory(directory: File, fileNameToFind: String): File? {
        val foundFiles = directory.listFiles { file ->
            file.isFile && file.name == fileNameToFind
        } ?: return null

        if (foundFiles.isNotEmpty()) {
            return foundFiles.firstOrNull()
        }

        val subdirectories = directory.listFiles { file -> file.isDirectory } ?: return null

        for (subdirectory in subdirectories) {
            val foundFile = findFileInDirectory(subdirectory, fileNameToFind)
            if (foundFile != null) {
                return foundFile
            }
        }

        return null
    }

    private fun BuildTarget<*>.isKotlinTarget(descriptor: ProjectDescriptor): Boolean {
        fun JavaSourceRootDescriptor.containsKotlinSources() = root.walk().any { it.isKotlinSourceFile }

        if (this !is ModuleBuildTarget) return false

        val rootDescriptors = computeRootDescriptors(
            descriptor.model,
            descriptor.moduleExcludeIndex,
            descriptor.ignoredFileIndex,
            descriptor.dataManager.dataPaths
        )

        return rootDescriptors.any { it is JavaSourceRootDescriptor && it.containsKotlinSources() }
    }

    // the famous Public Morozov pattern
    inner class ExposedPrivateApi {
        fun setUp() {
            this@RelocatableCacheTestCase.setUp()
        }

        fun tearDown() {
            this@RelocatableCacheTestCase.tearDown()
        }
    }
}