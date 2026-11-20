/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExternalToolchainDsl::class)

package org.jetbrains.kotlin.gradle.toolchains.binaryen

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames
import org.jetbrains.kotlin.gradle.dsl.toolchain.ExternalToolchainPlatform
import org.jetbrains.kotlin.gradle.toolchains.detectExternalToolchainPlatform
import org.jetbrains.kotlin.gradle.toolchains.executableName
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.propertiesExtension
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BinaryenToolchainManagementDslTest {
    @Test
    fun `request version helper validates the Binaryen release string`() {
        val request = buildProject().objects.newInstance(BinaryenToolchainRequest::class.java)
        assertFailsWith<IllegalArgumentException> { request.version(" ") }
        request.version("133")
        assertEquals("133", request.version.get())
    }

    @Test
    fun `default DSL selects the download service and configures its parameters`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService {
            downloadBaseUrl.set("https://example.com/binaryen")
            offline.set(true)
        }

        val service = assertIs<DefaultBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())
        assertEquals("https://example.com/binaryen", service.parameters.downloadBaseUrl.get())
        assertTrue(service.parameters.offline.get())
        assertTrue(service.parameters.installationDir.isPresent)

        // Merely creating a request provider must not provision or validate a distribution.
        service.request(project.objects.newInstance(BinaryenToolchainRequest::class.java))
    }

    @Test
    fun `default service interface maps to its implementation`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(DefaultBinaryenToolchainService::class) {
            downloadBaseUrl.set("https://example.com/binaryen")
        }

        assertIs<DefaultBinaryenToolchainServiceImpl>(project.locateOrRegisterBinaryenToolchainService().get())
    }

    @Test
    fun `preinstalled service resolves wasm-opt from the distribution without requiring request metadata`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(PreInstalledBinaryenToolchainService::class) {
            toolchainDir.set(project.layout.projectDirectory.dir("binaryen"))
        }
        val service = assertIs<PreInstalledBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())

        val result = service.request(project.objects.newInstance(BinaryenToolchainRequest::class.java)).get()

        assertEquals(project.expectedWasmOpt(project.projectDir.resolve("binaryen")), result.wasmOpt)
    }

    @Test
    fun `preinstalled service requires an explicit distribution directory`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(PreInstalledBinaryenToolchainService::class) { }
        val service = assertIs<PreInstalledBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())

        assertFalse(service.parameters.toolchainDir.isPresent)
        assertFails { service.request(project.objects.newInstance(BinaryenToolchainRequest::class.java)).get() }
    }

    @Test
    fun `Gradle properties configure the download service when no settings service exists`() {
        val project = buildProject {
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "DOWNLOAD")
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_DOWNLOAD_URL, "https://example.com/properties")
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_INSTALLATION_DIR, projectDir.resolve("binaryen").path)
        }

        val service = assertIs<DefaultBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())

        assertEquals("https://example.com/properties", service.parameters.downloadBaseUrl.get())
        assertEquals(project.projectDir.resolve("binaryen"), service.parameters.installationDir.get().asFile)
    }

    @Test
    fun `settings service wins over Gradle properties`() {
        val project = buildProject {
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "DOWNLOAD")
        }
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(PreInstalledBinaryenToolchainService::class) {
            toolchainDir.set(project.layout.projectDirectory.dir("custom-binaryen"))
        }

        val service = assertIs<PreInstalledBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())
        assertEquals(project.projectDir.resolve("custom-binaryen"), service.parameters.toolchainDir.get().asFile)
        assertSame(service, project.locateOrRegisterBinaryenToolchainService().get())
    }

    @Test
    fun `Gradle properties select the preinstalled distribution directory`() {
        val project = buildProject {
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "PREINSTALLED")
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_PREINSTALLED_DIR, "custom-binaryen")
        }

        val service = assertIs<PreInstalledBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())
        assertEquals(project.projectDir.resolve("custom-binaryen"), service.parameters.toolchainDir.get().asFile)
        val result = service.request(project.objects.newInstance(BinaryenToolchainRequest::class.java)).get()
        assertEquals(project.expectedWasmOpt(project.projectDir.resolve("custom-binaryen")), result.wasmOpt)
    }

    @Test
    fun `preinstalled Gradle property mode requires a distribution directory`() {
        val project = buildProject {
            propertiesExtension.set(PropertyNames.KOTLIN_WASM_BINARYEN_TOOLCHAIN_MODE, "PREINSTALLED")
        }
        assertFails { project.locateOrRegisterBinaryenToolchainService().get() }
    }

    @Test
    fun `download service resolves wasm-opt from existing distributions for each requested platform`() {
        for (os in listOf("linux", "windows")) {
            val project = buildProject()
            val installations = project.projectDir.resolve("installations")
            val executableName = if (os == "windows") "wasm-opt.exe" else "wasm-opt"
            val executable = installations.resolve("binaryen-version_133-x86_64-$os/bin/$executableName")
            executable.parentFile.mkdirs()
            executable.writeText("executable")
            BinaryenToolchainManagementDslImpl(project.gradle).toolchainService {
                installationDir.fileValue(installations)
                offline.set(true)
            }
            val request = project.objects.newInstance(BinaryenToolchainRequest::class.java).apply {
                version("133")
                platform.set(ExternalToolchainPlatform(os, "x86_64"))
            }
            val service = project.locateOrRegisterBinaryenToolchainService().get()
            assertEquals(executable.toPath(), service.request(request).get().wasmOpt)
        }
    }

    private fun Project.expectedWasmOpt(directory: File): Path =
        directory.resolve("bin/${providers.detectExternalToolchainPlatform().get().executableName("wasm-opt")}").toPath()

    @Test
    fun `service is disabled by default and can be disabled through the DSL`() {
        assertIs<DisabledBinaryenToolchainService>(buildProject().locateOrRegisterBinaryenToolchainService().get())
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).disable()
        assertIs<DisabledBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())
    }

    @Test
    fun `rejects a second service selection`() {
        val dsl = BinaryenToolchainManagementDslImpl(buildProject().gradle)
        dsl.toolchainService { }

        assertFailsWith<IllegalStateException> { dsl.disable() }
    }

    @Test
    fun `custom service receives its parameters and the consumer request`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(CustomBinaryenToolchainService::class) {
            executablePrefix.set("custom-wasm-opt-")
        }
        val request = project.objects.newInstance(BinaryenToolchainRequest::class.java).apply { version("133") }
        val service = assertIs<CustomBinaryenToolchainService>(project.locateOrRegisterBinaryenToolchainService().get())

        assertEquals(Paths.get("custom-wasm-opt-133"), service.request(request).get().wasmOpt)
    }

    @Test
    fun `custom service can declare no parameters`() {
        val project = buildProject()
        BinaryenToolchainManagementDslImpl(project.gradle).toolchainService(CustomBinaryenToolchainServiceWithoutParameters::class) { }

        val service = assertIs<CustomBinaryenToolchainServiceWithoutParameters>(project.locateOrRegisterBinaryenToolchainService().get())
        assertEquals(Paths.get("custom-wasm-opt"), service.request(project.objects.newInstance(BinaryenToolchainRequest::class.java)).get().wasmOpt)
    }

    abstract class CustomBinaryenToolchainService @Inject constructor(
        private val providers: ProviderFactory,
    ) : BinaryenToolchainService<CustomBinaryenToolchainService.Parameters> {
        interface Parameters : BuildServiceParameters {
            val executablePrefix: Property<String>
        }

        override fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain> = providers.provider {
            ProvisionedBinaryenToolchain(Paths.get(parameters.executablePrefix.get() + request.version.get()))
        }
    }

    abstract class CustomBinaryenToolchainServiceWithoutParameters @Inject constructor(
        private val providers: ProviderFactory,
    ) : BinaryenToolchainService<BuildServiceParameters.None> {
        override fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain> =
            providers.provider { ProvisionedBinaryenToolchain(Paths.get("custom-wasm-opt")) }
    }
}
