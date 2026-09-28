/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportDependencySelector
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportResolvedComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.swiftExportDependencySelectorFactory
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwiftExportDependencySelectorTests {

    private fun Project.selectorFor(dependency: Any): SwiftExportDependencySelector =
        swiftExportDependencySelectorFactory().fromNotation(dependency)

    private fun moduleVersion(group: String, name: String, version: String) = object : ModuleVersionIdentifier {
        override fun getGroup() = group
        override fun getName() = name
        override fun getVersion() = version
        override fun getModule() = object : ModuleIdentifier {
            override fun getGroup() = group
            override fun getName() = name
        }
    }

    private fun moduleComponent(group: String, name: String, version: String = "1.0") = SwiftExportResolvedComponent(
        id = object : ModuleComponentIdentifier {
            override fun getGroup() = group
            override fun getModule() = name
            override fun getVersion() = version
            override fun getModuleIdentifier() = moduleVersion(group, name, version).module
            override fun getDisplayName() = "$group:$name:$version"
        },
        rootComponentId = object : ModuleComponentIdentifier {
            override fun getGroup() = group
            override fun getModule() = name
            override fun getVersion() = version
            override fun getModuleIdentifier() = moduleVersion(group, name, version).module
            override fun getDisplayName() = "$group:$name:$version"
        },
        moduleVersion = moduleVersion(group, name, version),
    )

    /**
     * A real [org.gradle.api.artifacts.component.ProjectComponentIdentifier] from resolving a project dependency:
     * the interface gains methods across Gradle versions, so a hand-written double wouldn't compile everywhere.
     */
    private fun projectComponent(path: String, publishedAs: Pair<String, String>? = null): SwiftExportResolvedComponent {
        val root = buildProject()
        val name = path.substringAfterLast(':')
        buildProject(projectBuilder = { withParent(root); withName(name) }) {
            configurations.consumable("forTest")
        }
        val resolvable = root.configurations.detachedConfiguration(
            root.dependencies.project(mapOf("path" to path, "configuration" to "forTest"))
        )
        val dependencyResult = resolvable.incoming.resolutionResult.root.dependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .single()
        val selected = dependencyResult.selected
        return SwiftExportResolvedComponent(
            id = selected.id,
            rootComponentId = dependencyResult.resolvedVariant.owner,
            moduleVersion = publishedAs?.let { (group, module) -> moduleVersion(group, module, "1.0") },
        )
    }

    @Test
    fun `string coordinates convert to a module selector without the version`() {
        val project = buildProjectWithMPP()

        assertEquals(
            SwiftExportDependencySelector.Module(group = "org.example", name = "foo"),
            project.selectorFor("org.example:foo:1.0"),
        )
    }

    @Test
    fun `the same module selector results from different requested versions`() {
        val project = buildProjectWithMPP()

        assertEquals(
            project.selectorFor("org.example:foo:1.0"),
            project.selectorFor("org.example:foo:2.5"),
        )
    }

    @Test
    fun `a created external dependency converts to a module selector`() {
        val project = buildProjectWithMPP()
        val dependency = project.dependencies.create("org.example:foo:1.0")

        assertEquals(
            SwiftExportDependencySelector.Module(group = "org.example", name = "foo"),
            project.selectorFor(dependency),
        )
    }

    @Test
    fun `a project converts to a project path selector`() {
        val root = buildProjectWithMPP()
        val sub = buildProjectWithMPP(projectBuilder = { withParent(root); withName("sub") })

        assertEquals(SwiftExportDependencySelector.ProjectPath(":sub"), root.selectorFor(sub))
    }

    @Test
    fun `a project dependency converts to a project path selector`() {
        val root = buildProjectWithMPP()
        buildProjectWithMPP(projectBuilder = { withParent(root); withName("sub") })
        val dependency = root.dependencies.project(mapOf("path" to ":sub"))

        assertEquals(SwiftExportDependencySelector.ProjectPath(":sub"), root.selectorFor(dependency))
    }

    @Test
    fun `selector display names are suitable for diagnostics`() {
        assertEquals(
            "org.example:foo",
            SwiftExportDependencySelector.Module(group = "org.example", name = "foo").displayName,
        )
        assertEquals(
            ":shared:core",
            SwiftExportDependencySelector.ProjectPath(":shared:core").displayName,
        )
    }

    @Test
    fun `a dependency without a group is rejected at conversion`() {
        val project = buildProjectWithMPP()

        assertFailsWith<InvalidUserDataException> { project.selectorFor(":no-group") }
    }

    @Test
    fun `a module selector matches an external component regardless of version`() {
        val selector = SwiftExportDependencySelector.Module(group = "org.example", name = "foo")

        assertTrue(selector.matches(moduleComponent("org.example", "foo", version = "2.5")))
        assertFalse(selector.matches(moduleComponent("org.example", "bar")))
    }

    @Test
    fun `a module selector matches a project the module was substituted with`() {
        val selector = SwiftExportDependencySelector.Module(group = "org.example", name = "foo")

        assertTrue(selector.matches(projectComponent(":foo", publishedAs = "org.example" to "foo")))
        assertFalse(selector.matches(projectComponent(":foo", publishedAs = "org.example" to "bar")))
        assertFalse(selector.matches(projectComponent(":foo")))
    }

    @Test
    fun `a project path selector matches only project components`() {
        val selector = SwiftExportDependencySelector.ProjectPath(":foo")

        assertTrue(selector.matches(projectComponent(":foo")))
        assertFalse(selector.matches(projectComponent(":bar")))
        assertFalse(selector.matches(moduleComponent("org.example", "foo")))
    }

    @Test
    fun `an unknown component identifier matches nothing`() {
        val component = SwiftExportResolvedComponent(
            id = { "opaque" },
            rootComponentId = { "opaque" },
            moduleVersion = null
        )

        assertFalse(SwiftExportDependencySelector.Module(group = "org.example", name = "foo").matches(component))
        assertFalse(SwiftExportDependencySelector.ProjectPath(":foo").matches(component))
    }
}
