/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModel.Companion.NO_KOTLIN_NATIVE_HOME
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.plugins.gradle.DefaultExternalDependencyId
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService
import org.jetbrains.plugins.gradle.tooling.util.DependencyResolver
import org.jetbrains.plugins.gradle.tooling.util.SourceSetCachedFinder
import org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl
import java.io.File
import java.lang.reflect.Method

class KotlinMPPGradleModelBuilder : ModelBuilderService {
    // This flag enables import of source sets which do not belong to any compilation
    private val DEFAULT_IMPORT_ORPHAN_SOURCE_SETS = true
    private val DEFAULT_BUILD_METADATA_DEPENDENCIES_FOR_ACTUALISED_SOURCE_SETS = true

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder
            .create(project, e, "Gradle import errors")
            .withDescription("Unable to build Kotlin project configuration")
    }

    override fun canBuild(modelName: String?): Boolean {
        return modelName == KotlinMPPGradleModel::class.java.name
    }

    override fun buildAll(modelName: String, project: Project): Any? {
        val projectTargets = project.getTargets() ?: return null
        val dependencyResolver = DependencyResolverImpl(
            project,
            false,
            false,
            true,
            SourceSetCachedFinder(project)
        )
        val dependencyMapper = KotlinDependencyMapper()
        val sourceSets = buildSourceSets(dependencyResolver, project, dependencyMapper) ?: return null
        val sourceSetMap = sourceSets.map { it.name to it }.toMap()
        val targets = buildTargets(projectTargets, sourceSetMap, dependencyResolver, project, dependencyMapper) ?: return null
        computeSourceSetsDeferredInfo(sourceSetMap, targets, isHMPPEnabled(project), shouldCoerceRootSourceSetToCommon(project))
        val coroutinesState = getCoroutinesState(project)
        reportUnresolvedDependencies(targets)
        val kotlinNativeHome = KotlinNativeHomeEvaluator.getKotlinNativeHome(project) ?: NO_KOTLIN_NATIVE_HOME
        return KotlinMPPGradleModelImpl(
            filterOrphanSourceSets(sourceSetMap, targets, project),
            targets,
            ExtraFeaturesImpl(coroutinesState, isHMPPEnabled(project), isNativeDependencyPropagationEnabled(project)),
            kotlinNativeHome,
            dependencyMapper.toDependencyMap()
        )
    }

    private fun filterOrphanSourceSets(
        sourceSets: Map<String, KotlinSourceSetImpl>,
        targets: Collection<KotlinTarget>,
        project: Project
    ): Map<String, KotlinSourceSetImpl> {
        if (try {
                project.properties["import_orphan_source_sets"]
            } catch (e: Exception) {
                null
            }?.toString()?.toBoolean() ?: DEFAULT_IMPORT_ORPHAN_SOURCE_SETS
        ) return sourceSets
        val compiledSourceSets: Collection<String> =
            targets.flatMap { it.compilations }.flatMap { it.sourceSets }.flatMap { it.dependsOnSourceSets.union(listOf(it.name)) }
                .distinct()
        sourceSets.filter { !compiledSourceSets.contains(it.key) }.forEach {
            logger.warn("[sync warning] Source set \"${it.key}\" is not compiled with any compilation. This source set is not imported in the IDE.")
        }
        return sourceSets.filter { compiledSourceSets.contains(it.key) }
    }

    private fun isHMPPEnabled(project: Project): Boolean {
        //TODO(auskov): replace with Project.isKotlinGranularMetadataEnabled after merging with gradle branch
        return (project.findProperty("kotlin.mpp.enableGranularSourceSetsMetadata") as? String)?.toBoolean() ?: false
    }

    private fun shouldCoerceRootSourceSetToCommon(project: Project): Boolean {
        return (project.findProperty("kotlin.mpp.coerceRootSourceSetsToCommon") as? String)?.toBoolean() ?: true
    }

    private fun isNativeDependencyPropagationEnabled(project: Project): Boolean {
        return (project.findProperty("kotlin.native.enableDependencyPropagation") as? String)?.toBoolean() ?: true
    }

    private fun reportUnresolvedDependencies(targets: Collection<KotlinTarget>) {
        targets.asSequence()
            .flatMap { it.compilations.asSequence() }
            .flatMap { it.dependencies.asSequence() }
            .mapNotNull { (it as? UnresolvedExternalDependency)?.failureMessage }
            .toSet()
            .forEach { logger.warn(it) }
    }

    private fun getCoroutinesState(project: Project): String? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getExperimental = kotlinExt.javaClass.getMethodOrNull("getExperimental") ?: return null
        val experimentalExt = getExperimental(kotlinExt) ?: return null
        val getCoroutines = experimentalExt.javaClass.getMethodOrNull("getCoroutines") ?: return null
        return getCoroutines(experimentalExt) as? String
    }


    private fun calculateDependsOnClosure(
        sourceSet: KotlinSourceSetImpl?,
        sourceSetsMap: Map<String, KotlinSourceSetImpl>,
        cache: MutableMap<String, Set<String>>
    ): Set<String> {
        return if (sourceSet == null) {
            emptySet()
        } else {
            cache[sourceSet.name] ?: sourceSet.dependsOnSourceSets.flatMap { name ->
                calculateDependsOnClosure(
                    sourceSetsMap[name],
                    sourceSetsMap,
                    cache
                ).union(setOf(name))
            }.toSet().also { cache[sourceSet.name] = it }
        }
    }

    private fun buildSourceSets(
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper
    ): Collection<KotlinSourceSetImpl>? {
        val kotlinExt = project.extensions.findByName("kotlin") ?: return null
        val getSourceSets = kotlinExt.javaClass.getMethodOrNull("getSourceSets") ?: return null

        @Suppress("UNCHECKED_CAST")
        val sourceSets =
            (getSourceSets(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList<Named>()
        val androidDeps = buildAndroidDeps(kotlinExt.javaClass.classLoader, project)

        // Some performance optimisation: do not build metadata dependencies if source set is not common
        val doBuildMetadataDependencies = try {
            project.properties["build_metadata_dependencies_for_actualised_source_sets"]?.toString()?.toBoolean()
        } catch (_: Exception) {
            null
        } ?: DEFAULT_BUILD_METADATA_DEPENDENCIES_FOR_ACTUALISED_SOURCE_SETS
        val allSourceSetsProtos = sourceSets.mapNotNull { buildSourceSet(it, dependencyResolver, project, dependencyMapper, androidDeps) }
        val allSourceSets = if (doBuildMetadataDependencies) {
            allSourceSetsProtos.map { proto -> proto.buildKotlinSourceSetImpl(true) }
        } else {
            val unactualizedSourceSets = allSourceSetsProtos.flatMap { it.dependsOnSourceSets }.distinct()
            allSourceSetsProtos.map { proto -> proto.buildKotlinSourceSetImpl(unactualizedSourceSets.contains(proto.name)) }
        }

        val map = allSourceSets.map { it.name to it }.toMap()
        val dependsOnCache = HashMap<String, Set<String>>()
        return allSourceSets.map { sourceSet ->
            KotlinSourceSetImpl(
                sourceSet.name,
                sourceSet.languageSettings,
                sourceSet.sourceDirs,
                sourceSet.resourceDirs,
                sourceSet.dependencies,
                calculateDependsOnClosure(sourceSet, map, dependsOnCache),
                sourceSet.actualPlatforms as KotlinPlatformContainerImpl,
                sourceSet.isTestModule
            )
        }
    }

    private fun buildAndroidDeps(classLoader: ClassLoader, project: Project): Map<String, List<Any>>? {
        val includeAndroidDeps = try {
            project.properties["kotlin.include.android.dependencies"]?.toString()?.toBoolean() == true
        } catch (_: Exception) {
            false
        }
        if (includeAndroidDeps) {
            try {
                val resolverClass = classLoader.loadClass("org.jetbrains.kotlin.gradle.targets.android.internal.AndroidDependencyResolver")
                val getAndroidSourceSetDependencies = resolverClass.getMethodOrNull("getAndroidSourceSetDependencies", Project::class.java)
                val resolver = resolverClass.getField("INSTANCE").get(null)
                @Suppress("UNCHECKED_CAST")
                return getAndroidSourceSetDependencies?.let { it(resolver, project) } as Map<String, List<Any>>?
            } catch (e: Exception) {
                logger.info("Unexpected exception", e)
            }
        }
        return null
    }

    private fun buildSourceSet(
        gradleSourceSet: Named,
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper,
        androidDeps: Map<String, List<Any>>?
    ): KotlinSourceSetProto? {
        val sourceSetClass = gradleSourceSet.javaClass
        val getLanguageSettings = sourceSetClass.getMethodOrNull("getLanguageSettings") ?: return null
        val getSourceDirSet = sourceSetClass.getMethodOrNull("getKotlin") ?: return null
        val getResourceDirSet = sourceSetClass.getMethodOrNull("getResources") ?: return null
        val getDependsOn = sourceSetClass.getMethodOrNull("getDependsOn") ?: return null
        val languageSettings = getLanguageSettings(gradleSourceSet)?.let { buildLanguageSettings(it) } ?: return null
        val sourceDirs = (getSourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()
        val resourceDirs = (getResourceDirSet(gradleSourceSet) as? SourceDirectorySet)?.srcDirs ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val dependsOnSourceSets = (getDependsOn(gradleSourceSet) as? Set<Named>)?.mapTo(LinkedHashSet()) { it.name } ?: emptySet<String>()

        val sourceSetDependenciesBuilder: () -> Array<KotlinDependencyId> = {
            buildSourceSetDependencies(gradleSourceSet, dependencyResolver, project, androidDeps).map { dependencyMapper.getId(it) }.distinct()
                .toTypedArray()
        }
        return KotlinSourceSetProto(
            gradleSourceSet.name,
            languageSettings,
            sourceDirs,
            resourceDirs,
            sourceSetDependenciesBuilder,
            dependsOnSourceSets
        )
    }

    private fun buildLanguageSettings(gradleLanguageSettings: Any): KotlinLanguageSettings? {
        val languageSettingsClass = gradleLanguageSettings.javaClass
        val getLanguageVersion = languageSettingsClass.getMethodOrNull("getLanguageVersion") ?: return null
        val getApiVersion = languageSettingsClass.getMethodOrNull("getApiVersion") ?: return null
        val getProgressiveMode = languageSettingsClass.getMethodOrNull("getProgressiveMode") ?: return null
        val getEnabledLanguageFeatures = languageSettingsClass.getMethodOrNull("getEnabledLanguageFeatures") ?: return null
        val getExperimentalAnnotationsInUse = languageSettingsClass.getMethodOrNull("getExperimentalAnnotationsInUse")
        val getCompilerPluginArguments = languageSettingsClass.getMethodOrNull("getCompilerPluginArguments")
        val getCompilerPluginClasspath = languageSettingsClass.getMethodOrNull("getCompilerPluginClasspath")
        val getFreeCompilerArgs = languageSettingsClass.getMethodOrNull("getFreeCompilerArgs")
        @Suppress("UNCHECKED_CAST")
        return KotlinLanguageSettingsImpl(
            getLanguageVersion(gradleLanguageSettings) as? String,
            getApiVersion(gradleLanguageSettings) as? String,
            getProgressiveMode(gradleLanguageSettings) as? Boolean ?: false,
            getEnabledLanguageFeatures(gradleLanguageSettings) as? Set<String> ?: emptySet(),
            getExperimentalAnnotationsInUse?.invoke(gradleLanguageSettings) as? Set<String> ?: emptySet(),
            (getCompilerPluginArguments?.invoke(gradleLanguageSettings) as? List<String> ?: emptyList()).toTypedArray(),
            (getCompilerPluginClasspath?.invoke(gradleLanguageSettings) as? FileCollection)?.files ?: emptySet(),
            (getFreeCompilerArgs?.invoke(gradleLanguageSettings) as? List<String>).orEmpty().toTypedArray()
        )
    }

    private fun buildDependencies(
        dependencyHolder: Any,
        dependencyResolver: DependencyResolver,
        configurationNameAccessor: String,
        scope: String,
        project: Project,
        metadataDependencyTransformationBuilder: MetadataDependencyTransformationBuilder
    ): Collection<KotlinDependency> {
        val dependencyHolderClass = dependencyHolder.javaClass
        val getConfigurationName = dependencyHolderClass.getMethodOrNull(configurationNameAccessor) ?: return emptyList()
        val configurationName = getConfigurationName(dependencyHolder) as? String ?: return emptyList()
        val configuration = project.configurations.findByName(configurationName) ?: return emptyList()
        if (!configuration.isCanBeResolved) return emptyList()

        val dependencyAdjuster =
            DependencyAdjuster(configuration, scope, project, metadataDependencyTransformationBuilder.getTransformations(configurationName))

        val resolvedDependencies = dependencyResolver
            .resolveDependencies(configuration)
            .apply {
                forEach<ExternalDependency?> { (it as? AbstractExternalDependency)?.scope = scope }
                forEach<ExternalDependency?> {
                    if (it is DefaultExternalProjectDependency && it.projectDependencyArtifacts !is ArrayList) {
                        it.projectDependencyArtifacts = ArrayList(it.projectDependencyArtifacts)
                    }
                }
            }
            .flatMap { dependencyAdjuster.adjustDependency(it) }
        val singleDependencyFiles = resolvedDependencies.mapNotNullTo(LinkedHashSet<File>()) {
            (it as? FileCollectionDependency)?.files?.singleOrNull()
        }
        // Workaround for duplicated dependencies specified as a file collection (KT-26675)
        // Drop this code when the issue is fixed in the platform
        return resolvedDependencies.filter { dependency ->
            if (dependency !is FileCollectionDependency) return@filter true
            val files = dependency.files
            if (files.size <= 1) return@filter true
            (files.any { it !in singleDependencyFiles })
        }
    }

    private fun buildTargets(
        projectTargets: Collection<Named>,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper
    ): Collection<KotlinTarget>? {
        val isHMPPEnabled = isHMPPEnabled(project)
        return projectTargets.mapNotNull { buildTarget(it, sourceSetMap, dependencyResolver, project, dependencyMapper, isHMPPEnabled) }
    }

    private operator fun Any?.get(methodName: String, vararg params: Any): Any? {
        return this[methodName, params.map { it.javaClass }, params.toList()]
    }

    private operator fun Any?.get(methodName: String, paramTypes: List<Class<*>>, params: List<Any?>): Any? {
        if (this == null) return null
        return this::class.java.getMethodOrNull(methodName, *paramTypes.toTypedArray())?.invoke(this, *params.toTypedArray())
    }

    private fun buildArtifact(
        executableName: String,
        linkTask: Task,
        runConfiguration: KonanRunConfigurationModel
    ): KonanArtifactModel? {
        val outputKind = linkTask["getOutputKind"]["name"] as? String ?: return null
        val konanTargetName = linkTask["getTarget"] as? String ?: error("No arch target found")
        val outputFile = (linkTask["getOutputFile"] as? Provider<*>)?.orNull as? File ?: return null
        val compilationTarget = linkTask["getCompilation"]["getTarget"]
        val compilationTargetName = compilationTarget["getName"] as? String ?: return null
        val isTests = linkTask["getProcessTests"] as? Boolean ?: return null

        return KonanArtifactModelImpl(
            compilationTargetName,
            executableName,
            outputKind,
            konanTargetName,
            outputFile,
            linkTask.path,
            runConfiguration,
            isTests
        )
    }

    private fun konanArtifacts(target: Named): List<KonanArtifactModel> {
        val result = ArrayList<KonanArtifactModel>()

        val binaries = target["getBinaries"] as? Collection<*> ?: return result
        binaries.forEach { binary ->
            val executableName = binary["getBaseName"] as? String ?: ""
            val linkTask = binary["getLinkTask"] as? Task ?: return@forEach
            val runConfiguration = KonanRunConfigurationModelImpl(binary["getRunTask"] as? Exec)
            buildArtifact(executableName, linkTask, runConfiguration)?.let { result.add(it) }
        }

        return result
    }

    private fun buildTarget(
        gradleTarget: Named,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper,
        isHMPPEnabled: Boolean
    ): KotlinTarget? {
        val targetClass = gradleTarget.javaClass
        val getPlatformType = targetClass.getMethodOrNull("getPlatformType") ?: return null
        val getDisambiguationClassifier = targetClass.getMethodOrNull("getDisambiguationClassifier") ?: return null
        val platformId = (getPlatformType.invoke(gradleTarget) as? Named)?.name ?: return null
        val platform = KotlinPlatform.byId(platformId) ?: return null
        val useDisambiguationClassifier =
            targetClass.getMethodOrNull("getUseDisambiguationClassifierAsSourceSetNamePrefix")?.invoke(gradleTarget) as? Boolean ?: true
        val disambiguationClassifier = if (useDisambiguationClassifier)
            getDisambiguationClassifier(gradleTarget) as? String
        else {
            targetClass.getMethodOrNull("getOverrideDisambiguationClassifierOnIdeImport")?.invoke(gradleTarget) as? String
        }
        val getPreset = targetClass.getMethodOrNull("getPreset")
        val targetPresetName: String?
        targetPresetName = try {
            val targetPreset = getPreset?.invoke(gradleTarget)
            val getPresetName = targetPreset?.javaClass?.getMethodOrNull("getName")
            getPresetName?.invoke(targetPreset) as? String
        } catch (e: Throwable) {
            "${e::class.java.name}:${e.message}"
        }

        val gradleCompilations = getCompilations(gradleTarget) ?: return null
        val compilations = gradleCompilations.mapNotNull {
            val compilation = buildCompilation(it, disambiguationClassifier, sourceSetMap, dependencyResolver, project, dependencyMapper)
            if (compilation == null || platform != KotlinPlatform.ANDROID) {
                compilation
            } else {
                compilation.addDependsOnSourceSetsToCompilation(sourceSetMap, isHMPPEnabled)
            }
        }
        val jar = buildTargetJar(gradleTarget, project)
        val testRunTasks = buildTestRunTasks(project, gradleTarget)
        val nativeMainRunTasks =
            if (platform == KotlinPlatform.NATIVE) buildNativeMainRunTasks(gradleTarget)
            else emptyList()
        val artifacts = konanArtifacts(gradleTarget)
        val target = KotlinTargetImpl(
            gradleTarget.name,
            targetPresetName,
            disambiguationClassifier,
            platform,
            compilations,
            testRunTasks,
            nativeMainRunTasks,
            jar,
            artifacts
        )
        compilations.forEach {
            it.disambiguationClassifier = target.disambiguationClassifier
            it.platform = target.platform
        }
        return target
    }

    private fun KotlinCompilationImpl.addDependsOnSourceSetsToCompilation(
        sourceSetMap: Map<String, KotlinSourceSet>,
        isHMPPEnabled: Boolean
    ): KotlinCompilationImpl {
        val dependsOnSourceSets = this.sourceSets.flatMap { it.dependsOnSourceSets }.mapNotNull { sourceSetMap[it] }

        if (!isHMPPEnabled) {
            // intermediate source sets should be common if HMPP is disabled
            dependsOnSourceSets.subtract(this.sourceSets).forEach {
                it.actualPlatforms.addSimplePlatforms(listOf(KotlinPlatform.COMMON))
            }
        }

        return KotlinCompilationImpl(
            this.name,
            this.sourceSets.union(dependsOnSourceSets),
            this.dependencies,
            this.output,
            this.arguments,
            this.dependencyClasspath,
            this.kotlinTaskProperties,
            this.nativeExtensions
        )
    }

    private fun buildNativeMainRunTasks(gradleTarget: Named): Collection<KotlinNativeMainRunTask> {
        val executableBinaries = (gradleTarget::class.java.getMethodOrNull("getBinaries")?.invoke(gradleTarget) as? Collection<Any>)
            ?.filter { it.javaClass.name == "org.jetbrains.kotlin.gradle.plugin.mpp.Executable" } ?: return emptyList()
        return executableBinaries.mapNotNull { binary ->
            val runTaskName = binary::class.java.getMethod("getRunTaskName").invoke(binary) as String? ?: return@mapNotNull null
            val entryPoint = binary::class.java.getMethod("getEntryPoint").invoke(binary) as String? ?: return@mapNotNull null
            val debuggable = binary::class.java.getMethod("getDebuggable").invoke(binary) as Boolean

            val compilationName = binary.javaClass.getMethodOrNull("getCompilation")?.invoke(binary)?.let {
                it.javaClass.getMethodOrNull("getCompilationName")?.invoke(it)?.toString()
            } ?: KotlinCompilation.MAIN_COMPILATION_NAME

            KotlinNativeMainRunTaskImpl(
                runTaskName,
                compilationName,
                entryPoint,
                debuggable
            )
        }
    }

    private fun buildTestRunTasks(project: Project, gradleTarget: Named): Collection<KotlinTestRunTask> {
        val getTestRunsMethod = gradleTarget.javaClass.getMethodOrNull("getTestRuns")
        if (getTestRunsMethod != null) {
            val testRuns = getTestRunsMethod?.invoke(gradleTarget) as? Iterable<Any>
            if (testRuns != null) {
                val testReports =
                    testRuns.mapNotNull { (it.javaClass.getMethodOrNull("getExecutionTask")?.invoke(it) as? TaskProvider<Task>)?.get() }
                val testTasks = testReports.flatMap {
                    ((it.javaClass.getMethodOrNull("getTestTasks")?.invoke(it) as? Collection<Any>)?.mapNotNull {
                        when {
                            //TODO(auskov): getTestTasks should return collection of TaskProviders without mixing with Tasks
                            it is Provider<*> -> it.get() as? Task
                            it is Task -> it
                            else -> null
                        }
                    }) ?: listOf(it)
                }
                return testTasks.filter { it.enabled }.map {
                    val name = it.name
                    val compilation = it.javaClass.getMethodOrNull("getCompilation")?.invoke(it)
                    val compilationName = compilation?.javaClass?.getMethodOrNull("getCompilationName")?.invoke(compilation)?.toString()
                        ?: KotlinCompilation.TEST_COMPILATION_NAME
                    KotlinTestRunTaskImpl(name, compilationName)
                }.toList()
            }
            return emptyList()
        }

        // Otherwise, find the Kotlin test task with names matching the target name. This is a workaround that makes assumptions about
        // the tasks naming logic and is therefore an unstable and temporary solution until test runs API is implemented:
        @Suppress("UNCHECKED_CAST")
        val kotlinTestTaskClass = try {
            gradleTarget.javaClass.classLoader.loadClass("org.jetbrains.kotlin.gradle.tasks.KotlinTest") as Class<out Task>
        } catch (_: ClassNotFoundException) {
            return emptyList()
        }

        val targetDisambiguationClassifier = run {
            val getDisambiguationClassifier = gradleTarget.javaClass.getMethodOrNull("getDisambiguationClassifier")
                ?: return emptyList()

            getDisambiguationClassifier(gradleTarget) as String?
        }

        // The 'targetName' of a test task matches the target disambiguation classifier, potentially with suffix, e.g. jsBrowser
        val getTargetName = kotlinTestTaskClass.getDeclaredMethodOrNull("getTargetName") ?: return emptyList()

        val jvmTestTaskClass = try {
            gradleTarget.javaClass.classLoader.loadClass("org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest") as Class<out Task>
        } catch (_: ClassNotFoundException) {
            return emptyList()
        }
        val getJvmTargetName = jvmTestTaskClass.getDeclaredMethodOrNull("getTargetName") ?: return emptyList()


        return project.tasks.filter { kotlinTestTaskClass.isInstance(it) || jvmTestTaskClass.isInstance(it) }.mapNotNull { task ->
            val testTaskDisambiguationClassifier =
                (if (kotlinTestTaskClass.isInstance(task)) getTargetName(task) else getJvmTargetName(task)) as String?
            task.name.takeIf {
                targetDisambiguationClassifier.isNullOrEmpty() ||
                        testTaskDisambiguationClassifier != null &&
                        testTaskDisambiguationClassifier.startsWith(targetDisambiguationClassifier.orEmpty())
            }
        }.map { KotlinTestRunTaskImpl(it, KotlinCompilation.TEST_COMPILATION_NAME) }
    }

    private fun buildTargetJar(gradleTarget: Named, project: Project): KotlinTargetJar? {
        val targetClass = gradleTarget.javaClass
        val getArtifactsTaskName = targetClass.getMethodOrNull("getArtifactsTaskName") ?: return null
        val artifactsTaskName = getArtifactsTaskName(gradleTarget) as? String ?: return null
        val jarTask = project.tasks.findByName(artifactsTaskName) ?: return null
        val jarTaskClass = jarTask.javaClass
        val getArchivePath = jarTaskClass.getMethodOrNull("getArchivePath")
        val archiveFile = getArchivePath?.invoke(jarTask) as? File?
        return KotlinTargetJarImpl(archiveFile)
    }

    private fun buildCompilation(
        gradleCompilation: Named,
        classifier: String?,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper

    ): KotlinCompilationImpl? {
        val compilationClass = gradleCompilation.javaClass
        val getKotlinSourceSets = compilationClass.getMethodOrNull("getKotlinSourceSets") ?: return null

        @Suppress("UNCHECKED_CAST")
        val kotlinGradleSourceSets = (getKotlinSourceSets(gradleCompilation) as? Collection<Named>) ?: return null
        val kotlinSourceSets = kotlinGradleSourceSets.mapNotNull { sourceSetMap[it.name] }
        val compileKotlinTask = getCompileKotlinTaskName(project, gradleCompilation) ?: return null
        val output = buildCompilationOutput(gradleCompilation, compileKotlinTask) ?: return null
        val arguments = buildCompilationArguments(compileKotlinTask) ?: return null
        val dependencyClasspath = buildDependencyClasspath(compileKotlinTask)
        val dependencies =
            buildCompilationDependencies(gradleCompilation, classifier, sourceSetMap, dependencyResolver, project, dependencyMapper)
        val kotlinTaskProperties = getKotlinTaskProperties(compileKotlinTask, classifier)

        // Get konanTarget (for native compilations only).
        val konanTarget = compilationClass.getMethodOrNull("getKonanTarget")?.let { getKonanTarget ->
            val konanTarget = getKonanTarget.invoke(gradleCompilation)
            konanTarget.javaClass.getMethodOrNull("getName")?.let {
                it.invoke(konanTarget) as? String
            }
        }
        val nativeExtensions = konanTarget?.let(::KotlinNativeCompilationExtensionsImpl)

        return KotlinCompilationImpl(
            gradleCompilation.name,
            kotlinSourceSets,
            dependencies.map { dependencyMapper.getId(it) }.distinct().toTypedArray(),
            output,
            arguments,
            dependencyClasspath.toTypedArray(),
            kotlinTaskProperties,
            nativeExtensions
        )
    }


    /**
     * Returns only those dependencies with RUNTIME scope which are not present with compile scope
     */
    private fun Collection<KotlinDependency>.onlyNewDependencies(compileDependencies: Collection<KotlinDependency>): List<KotlinDependency> {
        val compileDependencyArtefacts = compileDependencies.flatMap { (it as? ExternalProjectDependency)?.projectDependencyArtifacts ?: emptyList()  }
        return this.filter {
            if (it is ExternalProjectDependency)
                !(compileDependencyArtefacts.containsAll(it.projectDependencyArtifacts))
            else
                true
        }
    }

    private fun buildCompilationDependencies(
        gradleCompilation: Named,
        classifier: String?,
        sourceSetMap: Map<String, KotlinSourceSet>,
        dependencyResolver: DependencyResolver,
        project: Project,
        dependencyMapper: KotlinDependencyMapper
    ): Set<KotlinDependency> {
        return LinkedHashSet<KotlinDependency>().apply {
            val transformationBuilder = MetadataDependencyTransformationBuilder(gradleCompilation)
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getCompileDependencyConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleCompilation, dependencyResolver, "getRuntimeDependencyConfigurationName", "RUNTIME", project, transformationBuilder
            ).onlyNewDependencies(this)

            this += sourceSetMap[compilationFullName(
                gradleCompilation.name,
                classifier
            )]?.dependencies?.map { dependencyMapper.getDependency(it) }?.filterNotNull() ?: emptySet()
        }
    }

    private class MetadataDependencyTransformationBuilder(val sourceSet: Any) {
        val transformationsMethod = sourceSet.javaClass.getMethodOrNull("getDependenciesTransformation", String::class.java)

        class KotlinMetadataDependencyTransformation(
            val groupId: String?,
            val moduleName: String,
            val projectPath: String?,
            val allVisibleSourceSets: Set<String>,
            val useFilesForSourceSets: Map<String, Iterable<File>>
        ) {
            constructor(
                transformation: Any,
                group: Method,
                module: Method,
                projectPath: Method,
                visibleSourceSets: Method,
                useFilesForSourceSets: Method
            ) : this(
                group(transformation) as String?,
                module(transformation) as String,
                projectPath(transformation) as String?,
                visibleSourceSets(transformation) as Set<String>,
                useFilesForSourceSets(transformation) as Map<String, Iterable<File>>
            )
        }

        fun getTransformations(configurationName: String): Collection<KotlinMetadataDependencyTransformation> {
            val transformations = transformationsMethod?.invoke(sourceSet, configurationName) as? Iterable<Any> ?: return emptyList()
            val transformationClass = transformations.firstOrNull()?.javaClass
                ?: return emptyList()

            val getGroupId = transformationClass.getMethodOrNull("getGroupId") ?: return emptyList()
            val getModuleName = transformationClass.getMethodOrNull("getModuleName") ?: return emptyList()
            val getProjectPath = transformationClass.getMethodOrNull("getProjectPath") ?: return emptyList()
            val getAllVisibleSourceSets = transformationClass.getMethodOrNull("getAllVisibleSourceSets") ?: return emptyList()
            val getUseFilesForSourceSets = transformationClass.getMethodOrNull("getUseFilesForSourceSets") ?: return emptyList()

            return transformations.map { transformation ->
                KotlinMetadataDependencyTransformation(
                    transformation,
                    getGroupId,
                    getModuleName,
                    getProjectPath,
                    getAllVisibleSourceSets,
                    getUseFilesForSourceSets
                )
            }.filter { it.allVisibleSourceSets.isNotEmpty() }
        }
    }


    private fun buildSourceSetDependencies(
        gradleSourceSet: Named,
        dependencyResolver: DependencyResolver,
        project: Project,
        androidDeps: Map<String, List<Any>>?
    ): List<KotlinDependency> {
        return ArrayList<KotlinDependency>().apply {
            val transformationBuilder = MetadataDependencyTransformationBuilder(gradleSourceSet)
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getApiMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getImplementationMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getCompileOnlyMetadataConfigurationName", "COMPILE", project, transformationBuilder
            )
            this += buildDependencies(
                gradleSourceSet, dependencyResolver, "getRuntimeOnlyMetadataConfigurationName", "RUNTIME", project, transformationBuilder
            ).onlyNewDependencies(this)

            this += buildAndroidSourceSetDependencies(androidDeps, gradleSourceSet)
        }
    }

    private fun buildAndroidSourceSetDependencies(
        androidDeps: Map<String, List<Any>>?,
        gradleSourceSet: Named
    ): Collection<KotlinDependency> {
        return androidDeps?.get(gradleSourceSet.name)?.map { it ->
            @Suppress("UNCHECKED_CAST")
            val collection = it["getCollection"] as Set<File>?
            if (collection == null) {
                DefaultExternalLibraryDependency().apply {
                    (id as? DefaultExternalDependencyId)?.apply {
                        name = it["getName"] as String?
                        group = it["getGroup"] as String?
                        version = it["getVersion"] as String?
                    }
                    file = it["getJar"] as File
                    source = it["getSource"] as File?
                }
            } else {
                DefaultFileCollectionDependency(collection)
            }
        } ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun safelyGetArguments(compileKotlinTask: Task, accessor: Method?) = try {
        accessor?.invoke(compileKotlinTask) as? List<String>
    } catch (e: Exception) {
        logger.info(e.message ?: "Unexpected exception: $e", e)
        null
    } ?: emptyList()

    private fun buildCompilationArguments(compileKotlinTask: Task): KotlinCompilationArguments? {
        val compileTaskClass = compileKotlinTask.javaClass
        val getCurrentArguments = compileTaskClass.getMethodOrNull("getSerializedCompilerArguments")
        val getDefaultArguments = compileTaskClass.getMethodOrNull("getDefaultSerializedCompilerArguments")
        val currentArguments = safelyGetArguments(compileKotlinTask, getCurrentArguments)
        val defaultArguments = safelyGetArguments(compileKotlinTask, getDefaultArguments)
        return KotlinCompilationArgumentsImpl(defaultArguments.toTypedArray(), currentArguments.toTypedArray())
    }

    private fun buildDependencyClasspath(compileKotlinTask: Task): List<String> {
        val abstractKotlinCompileClass =
            compileKotlinTask.javaClass.classLoader.loadClass(AbstractKotlinGradleModelBuilder.ABSTRACT_KOTLIN_COMPILE_CLASS)
        val getCompileClasspath =
            abstractKotlinCompileClass.getDeclaredMethodOrNull("getCompileClasspath") ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return (getCompileClasspath(compileKotlinTask) as? Collection<File>)?.map { it.path } ?: emptyList()
    }

    private fun buildCompilationOutput(
        gradleCompilation: Named,
        compileKotlinTask: Task
    ): KotlinCompilationOutput? {
        val compilationClass = gradleCompilation.javaClass
        val getOutput = compilationClass.getMethodOrNull("getOutput") ?: return null
        val gradleOutput = getOutput(gradleCompilation) ?: return null
        val gradleOutputClass = gradleOutput.javaClass
        val getClassesDirs = gradleOutputClass.getMethodOrNull("getClassesDirs") ?: return null
        val getResourcesDir = gradleOutputClass.getMethodOrNull("getResourcesDir") ?: return null
        val compileKotlinTaskClass = compileKotlinTask.javaClass
        val getDestinationDir = compileKotlinTaskClass.getMethodOrNull("getDestinationDir")
        val getOutputFile = compileKotlinTaskClass.getMethodOrNull("getOutputFile")
        val classesDirs = getClassesDirs(gradleOutput) as? FileCollection ?: return null
        val resourcesDir = getResourcesDir(gradleOutput) as? File ?: return null
        @Suppress("UNCHECKED_CAST") val destinationDir =
            getDestinationDir?.invoke(compileKotlinTask) as? File
            //TODO: Hack for KotlinNativeCompile
                ?: (getOutputFile?.invoke(compileKotlinTask) as? Property<File>)?.orNull?.parentFile
                ?: return null
        return KotlinCompilationOutputImpl(classesDirs.files, destinationDir, resourcesDir)
    }

    private fun computeSourceSetsDeferredInfo(
        sourceSets: Map<String, KotlinSourceSetImpl>,
        targets: Collection<KotlinTarget>,
        isHMPPEnabled: Boolean,
        coerceRootSourceSetsToCommon: Boolean
    ) {
        // includes only compilations where source set is listed
        val compiledSourceSetToCompilations = LinkedHashMap<KotlinSourceSet, MutableSet<KotlinCompilation>>()
        // includes compilations where source set is included via dependsOn
        val allSourceSetToCompilations = LinkedHashMap<KotlinSourceSet, MutableSet<KotlinCompilation>>()
        for (target in targets) {
            for (compilation in target.compilations) {
                for (sourceSet in compilation.sourceSets) {
                    compiledSourceSetToCompilations.getOrPut(sourceSet) { LinkedHashSet() } += compilation
                    allSourceSetToCompilations.getOrPut(sourceSet) { LinkedHashSet() } += compilation
                    sourceSet.dependsOnSourceSets.mapNotNull { sourceSets[it] }.forEach {
                        allSourceSetToCompilations.getOrPut(it) { LinkedHashSet() } += compilation
                    }
                }
            }
        }

        for (sourceSet in sourceSets.values) {
            if (!isHMPPEnabled) {
                val name = sourceSet.name
                if (name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
                    sourceSet.isTestModule = false
                    continue
                }
                if (name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME) {
                    sourceSet.isTestModule = true
                    continue
                }
            }

            (allSourceSetToCompilations[sourceSet]?.all { it.isTestModule }
                ?: if (!isHMPPEnabled && sourceSet.name == KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME) true else null)?.let { isTest ->
                sourceSet.isTestModule = isTest
            }
            (if (isHMPPEnabled) allSourceSetToCompilations[sourceSet] else compiledSourceSetToCompilations[sourceSet])?.let { compilations ->
                val platforms = compilations.map { it.platform }
                sourceSet.actualPlatforms.addSimplePlatforms(platforms)
            }

            if (sourceSet.shouldCoerceToCommon(isHMPPEnabled, coerceRootSourceSetsToCommon)) {
                sourceSet.actualPlatforms.addSimplePlatforms(listOf(KotlinPlatform.COMMON))
            }
        }
    }

    private fun KotlinSourceSetImpl.shouldCoerceToCommon(isHMPPEnabled: Boolean, coerceRootSourceSetsToCommon: Boolean): Boolean {
        val isRoot = name == COMMON_MAIN_SOURCE_SET_NAME || name == COMMON_TEST_SOURCE_SET_NAME

        // never makes sense to coerce single-targeted source-sets
        if (actualPlatforms.platforms.size == 1) return false

        return when {
            // pre-HMPP has only single-targeted source sets and COMMON
            !isHMPPEnabled -> true

            // in HMPP, we might want to coerce source sets to common, but only root ones, and only
            // when the corresponding setting is turned on
            isHMPPEnabled && isRoot && coerceRootSourceSetsToCommon -> true

            // in all other cases, in HMPP we shouldn't coerce anything
            else -> false
        }
    }

    private class DependencyAdjuster(
        private val configuration: Configuration,
        private val scope: String,
        private val project: Project,
        transformations: Collection<MetadataDependencyTransformationBuilder.KotlinMetadataDependencyTransformation>
    ) {
        private val adjustmentMap = HashMap<ExternalDependency, List<ExternalDependency>>()

        private val EXTRA_DEFAULT_CONFIGURATION_NAMES = listOf("metadataApiElements")

        private val projectDependencyTransformation =
            transformations.filter { it.projectPath != null }.associateBy { it.projectPath }

        val dependenciesByProjectPath by lazy {
            configuration
                .resolvedConfiguration
                .lenientConfiguration
                .allModuleDependencies
                .mapNotNull { dependency ->
                    val artifact = dependency.moduleArtifacts.firstOrNull {
                        it.id.componentIdentifier is ProjectComponentIdentifier
                    } ?: return@mapNotNull null
                    dependency to artifact
                }
                .groupBy { (it.second.id.componentIdentifier as ProjectComponentIdentifier).projectPath }
        }

        private fun wrapDependency(dependency: ExternalProjectDependency, newConfigurationName: String): ExternalProjectDependency {
            return DefaultExternalProjectDependency(dependency).apply {
                this.configurationName = newConfigurationName

                val nestedDependencies = this.dependencies.flatMap { adjustDependency(it) }
                this.dependencies.clear()
                this.dependencies.addAll(nestedDependencies)
            }
        }

        private val libraryDependencyTransformation =
            transformations.filter { it.projectPath == null }.associateBy { it.groupId to it.moduleName }

        private fun adjustLibraryDependency(dependency: ExternalDependency, parentScope: String? = null): List<ExternalDependency> =
            when (dependency) {
                is ExternalLibraryDependency -> {
                    val replaceFiles = libraryDependencyTransformation[dependency.id.group to dependency.id.name]?.useFilesForSourceSets
                    when {
                        replaceFiles != null -> replaceFiles.flatMap { (sourceSetName, replaceFiles) ->
                            replaceFiles.map { replaceFile ->
                                DefaultExternalLibraryDependency(dependency).apply {
                                    // Transitive dependencies don't have their scope set properly; TODO investigate may be IJ bug?
                                    scope = dependency.scope ?: parentScope

                                    classifier = sourceSetName
                                    file = replaceFile

                                    val adjustedDependencies =
                                        dependency.dependencies.flatMap { adjustDependency(it, dependency.scope ?: parentScope) }

                                    dependencies.clear()
                                    dependencies.addAll(adjustedDependencies)
                                }
                            }
                        }
                        else ->
                            listOf(
                                // Do nothing but set the correct scope for this dependency if needed and adjust recursively:
                                DefaultExternalLibraryDependency(dependency).apply {
                                    scope = dependency.scope ?: parentScope

                                    val adjustedDependencies =
                                        dependency.dependencies.flatMap { adjustDependency(it, dependency.scope ?: parentScope) }

                                    dependencies.clear()
                                    dependencies.addAll(adjustedDependencies)
                                }
                            )
                    }
                }
                else -> listOf(dependency)
            }

        fun adjustDependency(dependency: ExternalDependency, parentScope: String? = null): List<ExternalDependency> {
            return adjustmentMap.getOrPut(dependency) {
                if (dependency !is ExternalProjectDependency)
                    return@getOrPut adjustLibraryDependency(dependency, parentScope)
                if (dependency.configurationName != Dependency.DEFAULT_CONFIGURATION &&
                    !EXTRA_DEFAULT_CONFIGURATION_NAMES.contains(dependency.configurationName))
                    return@getOrPut listOf(dependency)
                val artifacts = dependenciesByProjectPath[dependency.projectPath] ?: return@getOrPut listOf(dependency)
                val artifactConfiguration = artifacts.mapTo(LinkedHashSet()) {
                    it.first.configuration
                }.singleOrNull() ?: return@getOrPut listOf(dependency)
                val taskGetterName = when (scope) {
                    "COMPILE" -> "getApiElementsConfigurationName"
                    "RUNTIME" -> "getRuntimeElementsConfigurationName"
                    else -> return@getOrPut listOf(dependency)
                }
                val dependencyProject =
                    if (project.rootProject.path == dependency.projectPath)
                        project.rootProject
                    else
                        project.rootProject.getChildProjectByPath(dependency.projectPath)

                val targets = dependencyProject?.getTargets() ?: return@getOrPut listOf(dependency)
                val gradleTarget = targets.firstOrNull {
                    val getter = it.javaClass.getMethodOrNull(taskGetterName) ?: return@firstOrNull false
                    getter(it) == artifactConfiguration
                } ?: return@getOrPut listOf(dependency)
                val classifier = gradleTarget.javaClass.getMethodOrNull("getDisambiguationClassifier")?.invoke(gradleTarget) as? String
                    ?: return@getOrPut listOf(dependency)
                val platformDependency = if (classifier != KotlinTarget.METADATA_TARGET_NAME) {
                    wrapDependency(dependency, compilationFullName(KotlinCompilation.MAIN_COMPILATION_NAME, classifier))
                } else null
                val commonDependencies = if (dependencyProject.path in projectDependencyTransformation) {
                    val visibleSourceSets = projectDependencyTransformation.getValue(dependencyProject.path).allVisibleSourceSets
                    visibleSourceSets.map { sourceSetName -> wrapDependency(dependency, sourceSetName) }
                } else {
                    listOf(wrapDependency(dependency, KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME))
                }
                return if (platformDependency != null) listOf(platformDependency) + commonDependencies else commonDependencies
            }
        }
    }

    companion object {
        private val logger = Logging.getLogger(KotlinMPPGradleModelBuilder::class.java)

        fun Project.getTargets(): Collection<Named>? {
            val kotlinExt = project.extensions.findByName("kotlin") ?: return null
            val getTargets = kotlinExt.javaClass.getMethodOrNull("getTargets") ?: return null
            @Suppress("UNCHECKED_CAST")
            return (getTargets.invoke(kotlinExt) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList()
        }

        fun getCompilations(target: Named): Collection<Named>? {
            val getCompilationsMethod = target.javaClass.getMethodOrNull("getCompilations") ?: return null
            @Suppress("UNCHECKED_CAST")
            return (getCompilationsMethod.invoke(target) as? NamedDomainObjectContainer<Named>)?.asMap?.values ?: emptyList()
        }

        fun getCompileKotlinTaskName(project: Project, compilation: Named): Task? {
            val compilationClass = compilation.javaClass
            val getCompileKotlinTaskName = compilationClass.getMethodOrNull("getCompileKotlinTaskName") ?: return null

            @Suppress("UNCHECKED_CAST")
            val compileKotlinTaskName = (getCompileKotlinTaskName(compilation) as? String) ?: return null
            return project.tasks.findByName(compileKotlinTaskName) ?: return null
        }
    }
}

private fun Project.getChildProjectByPath(path: String): Project? {
    var project = this
    for (name in path.split(":").asSequence().drop(1)) {
        project = project.childProjects[name] ?: return null
    }
    return project
}

