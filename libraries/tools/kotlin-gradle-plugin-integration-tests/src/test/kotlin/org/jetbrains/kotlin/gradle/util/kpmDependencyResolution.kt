/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.jetbrains.kotlin.gradle.kpm.ConfigurableSet
import org.jetbrains.kotlin.gradle.kpm.KpmModulePublicationMode
import org.jetbrains.kotlin.gradle.kpm.TestDependencyKind
import org.jetbrains.kotlin.gradle.kpm.TestKpmFragment
import org.jetbrains.kotlin.gradle.kpm.TestKpmGradleProject
import org.jetbrains.kotlin.gradle.kpm.TestKpmModule
import org.jetbrains.kotlin.gradle.kpm.TestKpmModuleDependency
import java.io.File

internal class PublishAllTestCaseExecutor {
    fun BaseGradleIT.execute(testCase: KpmDependencyResolutionTestCase) {
        val buildable = prepare(testCase)
        val buildOptions = defaultBuildOptions().copy(withBuildCache = true)
        buildable.topologicallySortedProjects.forEach { kpmProject ->
            // Running multiple builds and not just one is required because otherwise Gradle will fail to resolve dependencies on modules
            // that haven't been published yet.
            buildable.project.build(":${kpmProject.name}:publish", options = buildOptions) {
                assertSuccessful()
            }
        }
    }
}

internal class KpmDependencyResolutionTestCase(val name: String?) {
    val projects = ConfigurableSet<TestKpmGradleProject>()

    fun project(name: String, configure: TestKpmGradleProject.() -> Unit = { }): TestKpmGradleProject {
        val project = projects.getOrPut(name) { TestKpmGradleProject(name) }
        configure(project)
        return project
    }

    fun allModules(configure: TestKpmModule.() -> Unit) {
        projects.withAll {
            modules.withAll(configure)
        }
    }

    fun allFragments(configure: TestKpmFragment.() -> Unit) {
        projects.withAll {
            modules.withAll {
                fragments.withAll(configure)
            }
        }
    }

    override fun toString(): String = name ?: "<no name>"
}

internal fun TestKpmModule.expectVisibilityOfSimilarStructure(testKpmModule: TestKpmModule) =
    fragments.withAll { expectVisibilityOfSimilarStructure(testKpmModule) }

internal fun TestKpmFragment.expectVisibilityOfSimilarStructure(testKpmModule: TestKpmModule) {
    val fragmentNames = refinesClosure.mapTo(mutableSetOf()) { it.name }
    testKpmModule.fragments.withAll {
        if (name in fragmentNames) this@expectVisibilityOfSimilarStructure.expectVisibility { this@withAll.refinesClosure }
    }
}

internal data class BuildableTestCase(
    val testCase: KpmDependencyResolutionTestCase,
    val project: BaseGradleIT.Project,
    val projectsWithPublishedDependencies: Set<TestKpmGradleProject>,
    val topologicallySortedProjects: List<TestKpmGradleProject>
)

internal fun BaseGradleIT.prepare(
    testCase: KpmDependencyResolutionTestCase,
): BuildableTestCase {
    val group = "com.example"
    val version = "1.0"

    val moduleDependencyGraph =
        testCase.projects.associateWith { it.modules.flatMap { it.fragments.flatMap { it.moduleDependencies } } }

    val topSortedModules = topSort(moduleDependencyGraph.keys, fromNodesFirst = false) { node ->
        moduleDependencyGraph.getValue(node).map { it.module.kpmGradleProject }.filter { it != node }
    }
    val needToPublish = moduleDependencyGraph.flatMap { (_, edges) ->
        edges.mapNotNull { edge -> edge.module.kpmGradleProject.takeIf { edge.dependencyKind == TestDependencyKind.PUBLISHED } }
    }.toSet()

    val project = Project("pm20-template", GradleVersionRequired.AtLeast("7.0")).apply {
        projectDir.deleteRecursively()
        setupWorkingDir()
        testCase.projects.forEach { subproject ->
            gradleSettingsScript().appendText("\ninclude(\"${subproject.name}\")")

            val dir = projectDir.resolve(subproject.name)
            dir.mkdirs()

            generateBuildScript(this, subproject, group, version)

            subproject.modules.forEach { module ->
                module.fragments.forEach { fragment ->
                    generateFragmentSources(this, subproject, fragment, module)
                }
            }
        }
    }

    return BuildableTestCase(testCase, project, needToPublish, topSortedModules)
}

