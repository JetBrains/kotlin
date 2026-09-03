/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.dependsOnNpmTooling
import org.jetbrains.kotlin.gradle.targets.js.ir.nodeJsRoot
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.wasm.internal.isWasm
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.readText

@DisableCachingByDefault(because = "Performs lightweight FS and text substitution operations, not worth caching")
internal abstract class EsmBundleKotlinJsTests @Inject constructor(
    @Transient
    override val compilation: KotlinJsIrCompilation,
    private val providers: ProviderFactory,
    private val fs: FileSystemOperations,
) : DefaultTask(), RequiresNpmDependenciesTask {

    private val isWasm = compilation.isWasm

    /**
     * Directory where kotlin js/wasmsj linker produced binaries with tests in ESM format.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinLinkerOutputFiles: DirectoryProperty

    /**
     * JS-file from [kotlinLinkerOutputFiles] that should be an entry point
     */
    @get:Input
    abstract val testEntryFileName: Property<String>

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = setOf(
        compilation.nodeJsRoot().versions.kotlinWebHelpers
    )

    /**
     * Final directory where test results will be stored.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val httpServerService: Property<KotlinHttpServerForBrowserJsTests>

    @get:Internal
    val kotlinJsTestLocation: KotlinDefaultJsTestLocation
        get() {
            val httpServerService = this.httpServerService
            val uniqueBundleName = this.path
            return KotlinDefaultJsTestLocation(
                bundleLocation = outputDirectory,
                testHtmlFileName = providers.provider { "test.html" },
                url = outputDirectory.zip(httpServerService) { bundleDir, httpServer ->
                    httpServer.serve(uniqueBundleName, bundleDir).resolve("test.html")
                }
            )
        }

    @get:Internal
    internal abstract val npmToolingEnvDir: DirectoryProperty

    @TaskAction
    fun action() {
        val npmToolingEnv = npmToolingEnvDir.getFile()
        val modules = NpmProjectModules(npmToolingEnv)

        val jsTestRunnerFile = modules.require("kotlin-web-helpers/dist/kotlin-test-mocha-browser-runner.js")
        val testHtmlFileTemplate = Path(modules.require("kotlin-web-helpers/dist/static/test.html"))

        /** sync will remove old files, if they're missing in [kotlinLinkerOutputFiles] */
        fs.sync { syncSpec ->
            syncSpec.from(kotlinLinkerOutputFiles)
            syncSpec.into(outputDirectory.dir("kotlin"))
        }

        fs.sync { syncSpec ->
            syncSpec.from(jsTestRunnerFile)
            syncSpec.into(outputDirectory.dir("testFramework"))
        }

        val entryPointPlaceholder = if (isWasm) {
            "// kotlinWasmJsTestsEntry: './wasmJsTests.js',"
        } else {
            "// kotlinJsTestsEntry: './jsTests.js',"
        }
        // will be loaded by 'testFramework/kotlin-test-mocha-browser-runner.js' so one level up is required
        val entryPointEscaped = "../kotlin/${testEntryFileName.get()}".replace("'", "\'")

        val testHtmlFileContent = testHtmlFileTemplate
            .readText().replace(
                oldValue = "<script src=\"tests.bundle-kotlinTestRunner.js\"></script>",
                newValue = "<script type=\"module\" src=\"testFramework/kotlin-test-mocha-browser-runner.js\"></script>"
            ).replace(
                oldValue = entryPointPlaceholder,
                newValue = "kotlinWasmJsTestsEntry: '$entryPointEscaped',"
            ).replace(
                // tests.bundle.js is expected to be generated by Webpack bundler, for esm we use 'kotlinWasmJsTestsEntry'
                oldValue = "<script src=\"tests.bundle.js\"></script>",
                newValue = ""
            )
        outputDirectory.file("test.html").get().asFile.writeText(pinMochaCdnUrls(testHtmlFileContent))
    }
}

internal fun KotlinJsIrCompilation.locateOrRegisterEsmBundleKotlinJsTestsTask(): TaskProvider<EsmBundleKotlinJsTests> {
    val esmBundleKotlinJsTests = project.locateOrRegisterTask<EsmBundleKotlinJsTests>(
        name = disambiguateName("bundleAsEsm"), // for wasmTest name will be wasmTestBundleAsEsm
        args = listOf(this),
    ) {
        val task = this
        val npmToolingDir = npmToolingDir()
        task.dependsOnNpmTooling(compilation = this@locateOrRegisterEsmBundleKotlinJsTestsTask)

        val binary = binaries.getIrBinaries(
            KotlinJsBinaryMode.DEVELOPMENT
        ).single()

        task.npmToolingEnvDir.value(npmToolingDir).finalizeValue()
        task.kotlinLinkerOutputFiles.fileProvider(binary.linkSyncTask.flatMap { it.destinationDirectory })
        task.testEntryFileName.convention(binary.mainFileSyncPath.map { it.asFile.name })
        task.outputDirectory.set(project.layout.buildDirectory.dir("kotlin/${task.name}"))
        task.httpServerService.set(project.kotlinHttpServerForBrowserJsTests())
    }

    return esmBundleKotlinJsTests
}
