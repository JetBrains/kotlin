/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.config.ExternalSystemTestRunTask
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.externalSystemTestRunTasks
import org.jetbrains.kotlin.idea.project.isHMPPEnabled
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.utils.addToStdlib.filterIsInstanceWithChecker

class MessageCollector {
    private val builder = StringBuilder()

    fun report(message: String) {
        builder.append(message).append('\n')
    }

    fun check() {
        val message = builder.toString()
        if (message.isNotEmpty()) {
            assert(false) { message }
        }
    }
}

class ProjectInfo(
    project: Project,
    internal val projectPath: String,
    internal val exhaustiveModuleList: Boolean,
    internal val exhaustiveSourceSourceRootList: Boolean,
    internal val exhaustiveDependencyList: Boolean,
    internal val exhaustiveTestsList: Boolean
) {
    internal val messageCollector = MessageCollector()
    private val moduleManager = ModuleManager.getInstance(project)
    private val expectedModuleNames = HashSet<String>()
    private var allModulesAsserter: (ModuleInfo.() -> Unit)? = null

    fun allModules(body: ModuleInfo.() -> Unit) {
        assert(allModulesAsserter == null)
        allModulesAsserter = body
    }

    fun module(name: String, body: ModuleInfo.() -> Unit = {}) {
        val module = moduleManager.findModuleByName(name)
        if (module == null) {
            messageCollector.report("No module found: '$name' in ${moduleManager.modules.map { it.name }}")
            return
        }
        val moduleInfo = ModuleInfo(module, this)
        allModulesAsserter?.let { moduleInfo.it() }
        moduleInfo.run(body)
        expectedModuleNames += name
    }

    fun run(body: ProjectInfo.() -> Unit = {}) {
        body()

        if (exhaustiveModuleList) {
            val actualNames = moduleManager.modules.map { it.name }.sorted()
            val expectedNames = expectedModuleNames.sorted()
            if (actualNames != expectedNames) {
                messageCollector.report("Expected module list $expectedNames doesn't match the actual one: $actualNames")
            }
        }

        messageCollector.check()
    }
}

class ModuleInfo(val module: Module, private val projectInfo: ProjectInfo) {
    private val rootModel = module.rootManager
    private val expectedDependencyNames = HashSet<String>()
    private val expectedSourceRoots = HashSet<String>()
    private val expectedExternalSystemTestTasks = ArrayList<ExternalSystemTestRunTask>()

    private val sourceFolderByPath by lazy {
        rootModel.contentEntries.asSequence()
            .flatMap { it.sourceFolders.asSequence() }
            .mapNotNull {
                val path = it.file?.path ?: return@mapNotNull null
                FileUtil.getRelativePath(projectInfo.projectPath, path, '/')!! to it
            }
            .toMap()
    }

    private fun report(text: String) {
        projectInfo.messageCollector.report("Module '${module.name}': $text")
    }

    private fun checkReport(subject: String, expected: Any?, actual: Any?) {
        if (expected != actual) {
            report("$subject differs: expected $expected, got $actual")
        }
    }

    fun externalSystemTestTask(taskName: String, projectId: String, targetName: String) {
        expectedExternalSystemTestTasks.add(ExternalSystemTestRunTask(taskName, projectId, targetName))
    }

    fun languageVersion(expectedVersion: String) {
        val actualVersion = module.languageVersionSettings.languageVersion.versionString
        checkReport("Language version", expectedVersion, actualVersion)
    }

    fun isHMPP(expectedValue: Boolean) {
        checkReport("isHMPP", expectedValue, module.isHMPPEnabled)
    }

    fun targetPlatform(vararg platforms: TargetPlatform) {
        val expected = platforms.flatMap { it.componentPlatforms }.toSet()
        val actual = module.platform?.componentPlatforms

        if (actual == null) {
            report("Actual target platform is null")
            return
        }

        val notFound = expected.subtract(actual)
        if (notFound.isNotEmpty()) {
            report("These target platforms were not found: " + notFound.joinToString())
        }

        val unexpected = actual.subtract(expected)
        if (unexpected.isNotEmpty()) {
            report("Unexpected target platforms found: " + unexpected.joinToString())
        }
    }

