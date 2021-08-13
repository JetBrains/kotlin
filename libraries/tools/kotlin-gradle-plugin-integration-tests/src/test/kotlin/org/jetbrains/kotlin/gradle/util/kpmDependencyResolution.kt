/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
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
        val project = projects.singleOrNull { it.name == name } ?: TestKpmGradleProject(name).also(projects::add)
        configure(project)
        return project
    }

    override fun toString(): String = name ?: "<no name>"
}

internal class TestKpmGradleProject(val name: String) {
    val modules: ConfigurableSet<TestKpmModule> = run {
        val main = TestKpmModule(this, "main")
        val test = TestKpmModule(this, "test").apply {
            fragmentNamed("common").moduleDependencies.add(TestKpmModuleDependency(main, TestDependencyKind.DIRECT))
        }
        ConfigurableSet<TestKpmModule>().apply {
            add(main)
            add(test)
        }
    }

    fun allModules(action: TestKpmModule.() -> Unit) {
        modules.withAll(action)
    }

    fun module(name: String, configure: TestKpmModule.() -> Unit): TestKpmModule {
        val module = modules.singleOrNull { it.name == name } ?: TestKpmModule(this, name)
            .also(modules::add)
        configure(module)
        return module
    }

    fun moduleNamed(name: String) = modules.single { it.name == name }
    val main get() = moduleNamed("main")
    val test get() = moduleNamed("test")

    override fun toString(): String = ":$name"
}

internal class TestKpmModule(
    val kpmGradleProject: TestKpmGradleProject,
    val name: String
) {
    var publicationMode: KpmModulePublicationMode =
        if (name == MAIN_NAME) KpmModulePublicationMode.STANDALONE else KpmModulePublicationMode.PRIVATE

    val fragments = ConfigurableSet<TestKpmFragment>().apply {
        add(TestKpmFragment(this@TestKpmModule, "common"))
    }

    fun fragment(name: String, kind: FragmentKind, configure: TestKpmFragment.() -> Unit = { }): TestKpmFragment {
        val result = fragments.singleOrNull { it.name == name }
            ?.also { require(it.kind == kind) { "There's already a fragment with the name $name and kind $kind" } }
            ?: TestKpmFragment(this, name, kind).also(fragments::add)
        return result.also(configure)
    }

    fun fragmentNamed(name: String): TestKpmFragment = fragments.single { it.name == name }

    companion object {
        const val MAIN_NAME = "main"
    }

    fun depends(otherModule: TestKpmModule, kind: TestDependencyKind) {
        fragmentNamed("common").depends(otherModule, kind)
    }

    fun makePublic(publicationMode: KpmModulePublicationMode) {
        this.publicationMode = publicationMode
    }
}

internal enum class FragmentKind(val gradleType: String) {
    COMMON_FRAGMENT("KotlinGradleFragment"),
    JVM_VARIANT("KotlinJvmVariant"),
    LINUXX64_VARIANT("KotlinLinuxX64Variant"),
    IOSARM64_VARIANT("KotlinIosArm64Variant"),
    IOSX64_VARIANT("KotlinIosX64Variant")
}

internal class TestKpmFragment(
    val module: TestKpmModule,
    val name: String,
    val kind: FragmentKind = FragmentKind.COMMON_FRAGMENT,
) {
    val refines = mutableSetOf<TestKpmFragment>()
    val moduleDependencies = mutableSetOf<TestKpmModuleDependency>()

    fun refines(vararg otherFragments: TestKpmFragment) {
        require(otherFragments.all { it.module === module }) { "Only refinement within one module is supported" }
        refines.addAll(otherFragments)
    }

    val refinesClosure: Set<TestKpmFragment>
        get() = mutableSetOf<TestKpmFragment>().apply {
            fun visit(f: TestKpmFragment) {
                if (add(f)) f.refines.forEach(::visit)
            }
            visit(this@TestKpmFragment)
        }

    fun depends(otherModule: TestKpmModule, kind: TestDependencyKind) {
        if (otherModule.kpmGradleProject !== module.kpmGradleProject) {
            require(kind != TestDependencyKind.DIRECT) { "Direct dependencies are only allowed within one project" }
        }
        moduleDependencies.add(TestKpmModuleDependency(otherModule, kind))
    }

    private sealed class ExpectVisibilityItem {
        abstract val fragments: Iterable<TestKpmFragment>

        class ExpectVisibilityOf(override val fragments: Iterable<TestKpmFragment>) : ExpectVisibilityItem()
        class ExpectVisibilityOfLazy(val provideFragments: () -> Iterable<TestKpmFragment>) : ExpectVisibilityItem() {
            override val fragments: Iterable<TestKpmFragment> get() = provideFragments()
        }
    }

    private val _expectsVisibility = mutableListOf<ExpectVisibilityItem>()
    val expectsVisibility: Iterable<TestKpmFragment> get() = _expectsVisibility.flatMapTo(mutableSetOf(), ExpectVisibilityItem::fragments)

    fun expectVisibility(otherFragment: TestKpmFragment) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOf(listOf(otherFragment)))
    }

    fun expectVisibility(otherFragments: Iterable<TestKpmFragment>) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOf(otherFragments))
    }

    fun expectVisibility(provideOtherFragments: () -> Iterable<TestKpmFragment>) {
        _expectsVisibility.add(ExpectVisibilityItem.ExpectVisibilityOfLazy(provideOtherFragments))
    }
}

internal fun TestKpmModule.expectVisibilityOfSimilarStructure(testKpmModule: TestKpmModule) =
    fragments.withAll { expectVisibilityOfSimilarStructure(testKpmModule) }

internal fun TestKpmFragment.expectVisibilityOfSimilarStructure(testKpmModule: TestKpmModule) {
    val fragmentNames = refinesClosure.mapTo(mutableSetOf()) { it.name }
    testKpmModule.fragments.withAll {
        if (name in fragmentNames) this@expectVisibilityOfSimilarStructure.expectVisibility { this@withAll.refinesClosure }
    }
}

internal enum class KpmModulePublicationMode {
    PRIVATE, STANDALONE, EMBEDDED
}

internal enum class TestDependencyKind {
    DIRECT, PROJECT, PUBLISHED,
}

internal class TestKpmModuleDependency(val module: TestKpmModule, val dependencyKind: TestDependencyKind)

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
                        dep.module.kpmGradleProject.name else
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

internal class ConfigurableSet<T> private constructor(val _items: MutableSet<T>) : Set<T> by _items {
    constructor() : this(mutableSetOf())

    private val allItemsActions = mutableListOf<T.() -> Unit>()

    fun add(item: T) {
        allItemsActions.forEach { action -> action(item) }
        _items.add(item)
    }

    fun withAll(action: T.() -> Unit) {
        _items.forEach(action)
        allItemsActions.add(action)
    }
}