/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.targets.js.NpmVersions
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.WebpackRulesDsl.Companion.webpackRulesContainer
import org.jetbrains.kotlin.gradle.targets.js.internal.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.nodeJsRoot
import org.jetbrains.kotlin.gradle.targets.js.ir.npmToolingDir
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectModules
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRunner
import org.jetbrains.kotlin.gradle.targets.wasm.internal.isWasm
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.nodejs.nodeJsEnvSpec
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import java.io.IOException
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.readText

@CacheableTask
internal abstract class WebpackBundleKotlinJsTests
@Inject
constructor(
    @Internal
    @Transient
    final override val compilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val execOps: ExecOperations,
) : DefaultTask(), RequiresNpmDependenciesTask {

    private val npmProject = compilation.npmProject
    private val npmProjectDir: Provider<Directory> = npmProject.dir

    /** This flag prevents from installing NPM dependencies that not going to be used.
     * i.e. task is registered but wasn't requested for execution.
     * this can happen when NodeJS or Karma test framework is used
     */
    @get:Input
    internal val browserRunnersDeclared: Property<Boolean> = objects.property(false)

    init {
        onlyIf("At least one browser runner is declared in browser.test DSL") {
            browserRunnersDeclared.get()
        }

        onlyIf("Entry JS file with Kotlin tests should exist") {
            @Suppress("UNNECESSARY_SAFE_CALL")
            testsEntryFile.asFile.orNull?.exists() == true
        }
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testsEntryFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputBundleDir: DirectoryProperty

    @get:Internal
    abstract val httpServerService: Property<KotlinHttpServerForBrowserJsTests>

    @get:Internal
    val kotlinJsTestLocation: KotlinDefaultJsTestLocation
        get() {
            val httpServerService = this.httpServerService
            val uniqueBundleName = this.path
            return KotlinDefaultJsTestLocation(
                bundleLocation = outputBundleDir,
                testHtmlFileName = providers.provider { TEST_HTML_FILE_NAME },
                url = outputBundleDir.zip(httpServerService) { bundleDir, httpServer ->
                    httpServer.serve(uniqueBundleName, bundleDir).resolve(TEST_HTML_FILE_NAME)
                }
            )
        }

    @get:OutputFile
    internal val webpackConfigFile: Provider<RegularFile> =
        npmProjectDir.map { it.file("webpack-browser-test.config.js") }

    @get:Internal
    internal abstract val versions: Property<NpmVersions>

    @get:Internal
    internal abstract val npmToolingEnvDir: DirectoryProperty

    private val isWasm = compilation.isWasm

    @get:Internal
    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() {
            if (!browserRunnersDeclared.get()) return emptySet()
            val versions = versions.get()
            return setOf(
                versions.webpack,
                versions.webpackCli,
                versions.sourceMapLoader,
                versions.kotlinWebHelpers,
                // TODO: KT-86683 Add mocha as npm dependency instead of URL
            )
        }

    @TaskAction
    fun execute() {
        val npmToolingEnv = npmToolingEnvDir.getFile()
        val modules = NpmProjectModules(npmToolingEnv)

        val runnerModule = modules.require("kotlin-web-helpers/dist/kotlin-test-mocha-browser-runner.js")
        val staticHtml = modules.require("kotlin-web-helpers/dist/static/test.html")
        modules.require("playwright-core")


        val outputDir = outputBundleDir.get().asFile

        val config = KotlinWebpackConfig(
            npmProjectDir = npmProjectDir.map { it.asFile },
            mode = KotlinWebpackConfig.Mode.DEVELOPMENT,
            entry = testsEntryFile.get().asFile,
            output = KotlinWebpackOutput(
                clean = true, // cleans webpack output dir, useful for up-to-date safety
            ),
            outputPath = outputDir,
            outputFileName = "tests.bundle.js",
            rules = objects.webpackRulesContainer(),
            devtool = "source-map",
            sourceMaps = true,
            resolveLoadersFromKotlinToolingDir = isWasm,
            defineNonBrowserEnvironmentProperties = objects.propertyWithConvention(isWasm),
        )

        // FIXME: KT-86694 we don't actually need to bundle it, just fix 'format-util' bundling on kotlin-web-utils side
        config.extraJs += "config.entry.kotlinTestRunner = ${runnerModule.jsQuoted()}"
        // FIXME: KT-86683 add mocha as npm dependency
        config.extraJs += """config.externals = { "mocha": "Mocha" }"""

        if (isWasm) {
            config.experiments += listOf("asyncWebAssembly", "topLevelAwait")
        }

        val runner = KotlinWebpackRunner(
            name = name,
            npmProjectDir = npmProject.dir.get().asFile,
            nodeExecutable = npmProject.nodeExecutable,
            logger = logger,
            configFile = webpackConfigFile.get().asFile,
            tool = "webpack/bin/webpack.js",
            args = listOf("build"),
            nodeArgs = emptyList(),
            config = config,
            objects = objects,
            execOps = execOps,
            npmToolingEnvDir = npmToolingEnv,
            resolveModulesFromKotlinToolingDir = isWasm,
        )

        runner.execute()

        // Write should happen after webpack build, because of 'clean' policy
        writeTestHtmlFile(staticHtml)
    }

    private fun writeTestHtmlFile(staticHtmlPath: String) {
        val source = Path(staticHtmlPath)
        val html = try {
            source.readText(Charsets.UTF_8)
        } catch (e: IOException) {
            throw IllegalArgumentException("'$staticHtmlPath' file can't be loaded ", e)
        }

        val output = outputBundleDir.get().asFile.resolve(TEST_HTML_FILE_NAME)
        output.writeText(html)
    }

    companion object {
        private const val TEST_HTML_FILE_NAME = "test.html"
    }
}

internal fun KotlinJsIrCompilation.locateOrRegisterBrowserTestBundleTask(
    configure: WebpackBundleKotlinJsTests.() -> Unit
): TaskProvider<WebpackBundleKotlinJsTests> {
    val project = this.project
    return project.locateOrRegisterTask<WebpackBundleKotlinJsTests>(
        name = "prepareWebpackBundleForKotlinJsTests",
        args = listOf(this),
        invokeWhenRegistered = {}
    ) {
        val task = this
        val compilation = this@locateOrRegisterBrowserTestBundleTask

        val nodeJsRoot = compilation.nodeJsRoot()
        val nodeJsEnvSpec = compilation.nodeJsEnvSpec

        task.versions.value(nodeJsRoot.versions).disallowChanges()

        with(nodeJsEnvSpec) {
            task.dependsOn(project.nodeJsSetupTaskProvider)
        }

        task.dependsOn(nodeJsRoot.npmInstallTaskProvider)
        task.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })

        if (compilation.isWasm) {
            task.dependsOn((nodeJsRoot as WasmNodeJsRootExtension).toolingInstallTaskProvider)
        }
        task.npmToolingEnvDir.set(compilation.npmToolingDir())
        task.npmToolingEnvDir.disallowChanges()

        val binary = compilation.binaries.getIrBinaries(
            KotlinJsBinaryMode.DEVELOPMENT
        ).single()

        task.testsEntryFile.convention(binary.mainFileSyncPath)
        task.outputBundleDir.convention(project.layout.buildDirectory.dir("kotlinJsTest/dist"))
        val httpServerService = project.kotlinHttpServerForBrowserJsTests()
        task.usesService(httpServerService)
        task.httpServerService.convention(httpServerService)

        nodeJsRoot.taskRequirements.addTaskRequirements(this)

        configure()
    }
}
