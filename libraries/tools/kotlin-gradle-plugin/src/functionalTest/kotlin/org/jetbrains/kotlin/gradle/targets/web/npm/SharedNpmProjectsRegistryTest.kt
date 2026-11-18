/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.KotlinWebPlatformType
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSharedNpmProjectsRegistry
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.npmSharedDependenciesConfigurationName
import org.jetbrains.kotlin.gradle.util.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedNpmProjectsRegistryTest {

    private val root = buildProject(projectBuilder = { withName("root-project") }) {
        KotlinSharedNpmProjectsRegistry.registerForSettings(gradle)
        plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
    }

    private fun subproject(
        name: String,
        parent: Project = root,
        webIsolatedProjectsSupportPreview: Boolean = false,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ): Project {
        val project = buildProjectWithMPP(
            projectBuilder = { withParent(parent).withName(name) },
            preApplyCode = {
                if (webIsolatedProjectsSupportPreview) enableWebIsolatedProjectsSupportPreview()
            },
        ) {
            kotlin(configure)
        }
        return project.evaluate()
    }

    private fun reportFiles(platformType: KotlinPlatformType, vararg projects: Project): List<File> =
        projects
            .flatMap { it.multiplatformExtension.targets.filterIsInstance<KotlinJsIrTarget>() }
            .filter { it.platformType == platformType }
            .flatMap { it.compilations }
            .map { it.npmDependenciesReportFile() }
            .sorted()

    private fun resolvedFiles(configurationName: String, project: Project = root): List<File> =
        project.configurations.getByName(configurationName).incoming.artifactView { it.lenient(true) }.files.files.sorted()

    @Test
    fun `projects with JS or WasmJS targets are added to the shared npm project without declaring them on the root`() {
        val jsBrowser = subproject("js-browser") { js { browser() } }
        val wasmJsBrowser = subproject("wasm-js-browser") { wasmJs { browser() } }
        val jsAndWasmJs = subproject("js-and-wasm-js") {
            js { nodejs() }
            wasmJs { nodejs() }
        }
        subproject("jvm") { jvm() }

        assertEquals(
            reportFiles(KotlinPlatformType.js, jsBrowser, jsAndWasmJs),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver"),
        )
        assertEquals(
            reportFiles(KotlinPlatformType.wasm, wasmJsBrowser, jsAndWasmJs),
            resolvedFiles("kotlinNpmDependenciesReportWasmJsResolver"),
        )
        jsBrowser.assertNoDiagnostics(KotlinToolingDiagnostics.SharedNpmProjectRegisteredTooLate)
    }

    // TODO KT-91654 Also cover sealing a platform that wasn't read: resolve only JS, then add a WasmJS-only project, and check the warning and that the WasmJS resolver excludes it.
    @Test
    fun `projects configured after the shared npm project resolved its dependencies are not collected and warn`() {
        val early = subproject("early") { js { browser() } }
        assertEquals(
            reportFiles(KotlinPlatformType.js, early),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver"),
        )

        val late = subproject("late") {
            js { browser() }
            wasmJs { browser() }
        }

        // Reported once, although :late has both JS and WasmJS targets.
        late.kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(late.path)
            .assertContainsSingleDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectRegisteredTooLate)
        late.assertContainsDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectRegisteredTooLate(late.path))
        assertEquals(
            reportFiles(KotlinPlatformType.js, early),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver"),
        )
        early.assertNoDiagnostics(KotlinToolingDiagnostics.SharedNpmProjectRegisteredTooLate)
    }

    @Test
    fun `without the settings plugin projects are not collected`() {
        val rootWithoutSettingsPlugin = buildProject(projectBuilder = { withName("root-without-settings-plugin") }) {
            plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
        }
        subproject("js-browser", parent = rootWithoutSettingsPlugin) { js { browser() } }

        assertEquals(
            emptyList(),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver", rootWithoutSettingsPlugin),
        )

        val registryNames = rootWithoutSettingsPlugin.gradle.sharedServices.registrations.names
            .filter { it.startsWith("KotlinSharedNpmProjectsRegistry") }
        assertEquals(emptyList(), registryNames, "KGP must not register the registry itself")
    }

    @Test
    fun `without the shared npm project no warning is reported`() {
        val rootWithoutSharedNpmProject = buildProject(projectBuilder = { withName("root-without-shared-npm-project") })
        val jsBrowser = subproject("js-browser", parent = rootWithoutSharedNpmProject) { js { browser() } }

        jsBrowser.assertNoDiagnostics(KotlinToolingDiagnostics.SharedNpmProjectEcosystemPluginMissing)
        assertNull(
            rootWithoutSharedNpmProject.configurations.findByName(KotlinWebPlatformType.JS.npmSharedDependenciesConfigurationName),
            "Without Preview, KGP must not apply the shared npm project plugin",
        )
    }

    // TODO KT-91654 Add a test where the root project itself has JS/WasmJS targets: with the registry its own reports are collected once, and under Preview without the settings plugin it applies the shared npm project plugin to itself from its own target side effect.
    @Test
    fun `with Preview and without the settings plugin the root project collects the other projects and warns`() {
        val rootProject = buildProject(projectBuilder = { withName("root-preview") }) {
            enableWebIsolatedProjectsSupportPreview()
        }
        val jsBrowser = subproject("js-browser", parent = rootProject, webIsolatedProjectsSupportPreview = true) {
            js { browser() }
        }
        val wasmJsBrowser = subproject("wasm-js-browser", parent = rootProject, webIsolatedProjectsSupportPreview = true) {
            wasmJs { browser() }
        }
        subproject("jvm", parent = rootProject, webIsolatedProjectsSupportPreview = true) { jvm() }

        assertEquals(
            reportFiles(KotlinPlatformType.js, jsBrowser),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver", rootProject),
        )
        assertEquals(
            reportFiles(KotlinPlatformType.wasm, wasmJsBrowser),
            resolvedFiles("kotlinNpmDependenciesReportWasmJsResolver", rootProject),
        )
        // Reported once per build, by the first project that needs the shared npm project.
        jsBrowser.assertContainsDiagnostic(KotlinToolingDiagnostics.SharedNpmProjectEcosystemPluginMissing(isolatedProjectsEnabled = false))
        wasmJsBrowser.assertNoDiagnostics(KotlinToolingDiagnostics.SharedNpmProjectEcosystemPluginMissing)
        assertEquals(
            listOf(KotlinSharedNpmProjectsRegistry.FALLBACK_SERVICE_NAME),
            rootProject.gradle.sharedServices.registrations.names.filter { it.startsWith("KotlinSharedNpmProjectsRegistry") },
            "Without the settings plugin, KGP registers the fallback registry",
        )
    }

    @Test
    fun `without the settings plugin only the declared projects are collected`() {
        val project = buildProject(projectBuilder = { withName("root-without-settings-plugin") }) {
            plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
        }
        val jsBrowser = subproject("js-browser", parent = project) {
            js { browser() }
        }
        val wasmJsBrowser = subproject("wasm-js-browser", parent = project) {
            wasmJs { browser() }
        }
        subproject("undeclared", parent = project) {
            js { nodejs() }
            wasmJs { nodejs() }
        }
        project.dependencies.kotlinNpmSharedDependenciesJs(jsBrowser.path)
        project.dependencies.kotlinNpmSharedDependenciesWasmJs(wasmJsBrowser.path)

        assertEquals(
            reportFiles(KotlinPlatformType.js, jsBrowser),
            resolvedFiles("kotlinNpmDependenciesReportJsResolver", project),
        )
        assertEquals(
            reportFiles(KotlinPlatformType.wasm, wasmJsBrowser),
            resolvedFiles("kotlinNpmDependenciesReportWasmJsResolver", project),
        )
    }

    @Test
    fun `declaring a project that is also in the registry collects it once`() {
        val jsBrowser = subproject("js-browser") {
            js { browser() }
        }
        root.dependencies.kotlinNpmSharedDependenciesJs(jsBrowser.path)

        assertEquals(
            reportFiles(KotlinPlatformType.js, jsBrowser),
            root.configurations.getByName("kotlinNpmDependenciesReportJsResolver")
                .incoming.artifactView { it.lenient(true) }.artifacts.map { it.file }.sorted(),
        )
    }

    companion object {
        private fun Project.enableWebIsolatedProjectsSupportPreview() {
            propertiesExtension.set(PropertiesProvider.PropertyNames.KOTLIN_WEB_ISOLATED_PROJECTS_SUPPORT, "Preview")
        }

        private fun DependencyHandler.kotlinNpmSharedDependenciesJs(projectPath: String) {
            add("kotlinNpmSharedDependenciesJs", project(projectPath))
        }

        private fun DependencyHandler.kotlinNpmSharedDependenciesWasmJs(projectPath: String) {
            add("kotlinNpmSharedDependenciesWasmJs", project(projectPath))
        }

    }
}