    fun apiVersion(expectedVersion: String) {
        val actualVersion = module.languageVersionSettings.apiVersion.versionString
        checkReport("API version", expectedVersion, actualVersion)
    }

    fun platform(expectedPlatform: TargetPlatform) {
        val actualPlatform = module.platform
        checkReport("Platform", expectedPlatform, actualPlatform)
    }

    fun additionalArguments(arguments: String?) {
        val actualArguments = KotlinFacet.get(module)?.configuration?.settings?.compilerSettings?.additionalArguments
        checkReport("Additional arguments", arguments, actualArguments)
    }

    fun libraryDependency(libraryName: String, scope: DependencyScope) {
        val libraryEntries = rootModel.orderEntries.filterIsInstance<LibraryOrderEntry>().filter { it.libraryName == libraryName }
        if (libraryEntries.size > 1) {
            report("Multiple root entries for library $libraryName")
        }
        if (libraryEntries.isEmpty()) {
            val candidate = rootModel.orderEntries
                .filterIsInstance<LibraryOrderEntry>()
                .sortedWith(Comparator { o1, o2 ->
                    val o1len = o1?.libraryName?.commonPrefixWith(libraryName)?.length ?: 0
                    val o2len = o2?.libraryName?.commonPrefixWith(libraryName)?.length ?: 0
                    o2len - o1len
                }).firstOrNull()

            val candidateName = candidate?.libraryName
            report("Expected library dependency $libraryName, found nothing. Most probably candidate: $candidateName")
        }
        checkLibrary(libraryEntries.singleOrNull(), libraryName, scope)
    }

    fun libraryDependencyByUrl(classesUrl: String, scope: DependencyScope) {
        val libraryEntries = rootModel.orderEntries.filterIsInstance<LibraryOrderEntry>().filter { entry ->
            entry.library?.getUrls(OrderRootType.CLASSES)?.any { it == classesUrl } ?: false
        }
        if (libraryEntries.size > 1) {
            report("Multiple entries for library $classesUrl")
        }
        checkLibrary(libraryEntries.singleOrNull(), classesUrl, scope)
    }

    private fun checkLibrary(libraryEntry: LibraryOrderEntry?, id: String, scope: DependencyScope) {
        if (libraryEntry == null) {
            report("No library dependency found for $id")
            return
        }
        checkDependencyScope(libraryEntry, scope)
        expectedDependencyNames += libraryEntry.debugText
    }

    fun moduleDependency(moduleName: String, scope: DependencyScope, productionOnTest: Boolean? = null, allowMultiple: Boolean = false) {
        val moduleEntries = rootModel.orderEntries.asList()
            .filterIsInstanceWithChecker<ModuleOrderEntry> { it.moduleName == moduleName && it.scope == scope }

        // In normal conditions, 'allowMultiple' should always be 'false'. In reality, however, a lot of tests fails because of it.
        if (!allowMultiple && moduleEntries.size > 1) {
            val allEntries = rootModel.orderEntries.filterIsInstance<ModuleOrderEntry>().joinToString { it.debugText }
            report("Multiple order entries found for module $moduleName: $allEntries")
            return
        }

        val moduleEntry = moduleEntries.firstOrNull()

        if (moduleEntry == null) {
            val allModules = rootModel.orderEntries.filterIsInstance<ModuleOrderEntry>().joinToString { it.debugText }
            report("Module dependency ${moduleName} (${scope.displayName}) not found. All module dependencies: $allModules")
            return
        }
        checkDependencyScope(moduleEntry, scope)
        checkProductionOnTest(moduleEntry, productionOnTest)
        expectedDependencyNames += moduleEntry.debugText
    }

    private val ANY_PACKAGE_PREFIX = "any_package_prefix"

