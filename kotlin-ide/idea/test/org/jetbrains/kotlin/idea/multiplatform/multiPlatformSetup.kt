/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.multiplatform

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.utils.clearFileFromDiagnosticMarkup
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.stubs.createMultiplatformFacetM1
import org.jetbrains.kotlin.idea.stubs.createMultiplatformFacetM3
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.projectModel.*
import org.jetbrains.kotlin.types.typeUtil.closure
import java.io.File

// allows to configure a test mpp project
// testRoot is supposed to contain several directories which contain module sources roots
// configuration is based on those directories names
fun AbstractMultiModuleTest.setupMppProjectFromDirStructure(testRoot: File) {
    assert(testRoot.isDirectory) { testRoot.absolutePath + " must be a directory" }
    val dependencies = dependenciesFile(testRoot)
    if (dependencies.exists()) {
        setupMppProjectFromDependenciesFile(dependencies, testRoot)
        return
    }

    val dirs = testRoot.listFiles().filter { it.isDirectory }
    val rootInfos = dirs.map { parseDirName(it) }
    doSetupProject(rootInfos)
}

fun AbstractMultiModuleTest.setupMppProjectFromTextFile(testRoot: File) {
    assert(testRoot.isDirectory) { testRoot.absolutePath + " must be a directory" }
    val dependencies = dependenciesFile(testRoot)
    setupMppProjectFromDependenciesFile(dependencies, testRoot)
}

private fun dependenciesFile(testRoot: File) = File(testRoot, "dependencies.txt")

fun AbstractMultiModuleTest.setupMppProjectFromDependenciesFile(dependencies: File, testRoot: File) {
    val projectModel = ProjectStructureParser(testRoot).parse(FileUtil.loadFile(dependencies))

    check(projectModel.modules.isNotEmpty()) { "No modules were parsed from dependencies.txt" }

    doSetup(projectModel)
}

fun AbstractMultiModuleTest.doSetup(projectModel: ProjectResolveModel) {
    val resolveModulesToIdeaModules = projectModel.modules.map { resolveModule ->
        val ideaModule = createModule(resolveModule.name)

        addRoot(
            ideaModule,
            resolveModule.root,
            isTestRoot = false,
            transformContainedFiles = { if (it.extension == "kt") clearFileFromDiagnosticMarkup(it) }
        )

        if (resolveModule.testRoot != null) {
            addRoot(
                ideaModule,
                resolveModule.testRoot,
                isTestRoot = true,
                transformContainedFiles = { if (it.extension == "kt") clearFileFromDiagnosticMarkup(it) }
            )
        }

        resolveModule to ideaModule
    }.toMap()

    for ((resolveModule, ideaModule) in resolveModulesToIdeaModules.entries) {
        resolveModule.dependencies.closure(preserveOrder = true) { it.to.dependencies }.forEach {
            when (val to = it.to) {
                FullJdk -> ConfigLibraryUtil.configureSdk(ideaModule, PluginTestCaseBase.addJdk(testRootDisposable) {
                    PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK)
                })

                is ResolveLibrary -> ideaModule.addLibrary(to.root, to.name, to.kind)

                else -> ideaModule.addDependency(resolveModulesToIdeaModules[to]!!)
            }
        }
    }

    for ((resolveModule, ideaModule) in resolveModulesToIdeaModules.entries) {
        val platform = resolveModule.platform
        ideaModule.createMultiplatformFacetM3(
            platform,
            dependsOnModuleNames = resolveModule.dependencies.filter { it.kind == ResolveDependency.Kind.DEPENDS_ON }.map { it.to.name }
        )
        // New inference is enabled here as these tests are using type refinement feature that is working only along with NI
        ideaModule.enableMultiPlatform(additionalCompilerArguments = "-Xnew-inference")
    }
}

private fun AbstractMultiModuleTest.doSetupProject(rootInfos: List<RootInfo>) {
    val infosByModuleId = rootInfos.groupBy { it.moduleId }
    val modulesById = infosByModuleId.mapValues { (moduleId, infos) ->
        createModuleWithRoots(moduleId, infos)
    }

    infosByModuleId.entries.forEach { (id, rootInfos) ->
        val module = modulesById[id]!!
        rootInfos.flatMap { it.dependencies }.forEach {
            val platform = id.platform
            when (it) {
                is ModuleDependency -> module.addDependency(modulesById[it.moduleId]!!)
                is StdlibDependency -> {
                    when {
                        platform.isCommon() -> module.addLibrary(
                            ForTestCompileRuntime.stdlibCommonForTests(), kind = CommonLibraryKind
                        )
                        platform.isJvm() -> module.addLibrary(ForTestCompileRuntime.runtimeJarForTests())
                        platform.isJs() -> module.addLibrary(ForTestCompileRuntime.stdlibJsForTests(), kind = JSLibraryKind)
                        else -> error("Unknown platform $this")
                    }
                }
                is FullJdkDependency -> {
                    ConfigLibraryUtil.configureSdk(module, PluginTestCaseBase.addJdk(testRootDisposable) {
                        PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK)
                    })
                }
                is CoroutinesDependency -> module.enableCoroutines()
                is KotlinTestDependency -> when {
                    platform.isJvm() -> module.addLibrary(ForTestCompileRuntime.kotlinTestJUnitJarForTests())
                    platform.isJs() -> module.addLibrary(ForTestCompileRuntime.kotlinTestJsJarForTests(), kind = JSLibraryKind)
                }
            }
        }
    }

    modulesById.forEach { (nameAndPlatform, module) ->
        val (name, platform) = nameAndPlatform
        when {
            platform.isCommon() -> {
                module.createMultiplatformFacetM1(platform, useProjectSettings = false, implementedModuleNames = emptyList())
            }

            else -> {
                val commonModuleId = ModuleId(name, CommonPlatforms.defaultCommonPlatform)

                module.createMultiplatformFacetM1(platform, implementedModuleNames = listOf(commonModuleId.ideaModuleName()))
                module.enableMultiPlatform()

                modulesById[commonModuleId]?.let { commonModule ->
                    module.addDependency(commonModule)
                }
            }
        }
    }
}

