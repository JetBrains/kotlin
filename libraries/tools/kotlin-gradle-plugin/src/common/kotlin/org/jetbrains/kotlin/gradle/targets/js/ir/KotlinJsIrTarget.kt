/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication.setUpResourcesVariant
import org.jetbrains.kotlin.gradle.targets.js.JsAggregatingExecutionSource
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsReportAggregatingTestRun
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTestRunFactory
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetConfigurator.Companion.configureJsDefaultOptions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import javax.inject.Inject

abstract class KotlinJsIrTarget
@Inject
constructor(
    project: Project,
    platformType: KotlinPlatformType,
) :
    KotlinTargetWithBinaries<KotlinJsIrCompilation, KotlinJsBinaryContainer>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl,
    KotlinWasmJsTargetDsl,
    KotlinWasmWasiTargetDsl,
    KotlinJsSubTargetContainerDsl,
    KotlinWasmSubTargetContainerDsl {

    private val propertiesProvider = PropertiesProvider(project)

    override val subTargets: NamedDomainObjectContainer<IKotlinJsIrSubTarget> = project.container(
        IKotlinJsIrSubTarget::class.java
    )

    override val testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun> by lazy {
        project.container(KotlinJsReportAggregatingTestRun::class.java, KotlinJsTestRunFactory(this))
    }

    open var isMpp: Boolean? = null
        internal set

    override var wasmTargetType: KotlinWasmTargetType? = null
        internal set

    override var moduleName: String? = null
        set(value) {
            field = value
            compilations.all { compilation ->
                val compilationName = if (compilation.name != MAIN_COMPILATION_NAME) {
                    compilation.name
                } else null

                val name = sequenceOf(moduleName, compilationName)
                    .filterNotNull()
                    .joinToString("-")

                compilation.outputModuleName.set(name)
            }
        }

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)
        val usageContexts = createUsageContexts(mainCompilation).toMutableSet()

        val componentName =
            if (project.kotlinExtension is KotlinMultiplatformExtension)
                targetName
            else PRIMARY_SINGLE_COMPONENT_NAME

        usageContexts.addIfNotNull(
            createSourcesJarAndUsageContextIfPublishable(
                producingCompilation = mainCompilation,
                componentName = componentName,
                artifactNameAppendix = wasmDecamelizedDefaultNameOrNull() ?: dashSeparatedName(targetName.toLowerCaseAsciiOnly())
            )
        )

        usageContexts.addIfNotNull(
            setUpResourcesVariant(
                mainCompilation
            )
        )

        val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

        setOf(result)
    }

    override fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>,
    ): KotlinVariant {
        return super.createKotlinVariant(componentName, compilation, usageContexts).apply {
            artifactTargetName = wasmDecamelizedDefaultNameOrNull() ?: componentName
        }
    }

    override fun createUsageContexts(producingCompilation: KotlinCompilation<*>): Set<DefaultKotlinUsageContext> {
        val usageContexts = super.createUsageContexts(producingCompilation)

        if (isMpp!!) return usageContexts

        return usageContexts +
                DefaultKotlinUsageContext(
                    compilation = compilations.getByName(MAIN_COMPILATION_NAME),
                    mavenScope = KotlinUsageContext.MavenScope.COMPILE,
                    dependencyConfigurationName = commonFakeApiElementsConfigurationName,
                    overrideConfigurationArtifacts = project.setProperty { emptyList() }
                )
    }

    internal val commonFakeApiElementsConfigurationName: String
        get() = lowerCamelCaseName(
            disambiguationClassifier,
            "commonFakeApiElements"
        )

    override val binaries: KotlinJsBinaryContainer
        get() = compilations.withType(KotlinJsIrCompilation::class.java)
            .named(MAIN_COMPILATION_NAME)
            .map { it.binaries }
            .get()

    internal val configureTestSideEffect: Unit by lazy {
        val mainCompilation = compilations.matching { it.isMain() }

        compilations.matching { it.isTest() }
            .all { testCompilation ->
                val testBinaries = testCompilation.binaries.executableIrInternal(testCompilation)

                if (wasmTargetType != KotlinWasmTargetType.WASI) {
                    testBinaries.forEach { binary ->
                        binary.linkSyncTask.configure { task ->
                            mainCompilation.all {
                                task.from.from(project.tasks.named(it.processResourcesTaskName))
                            }
                        }
                    }
                }
            }
    }

    fun <T : IKotlinJsIrSubTarget> addSubTarget(type: Class<T>, configure: T.() -> Unit): T {
        val subTarget = project.objects.newInstance(type, this).also(configure)
        subTargets.add(subTarget)
        return subTarget
    }

    private val commonLazyDelegate = lazy {
        NpmResolverPlugin.apply(project)
        compilations.all { compilation ->
            compilation.binaries
                .withType(JsIrBinary::class.java)
                .all { binary ->
                    val syncTask = binary.linkSyncTask
                    val tsValidationTask = registerTypeScriptCheckTask(binary)

                    binary.linkTask.configure {

                        it.finalizedBy(syncTask)

                        if (binary.generateTs) {
                            it.finalizedBy(tsValidationTask)
                        }
                    }
                }
        }
    }

    private val commonLazy by commonLazyDelegate

    private fun registerTypeScriptCheckTask(binary: JsIrBinary): TaskProvider<TypeScriptValidationTask> {
        val linkTask = binary.linkTask
        val compilation = binary.compilation
        return project.registerTask(binary.validateGeneratedTsTaskName, listOf(compilation)) {
            it.inputDir.set(linkTask.flatMap { it.destinationDirectory })
            it.validationStrategy.set(
                when (binary.mode) {
                    KotlinJsBinaryMode.DEVELOPMENT -> propertiesProvider.jsIrGeneratedTypeScriptValidationDevStrategy
                    KotlinJsBinaryMode.PRODUCTION -> propertiesProvider.jsIrGeneratedTypeScriptValidationProdStrategy
                }
            )
        }
    }

    @Deprecated("Binaryen is enabled by default. This call is redundant.")
    override fun applyBinaryen(body: BinaryenExec.() -> Unit) {
    }

    //Browser
    private val browserLazyDelegate = lazy {
        commonLazy
        addSubTarget(KotlinBrowserJsIr::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(LibraryConfigurator(this))
            if (propertiesProvider.jsBrowserWebpack) {
                subTargetConfigurators.add(WebpackConfigurator(this))
            }
        }
    }

    override val browser: KotlinJsBrowserDsl by browserLazyDelegate

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        body(browser)
    }

    //node.js
    private val nodejsLazyDelegate = lazy {
        if (wasmTargetType != KotlinWasmTargetType.WASI) {
            commonLazy
        } else {
            NodeJsRootPlugin.apply(project.rootProject)
        }

        addSubTarget(KotlinNodeJsIr::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(LibraryConfigurator(this))
            subTargetConfigurators.add(NodeJsEnvironmentConfigurator(this))
        }
    }

    override val nodejs: KotlinJsNodeDsl by nodejsLazyDelegate

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        body(nodejs)
    }

    //d8
    private val d8LazyDelegate = lazy {
//        commonLazy
        NodeJsRootPlugin.apply(project.rootProject)

        addSubTarget(KotlinD8Ir::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(LibraryConfigurator(this))
            subTargetConfigurators.add(D8EnvironmentConfigurator(this))
        }
    }

    override val d8: KotlinWasmD8Dsl by d8LazyDelegate

    private fun KotlinJsIrSubTarget.configureSubTarget() {
        configure()
    }

    override fun d8(body: KotlinWasmD8Dsl.() -> Unit) {
        body(d8)
    }

    override fun useCommonJs() {
        compilations.configureEach { jsCompilation ->
            jsCompilation.compileTaskProvider.configure {
                compilerOptions.configureCommonJsOptions()
            }

            jsCompilation.binaries
                .withType(JsIrBinary::class.java)
                .configureEach {
                    it.linkTask.configure { linkTask ->
                        linkTask.compilerOptions.configureCommonJsOptions()
                    }
                }
        }
    }

    override fun useEsModules() {
        compilations.configureEach { jsCompilation ->
            // Here it is essential to configure compilation compiler options as npm queries
            // compilation fileExtension before any task configuration action is done
            @Suppress("DEPRECATION")
            jsCompilation.compilerOptions.options.configureEsModulesOptions()

            jsCompilation.binaries
                .withType(JsIrBinary::class.java)
                .configureEach {
                    it.linkTask.configure { linkTask ->
                        linkTask.compilerOptions.configureEsModulesOptions()
                    }
                }
        }

    }

    @ExperimentalMainFunctionArgumentsDsl
    override fun passAsArgumentToMainFunction(jsExpression: String) {
        compilations
            .all {
                it.binaries
                    .withType(JsIrBinary::class.java)
                    .all {
                        it.linkTask.configure { linkTask ->
                            linkTask.compilerOptions.freeCompilerArgs.add("-Xplatform-arguments-in-main-function=$jsExpression")
                        }
                    }
            }
    }

    private fun KotlinJsCompilerOptions.configureCommonJsOptions() {
        moduleKind.convention(JsModuleKind.MODULE_COMMONJS)
        sourceMap.convention(true)
        sourceMapEmbedSources.convention(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_NEVER)
    }

    private fun KotlinJsCompilerOptions.configureEsModulesOptions() {
        moduleKind.convention(JsModuleKind.MODULE_ES)
        sourceMap.convention(true)
        sourceMapEmbedSources.convention(JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_NEVER)
    }

    override fun generateTypeScriptDefinitions() {
        compilations
            .all {
                it.binaries
                    .withType(JsIrBinary::class.java)
                    .all {
                        it.generateTs = true
                        it.linkTask.configure { linkTask ->
                            linkTask.compilerOptions.freeCompilerArgs.add(GENERATE_D_TS)
                        }
                    }
            }
    }

    @ExperimentalKotlinGradlePluginApi
    override val compilerOptions: KotlinJsCompilerOptions = project.objects
        .newInstance<KotlinJsCompilerOptionsDefault>()
        .apply {
            configureJsDefaultOptions()
        }
}

fun KotlinJsIrTarget.wasmDecamelizedDefaultNameOrNull(): String? = if (platformType == KotlinPlatformType.wasm) {
    val defaultWasmTargetName = wasmTargetType?.let {
        KotlinWasmTargetPreset.WASM_PRESET_NAME + it.name.toLowerCaseAsciiOnly().capitalizeAsciiOnly()
    }

    defaultWasmTargetName
        ?.takeIf {
            targetName == defaultWasmTargetName
        }?.decamelize()
} else null
