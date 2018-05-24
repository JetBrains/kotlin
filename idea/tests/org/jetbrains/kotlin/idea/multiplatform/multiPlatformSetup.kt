/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.multiplatform

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

// allows to configure a test mpp project
// testRoot is supposed to contain several directories which contain module sources roots
// configuration is based on those directories names
fun AbstractMultiModuleTest.setupMppProjectFromDirStructure(testRoot: File) {
    assert(testRoot.isDirectory) { testRoot.absolutePath + " must be a directory" }
    val dirs = testRoot.listFiles().filter { it.isDirectory }
    val rootInfos = dirs.map { parseDirName(it) }
    val infosByModuleId = rootInfos.groupBy { it.moduleId }
    val modulesById = infosByModuleId.mapValues { (moduleId, infos) ->
        createModuleWithRoots(moduleId, infos)
    }

    infosByModuleId.entries.forEach { (id, rootInfos) ->
        val module = modulesById[id]!!
        rootInfos.flatMap { it.dependencies }.forEach {
            when (it) {
                is ModuleDependency -> module.addDependency(modulesById[it.moduleId]!!)
                is StdlibDependency -> when (id.platform) {
                    is TargetPlatformKind.Common -> module.addLibrary(
                        ForTestCompileRuntime.stdlibCommonForTests(), kind = CommonLibraryKind
                    )
                    is TargetPlatformKind.Jvm -> module.addLibrary(ForTestCompileRuntime.runtimeJarForTests())
                    is TargetPlatformKind.JavaScript -> module.addLibrary(ForTestCompileRuntime.stdlibJsForTests(), kind = JSLibraryKind)
                }
                is FullJdkDependency -> {
                    ConfigLibraryUtil.configureSdk(module, PluginTestCaseBase.addJdk(testRootDisposable) {
                        PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK)
                    })
                }
                is CoroutinesDependency -> module.enableCoroutines()
            }
        }
    }

    modulesById.forEach { (nameAndPlatform, module) ->
        val (name, platform) = nameAndPlatform
        when (platform) {
            TargetPlatformKind.Common -> module.createFacet(TargetPlatformKind.Common, useProjectSettings = false)
            else -> {
                val commonModuleId = ModuleId(name, TargetPlatformKind.Common)

                module.createFacet(platform, implementedModuleName = commonModuleId.ideaModuleName())
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

        if (moduleId.platform is TargetPlatformKind.JavaScript && isTestRoot) {
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
    val moduleDir = PlatformTestCase.createTempDir("")
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
    listOf("header", "common", "expect") to TargetPlatformKind.Common,
    listOf("java", "jvm") to TargetPlatformKind.Jvm[JvmTarget.DEFAULT],
    listOf("java8", "jvm8") to TargetPlatformKind.Jvm[JvmTarget.JVM_1_8],
    listOf("java6", "jvm6") to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6],
    listOf("js", "javascript") to TargetPlatformKind.JavaScript
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
        else -> ModuleDependency(parseModuleId(dependencyString.split("-")))
    }
}

private fun parseModuleId(parts: List<String>): ModuleId {
    val platform = parsePlatform(parts)
    val name = parseModuleName(parts)
    return ModuleId(name, platform)
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

private data class ModuleId(
    val groupName: String,
    val platform: TargetPlatformKind<*>
) {
    fun ideaModuleName() = "${groupName}_${platform.presentableName}"
}

private val TargetPlatformKind<*>.presentableName: String
    get() = when (this) {
        is TargetPlatformKind.Common -> "Common"
        is TargetPlatformKind.Jvm -> "JVM"
        is TargetPlatformKind.JavaScript -> "JS"
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