private fun AbstractMultiModuleTest.createModuleWithRoots(
    moduleId: ModuleId,
    infos: List<RootInfo>
): Module {
    val module = createModule(moduleId.ideaModuleName())
    for ((_, isTestRoot, moduleRoot) in infos) {
        addRoot(module, moduleRoot, isTestRoot)

        if (moduleId.platform.isJs() && isTestRoot) {
            setupJsTestOutput(module)
        }
    }
    return module
}

// test line markers for JS do not work without additional setup
private fun setupJsTestOutput(module: Module) {
    ModuleRootModificationUtil.updateModel(module) {
        with(it.getModuleExtension(CompilerModuleExtension::class.java)!!) {
            inheritCompilerOutputPath(false)
            setCompilerOutputPathForTests("js_out")
        }
    }
}

private fun AbstractMultiModuleTest.createModule(name: String): Module {
    val moduleDir = createTempDir("")
    val module = createModule(moduleDir.toString() + "/" + name, StdModuleTypes.JAVA)
    val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir)
    TestCase.assertNotNull(root)
    object : WriteCommandAction.Simple<Unit>(module.project) {
        @Throws(Throwable::class)
        override fun run() {
            root!!.refresh(false, true)
        }
    }.execute().throwException()
    return module
}

private val testSuffixes = setOf("test", "tests")
private val platformNames = mapOf(
    listOf("header", "common", "expect") to CommonPlatforms.defaultCommonPlatform,
    listOf("java", "jvm") to JvmPlatforms.defaultJvmPlatform,
    listOf("java8", "jvm8") to JvmPlatforms.jvm18,
    listOf("java6", "jvm6") to JvmPlatforms.jvm16,
    listOf("js", "javascript") to JsPlatforms.defaultJsPlatform
)

private fun parseDirName(dir: File): RootInfo {
    val parts = dir.name.split("_")
    return RootInfo(parseModuleId(parts), parseIsTestRoot(parts), dir, parseDependencies(parts))
}

private fun parseDependencies(parts: List<String>) =
    parts.filter { it.startsWith("dep(") && it.endsWith(")") }.map {
        parseDependency(it)
    }

private fun parseDependency(it: String): Dependency {
    val dependencyString = it.removePrefix("dep(").removeSuffix(")")

    return when {
        dependencyString.equals("stdlib", ignoreCase = true) -> StdlibDependency
        dependencyString.equals("fulljdk", ignoreCase = true) -> FullJdkDependency
        dependencyString.equals("coroutines", ignoreCase = true) -> CoroutinesDependency
        dependencyString.equals("kotlin-test", ignoreCase = true) -> KotlinTestDependency
        else -> ModuleDependency(parseModuleId(dependencyString.split("-")))
    }
}

private fun parseModuleId(parts: List<String>): ModuleId {
    val platform = parsePlatform(parts)
    val name = parseModuleName(parts)
    val id = parseIndex(parts) ?: 0
    assert(id == 0 || !platform.isCommon())
    return ModuleId(name, platform, id)
}

private fun parsePlatform(parts: List<String>) =
    platformNames.entries.single { (names, _) ->
        names.any { name -> parts.any { part -> part.equals(name, ignoreCase = true) } }
    }.value

private fun parseModuleName(parts: List<String>) = when {
    parts.size > 1 -> parts.first()
    else -> "testModule"
}

private fun parseIsTestRoot(parts: List<String>) =
    testSuffixes.any { suffix -> parts.any { it.equals(suffix, ignoreCase = true) } }

private fun parseIndex(parts: List<String>): Int? {
    return parts.singleOrNull() { it.startsWith("id") }?.substringAfter("id")?.toInt()
}

private data class ModuleId(
    val groupName: String,
    val platform: TargetPlatform,
    val index: Int = 0
) {
    fun ideaModuleName(): String {
        val suffix = "_$index".takeIf { index != 0 } ?: ""
        return "${groupName}_${platform.presentableName}$suffix"
    }
}

private val TargetPlatform.presentableName: String
    get() = when {
        isCommon() -> "Common"
        isJvm() -> "JVM"
        isJs() -> "JS"
        else -> error("Unknown platform $this")
    }

private data class RootInfo(
    val moduleId: ModuleId,
    val isTestRoot: Boolean,
    val moduleRoot: File,
    val dependencies: List<Dependency>
)

private sealed class Dependency
private class ModuleDependency(val moduleId: ModuleId) : Dependency()
private object StdlibDependency : Dependency()
private object FullJdkDependency : Dependency()
private object CoroutinesDependency : Dependency()
private object KotlinTestDependency : Dependency()