private fun generateBuildScript(
    project: BaseGradleIT.Project,
    subproject: TestKpmGradleProject,
    group: String,
    version: String
) {
    project.projectDir.resolve("${subproject.name}/build.gradle.kts").writeText(buildIndentedString {
        appendLine("import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*\n")

        appendLine("group = \"$group\"")
        appendLine("version = \"$version\"\n")

        withIndent("plugins {", "}") {
            appendLine("kotlin(\"multiplatform.pm20\")")
            appendLine("`maven-publish`")
        }

        withIndent("kotlin {", "}") {
            subproject.modules.forEach { module ->
                withIndent("modules.maybeCreate(\"${module.name}\").apply {", "}\n") {
                    when (module.publicationMode) {
                        KpmModulePublicationMode.STANDALONE -> if (module.name != "main") appendLine("makePublic()")
                        KpmModulePublicationMode.EMBEDDED -> appendLine("makePublic(Embedded)")
                        else -> Unit
                    }

                    val topSortedFragments = topSort(module.fragments, fromNodesFirst = false) { it.refines }
                    topSortedFragments.forEach { fragment ->
                        withIndent("\nfragments.maybeCreate(\"${fragment.name}\", ${fragment.kind.gradleType}::class).apply {", "}\n") {
                            fragment.refines.forEach { other ->
                                appendLine("refines(fragments[\"${other.name}\"])")
                            }
                            if (fragment.moduleDependencies.isNotEmpty()) {
                                withIndent("dependencies {", "}") {
                                    fragment.moduleDependencies.forEach { dep ->
                                        appendLine("api(${moduleDependencyNotation(dep, group, version)})")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    })
}

private fun moduleDependencyNotation(
    dep: TestKpmModuleDependency,
    group: String,
    version: String
): String {
    val moduleNotation = when (dep.dependencyKind) {
        TestDependencyKind.DIRECT -> "modules.getByName(\"${dep.module.name}\")"
        TestDependencyKind.PROJECT -> "project(\":${dep.module.kpmGradleProject.name}\")"
        TestDependencyKind.PUBLISHED -> when (dep.module.publicationMode) {
            KpmModulePublicationMode.EMBEDDED -> "\"$group:${dep.module.kpmGradleProject.name}:$version\""
            KpmModulePublicationMode.STANDALONE -> {
                val moduleName =
                    if (dep.module.name == TestKpmModule.MAIN_NAME)
                        dep.module.kpmGradleProject.name
                    else
                        "${dep.module.kpmGradleProject.name}-${dep.module.name.toLowerCase()}"
                "\"$group:$moduleName:$version\""
            }
            KpmModulePublicationMode.PRIVATE -> error("dependency on published module points to a private module")
        }
    }
    val notationWithAuxiliaryModulePart =
        if (dep.module.name != TestKpmModule.MAIN_NAME && (
                    dep.dependencyKind == TestDependencyKind.PROJECT ||
                            dep.dependencyKind == TestDependencyKind.PUBLISHED &&
                            dep.module.publicationMode == KpmModulePublicationMode.EMBEDDED
                    )
        ) "kotlinAuxiliaryModule($moduleNotation, \"${dep.module.name}\")"
        else moduleNotation
    return notationWithAuxiliaryModulePart
}

private fun generateFragmentSources(
    project: BaseGradleIT.Project,
    subproject: TestKpmGradleProject,
    fragment: TestKpmFragment,
    module: TestKpmModule
) {
    fun fragmentPackage(testKpmFragment: TestKpmFragment) =
        "${testKpmFragment.module.kpmGradleProject.name}.${testKpmFragment.module.name}.${testKpmFragment.name}"

    val fragmentSrcDir =
        project.projectDir.resolve("${subproject.name}/src/${fragment.name}${module.name.capitalize()}/kotlin").also(File::mkdirs)

    val functionName = "testFunction"

    fragmentSrcDir.resolve("${fragmentPackage(fragment)}.own.kt").writeText(
        """
            package ${fragmentPackage(fragment)}
            fun ${functionName}() = Unit
        """.trimIndent()
    )
    fragment.expectsVisibility.forEach { otherFragment ->
        fragmentSrcDir.resolve("${fragmentPackage(fragment)}.sees.${fragmentPackage(otherFragment)}.kt").writeText(
            """
                package ${fragmentPackage(fragment)}.sees.${fragmentPackage(otherFragment)}
                fun invoke() = ${fragmentPackage(otherFragment)}.${functionName}()
            """.trimIndent()
        )
    }

}

private fun <T> topSort(nodes: Iterable<T>, fromNodesFirst: Boolean = true, edges: (T) -> Iterable<T>): List<T> {
    val grey = mutableSetOf<T>()
    val black = mutableSetOf<T>()
    fun visit(t: T) {
        if (!grey.add(t)) error("Found a loop in the graph: ${grey.joinToString()}")
        edges(t).forEach { if (it !in black) visit(it) }
        grey.remove(t)
        black.add(t)
    }
    nodes.forEach(::visit)
    return black.toList().let { if (fromNodesFirst) it.reversed() else it }
}

internal interface IndentedStringBuilder {
    fun appendLine(line: Any)
    fun withIndent(lineBeforeIndent: Any? = null, lineAfterIndent: Any? = null, action: IndentedStringBuilder.() -> Unit)
}

internal fun buildIndentedString(builderAction: IndentedStringBuilder.() -> Unit): String {
    val indentationString = "    "

    class IndentedStringBuilderImpl(val stringBuilder: StringBuilder, val indentationLevel: Int) : IndentedStringBuilder {
        override fun appendLine(line: Any) {
            stringBuilder.appendLine("${indentationString.repeat(indentationLevel)}$line")
        }

        override fun withIndent(lineBeforeIndent: Any?, lineAfterIndent: Any?, action: IndentedStringBuilder.() -> Unit) {
            lineBeforeIndent?.let(::appendLine)
            IndentedStringBuilderImpl(stringBuilder, indentationLevel + 1).action()
            lineAfterIndent?.let(::appendLine)
        }
    }
    return IndentedStringBuilderImpl(StringBuilder(), 0).also(builderAction).stringBuilder.toString()
}
