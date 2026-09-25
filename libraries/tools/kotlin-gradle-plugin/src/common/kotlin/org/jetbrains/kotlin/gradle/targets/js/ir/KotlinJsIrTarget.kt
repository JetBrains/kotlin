/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KotlinTargetWithKotlinArchiveSupport
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.publication.setUpResourcesVariant
import org.jetbrains.kotlin.gradle.targets.js.*
import org.jetbrains.kotlin.gradle.targets.js.dsl.*
import org.jetbrains.kotlin.gradle.targets.js.internal.jsToolingProject
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetConfigurator.Companion.configureJsDefaultOptions
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin
import org.jetbrains.kotlin.gradle.targets.js.typescript.KotlinJsDtsGenerationTask
import org.jetbrains.kotlin.gradle.targets.js.typescript.TypeScriptValidationTask
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmResolverPlugin
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import javax.inject.Inject
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension as wasmKotlinNodeJsRootExtension

internal fun ObjectFactory.KotlinJsIrTarget(
    project: Project,
    platformType: KotlinPlatformType,
): KotlinJsIrTarget = newInstance(project, platformType)

abstract class KotlinJsIrTarget
@Inject
internal constructor(
    project: Project,
    platformType: KotlinPlatformType,
) :
    KotlinTargetWithBinaries<KotlinJsIrCompilation, KotlinJsBinaryContainer>(project, platformType),
    KotlinTargetWithTests<JsAggregatingExecutionSource, KotlinJsReportAggregatingTestRun>,
    KotlinJsTargetDsl,
    KotlinWasmTargetDsl,
    KotlinJsSubTargetContainerDsl,
    KotlinTargetWithKotlinArchiveSupport {

    @InternalKotlinGradlePluginApi
    override val isStoredInKotlinArchive: Provider<Boolean> =
        project.multiplatformExtension.publishing.publicationFormat.map { it == KotlinPublicationFormat.KOTLIN_ARCHIVE }

    @InternalKotlinGradlePluginApi
    override val platformNameInKotlinArchive: String
        get() = targetPreset?.name ?: error("Name in kotlin archive in unknown for $targetName")

    @Deprecated(
        "Creating new KotlinJsIrTarget instances outside of Kotlin Gradle plugin is deprecated. Scheduled for removal in Kotlin 2.7.",
        level = DeprecationLevel.ERROR,
    )
    constructor(
        project: Project,
        platformType: KotlinPlatformType,
        @Suppress("UNUSED_PARAMETER")
        isMpp: Boolean,
    ) : this(project, platformType)

    private val propertiesProvider = PropertiesProvider(project)
    internal val shouldGenerateTypeScriptDefinitions: Property<Boolean> = project.objects.property<Boolean>(false)

    override val subTargets: NamedDomainObjectContainer<KotlinJsIrSubTargetWithBinary> = project.objects.domainObjectContainer(
        KotlinJsIrSubTargetWithBinary::class.java
    )

    override val testRuns: NamedDomainObjectContainer<KotlinJsReportAggregatingTestRun> by lazy {
        project.objects.domainObjectContainer(KotlinJsReportAggregatingTestRun::class.java, KotlinJsTestRunFactory(this))
    }

    override var wasmTargetType: KotlinWasmTargetType? = null
        internal set

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

    internal fun <T : KotlinJsIrSubTargetWithBinary> addSubTarget(type: Class<T>, configure: T.() -> Unit): T {
        val subTarget = project.objects.newInstance(type, this).also(configure)
        subTargets.add(subTarget)
        return subTarget
    }

    private val commonLazyDelegate = lazy {
        webTargetVariant(
            { NpmResolverPlugin.apply(project) },
            { WasmNpmResolverPlugin.apply(project) },
        )
        compilations.all { compilation ->
            compilation.binaries
                .withType(JsIrBinary::class.java)
                .matching { it.target.wasmTargetType == null }
                .all { binary ->
                    val syncTask = binary.linkSyncTask

                    binary.linkTask.configure {
                        it.finalizedBy(syncTask)
                    }
                }
        }
    }

    private val commonLazy by commonLazyDelegate

    private fun registerTypeScriptCheckTask(
        binary: JsIrBinary,
        inputDirectory: Provider<Directory>,
    ): TaskProvider<TypeScriptValidationTask> {
        val compilation = binary.compilation
        return project.registerTask(binary.validateGeneratedTsTaskName, listOf(compilation)) {
            it.versions.value(
                compilation.webTargetVariant(
                    { project.jsToolingProject().kotlinNodeJsRootExtension.versions },
                    { project.jsToolingProject().wasmKotlinNodeJsRootExtension.versions },
                )
            ).disallowChanges()
            it.inputDir.set(inputDirectory)
            it.validationStrategy.set(
                when (binary.mode) {
                    KotlinJsBinaryMode.DEVELOPMENT -> propertiesProvider.jsIrGeneratedTypeScriptValidationDevStrategy
                    KotlinJsBinaryMode.PRODUCTION -> propertiesProvider.jsIrGeneratedTypeScriptValidationProdStrategy
                }
            )
        }
    }

    internal open fun KotlinBrowserJsIr.bundleConfigurator() {
        subTargetConfigurators.add(WebpackConfigurator(this))
    }

    //region Browser
    private val browserLazyDelegate = lazy {
        commonLazy
        addSubTarget(KotlinBrowserJsIr::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(SwcConfigurator(this))
            subTargetConfigurators.add(LibraryConfigurator(this))
            bundleConfigurator()
        }
    }

    override val browser: KotlinJsBrowserDsl by browserLazyDelegate

    override fun browser(body: KotlinJsBrowserDsl.() -> Unit) {
        body(browser)
    }
    //endregion

    //region node.js
    private val nodejsLazyDelegate = lazy {
        if (wasmTargetType != KotlinWasmTargetType.WASI) {
            commonLazy
        } else {
            WasmNodeJsPlugin.apply(project)
            WasmNodeJsRootPlugin.apply(project.jsToolingProject())
        }

        addSubTarget(KotlinNodeJsIr::class.java) {
            configureSubTarget()
            subTargetConfigurators.add(SwcConfigurator(this))
            subTargetConfigurators.add(LibraryConfigurator(this))
            subTargetConfigurators.add(NodeJsEnvironmentConfigurator(this))
        }
    }

    override val nodejs: KotlinJsNodeDsl by nodejsLazyDelegate

    override fun nodejs(body: KotlinJsNodeDsl.() -> Unit) {
        body(nodejs)
    }
    //endregion

    internal fun KotlinJsIrSubTarget.configureSubTarget() {
        configure()
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
        shouldGenerateTypeScriptDefinitions.set(true)
        compilations
            .all {
                it.binaries
                    .withType(JsIrBinary::class.java)
                    .all { binary ->
                        if (propertiesProvider.jsRichTypeScriptGenerator && binary.target.wasmTargetType == null) {
                            val dtsTask = registerDtsGenerationTask(binary)
                            binary.dtsGenerationTask = dtsTask

                            val tsValidationTask = registerTypeScriptCheckTask(
                                binary,
                                dtsTask.flatMap { it.outputDirectory },
                            )
                            tsValidationTask.configure { it.mustRunAfter(binary.linkSyncTask) }
                            dtsTask.configure { it.finalizedBy(tsValidationTask) }
                            binary.linkSyncTask.configure { it.from.from(dtsTask) }
                        } else {
                            val tsValidationTask = registerTypeScriptCheckTask(
                                binary,
                                binary.linkTask.flatMap { it.destinationDirectory },
                            )
                            binary.linkTask.configure { linkTask ->
                                linkTask.compilerOptions.freeCompilerArgs.add(GENERATE_D_TS)
                                linkTask.finalizedBy(tsValidationTask)
                            }
                        }
                    }
            }
    }

    private fun registerDtsGenerationTask(binary: JsIrBinary): TaskProvider<KotlinJsDtsGenerationTask> {
        val linkTask = binary.linkTask
        val configurations = project.configurations

        val resultTask = project.registerTask<KotlinJsDtsGenerationTask>(
            lowerCamelCaseName(
                binary.compilation.target.disambiguationClassifier,
                binary.compilation.name.takeIf { it != MAIN_COMPILATION_NAME },
                binary.name,
                KotlinJsDtsGenerationTask.NAME,
            ),
        ) { task ->
            task.klibs.from(linkTask.map { it.libraries })
            task.entryModule.set(linkTask.flatMap { it.entryModule })
            task.granularity.set(linkTask.map { it.outputGranularity })
            task.kotlinBuildToolsApiClasspath.from(configurations.named(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME))

            task.outputDirectory.set(binary.outputDirBase.map { it.dir(KotlinJsDtsGenerationTask.OUTPUT_DIRECTORY_NAME) })
        }

        linkTask.configure { link ->
            val dtsTask = resultTask.get()
            KotlinJsCompilerOptionsHelper.syncOptionsAsConvention(
                link.compilerOptions,
                dtsTask.linkCompilerOptions,
            )
        }

        return resultTask
    }

    override val compilerOptions: KotlinJsCompilerOptions = project.objects
        .newInstance<KotlinJsCompilerOptionsDefault>()
        .apply {
            configureJsDefaultOptions()
        }

    internal companion object {
        private val DECAMELIZE_REGEX = "([A-Z])".toRegex()

        internal fun buildNpmProjectName(
            project: Project,
            targetName: String,
            defaultTargetName: String,
        ): String {
            return buildString {
                if (project.isRootProject()) {
                    append(project.rootProjectName())
                } else {
                    append(project.rootProjectName().replace(":", "-"))
                    append(project.path.replace(":", "-"))
                }

                if (targetName.isNotEmpty() && targetName != defaultTargetName) {
                    append("-")
                    append(
                        targetName
                            .replace(DECAMELIZE_REGEX) {
                                it.groupValues
                                    .drop(1)
                                    .joinToString(prefix = "-", separator = "-")
                            }
                            .toLowerCaseAsciiOnly()
                    )
                }
            }
        }
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