    fun sourceFolder(pathInProject: String, rootType: JpsModuleSourceRootType<*>, packagePrefix: String? = ANY_PACKAGE_PREFIX) {
        val sourceFolder = sourceFolderByPath[pathInProject]
        if (sourceFolder == null) {
            report("No source root found: '$pathInProject' among $sourceFolderByPath")
            return
        }
        if (packagePrefix != ANY_PACKAGE_PREFIX && sourceFolder.packagePrefix != packagePrefix) {
            report("Source root '$pathInProject': Expected package prefix $packagePrefix, got: ${sourceFolder.packagePrefix}")
        }
        expectedSourceRoots += pathInProject
        val actualRootType = sourceFolder.rootType
        if (actualRootType != rootType) {
            report("Source root '$pathInProject': Expected root type $rootType, got: $actualRootType")
            return
        }
    }

    fun inheritProjectOutput() {
        val isInherited = CompilerModuleExtension.getInstance(module)?.isCompilerOutputPathInherited ?: true
        if (!isInherited) {
            report("Project output is not inherited")
        }
    }

    fun outputPath(pathInProject: String, isProduction: Boolean) {
        val compilerModuleExtension = CompilerModuleExtension.getInstance(module)
        val url = if (isProduction) compilerModuleExtension?.compilerOutputUrl else compilerModuleExtension?.compilerOutputUrlForTests
        val actualPathInProject = url?.let {
            FileUtil.getRelativePath(
                projectInfo.projectPath,
                JpsPathUtil.urlToPath(
                    it
                ),
                '/'
            )
        }

        checkReport("Output path", pathInProject, actualPathInProject)
    }

    fun run(body: ModuleInfo.() -> Unit = {}) {
        body()

        if (projectInfo.exhaustiveDependencyList) {
            val actualDependencyNames = rootModel
                .orderEntries.asList()
                .filterIsInstanceWithChecker<ExportableOrderEntry> { it is ModuleOrderEntry || it is LibraryOrderEntry }
                .map { it.debugText }
                .sorted()
                .distinct()

            val expectedDependencyNames = expectedDependencyNames.sorted()
            checkReport("Dependency list", expectedDependencyNames, actualDependencyNames)
        }

        val actualTasks = module.externalSystemTestRunTasks()

        val containsAllTasks = actualTasks.containsAll(expectedExternalSystemTestTasks)
        val containsSameTasks = actualTasks == expectedExternalSystemTestTasks

        if ((!containsAllTasks) || (projectInfo.exhaustiveTestsList && !containsSameTasks)) {
            report("Expected tests list $expectedExternalSystemTestTasks, got: $actualTasks")
        }

        if (projectInfo.exhaustiveSourceSourceRootList) {
            val actualSourceRoots = sourceFolderByPath.keys.sorted()
            val expectedSourceRoots = expectedSourceRoots.sorted()
            if (actualSourceRoots != expectedSourceRoots) {
                report("Expected source root list $expectedSourceRoots, got: $actualSourceRoots")
            }
        }

        if (rootModel.sdk == null) {
            report("No SDK defined")
        }
    }

    private fun checkDependencyScope(library: ExportableOrderEntry, expectedScope: DependencyScope) {
        checkReport("Dependency scope", expectedScope, library.scope)
    }

    private fun checkProductionOnTest(library: ExportableOrderEntry, productionOnTest: Boolean?) {
        if (productionOnTest == null) return
        val actualFlag = (library as? ModuleOrderEntry)?.isProductionOnTestDependency
        if (actualFlag == null) {
            report("Dependency '${library.presentableName}' has no 'productionOnTest' property")
        } else {
            if (actualFlag != productionOnTest) {
                report("Dependency '${library.presentableName}': expected productionOnTest '$productionOnTest', got '$actualFlag'")
            }
        }

    }
}

fun checkProjectStructure(
    project: Project,
    projectPath: String,
    exhaustiveModuleList: Boolean,
    exhaustiveSourceSourceRootList: Boolean,
    exhaustiveDependencyList: Boolean,
    exhaustiveTestsList: Boolean,
    body: ProjectInfo.() -> Unit = {}
) {
    ProjectInfo(
        project,
        projectPath,
        exhaustiveModuleList,
        exhaustiveSourceSourceRootList,
        exhaustiveDependencyList,
        exhaustiveTestsList
    ).run(body)
}

private val ExportableOrderEntry.debugText: String
    get() = "$presentableName (${scope.displayName})"