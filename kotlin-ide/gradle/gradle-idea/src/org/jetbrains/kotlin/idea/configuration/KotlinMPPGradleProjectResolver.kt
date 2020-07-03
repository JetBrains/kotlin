/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.normalizePath
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.toCanonicalPath
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.PathUtilRt
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import com.intellij.util.text.VersionComparatorUtil
import org.gradle.tooling.model.UnsupportedMethodException
import org.gradle.tooling.model.idea.IdeaContentRoot
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.config.ExternalSystemNativeMainRunTask
import org.jetbrains.kotlin.config.ExternalSystemRunTask
import org.jetbrains.kotlin.config.ExternalSystemTestRunTask
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.idea.configuration.GradlePropertiesFileFacade.Companion.KOTLIN_NOT_IMPORTED_COMMON_SOURCE_SETS_SETTING
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibrariesDependencySubstitutor
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibrariesFixer
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE
import org.jetbrains.kotlin.idea.platform.IdePlatformKindTooling
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.plugins.gradle.model.*
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.CONFIGURATION_ARTIFACTS
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.MODULES_OUTPUTS
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.buildDependencies
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getModuleId
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.lang.reflect.Proxy
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap

@Order(ExternalSystemConstants.UNORDERED + 1)
open class KotlinMPPGradleProjectResolver : AbstractProjectResolverExtensionCompat() {
    override fun getToolingExtensionsClasses(): Set<Class<out Any>> {
        return setOf(KotlinMPPGradleModelBuilder::class.java, Unit::class.java)
    }

    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinMPPGradleModel::class.java)
    }

    override fun initializeModuleNode(
        gradleModule: IdeaModule,
        moduleDataNode: DataNode<ModuleData>,
        projectDataNode: DataNode<ProjectData>
    ) {
        initializeModuleData(gradleModule, moduleDataNode, projectDataNode, resolverCtx)
    }

    override fun getExtraCommandLineArgs(): List<String> {
        return if (!androidPluginPresent) listOf("-Pkotlin.include.android.dependencies=true") else emptyList()
    }

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        if (ExternalSystemApiUtil.find(ideModule, BuildScriptClasspathData.KEY) == null) {
            val buildScriptClasspathData = buildClasspathData(gradleModule, resolverCtx)
            ideModule.createChild(BuildScriptClasspathData.KEY, buildScriptClasspathData)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java)
        if (mppModel == null) {
            return super.populateModuleContentRoots(gradleModule, ideModule)
        } else {
            if (!nativeDebugAdvertised && mppModel.kotlinNativeHome.isNotEmpty()) {
                nativeDebugAdvertised = true
                suggestNativeDebug(resolverCtx.projectPath)
            }
        }

        populateContentRoots(gradleModule, ideModule, resolverCtx)
    }

    override fun populateModuleCompileOutputSettings(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        if (resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java) == null) {
            super.populateModuleCompileOutputSettings(gradleModule, ideModule)
        }

        val mppModel = resolverCtx.getMppModel(gradleModule) ?: return
        val ideaOutDir = File(ideModule.data.linkedExternalProjectPath, "out")
        val projectDataNode = ideModule.getDataNode(ProjectKeys.PROJECT)!!
        val moduleOutputsMap = projectDataNode.getUserData(MODULES_OUTPUTS)!!
        val outputDirs = HashSet<String>()
        processCompilations(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, compilation ->
            var gradleOutputMap = dataNode.getUserData(GradleProjectResolver.GRADLE_OUTPUTS)
            if (gradleOutputMap == null) {
                gradleOutputMap = MultiMap.create()
                dataNode.putUserData(GradleProjectResolver.GRADLE_OUTPUTS, gradleOutputMap)
            }

            val moduleData = dataNode.data

            with(compilation.output) {
                effectiveClassesDir?.let {
                    moduleData.isInheritProjectCompileOutputPath = false
                    moduleData.setCompileOutputPath(compilation.sourceType, it.absolutePath)
                    for (gradleOutputDir in classesDirs) {
                        recordOutputDir(gradleOutputDir, it, compilation.sourceType, moduleData, moduleOutputsMap, gradleOutputMap)
                    }
                }
                resourcesDir?.let {
                    moduleData.setCompileOutputPath(compilation.resourceType, it.absolutePath)
                    recordOutputDir(it, it, compilation.resourceType, moduleData, moduleOutputsMap, gradleOutputMap)
                }
            }

            dataNode.createChild(KotlinOutputPathsData.KEY, KotlinOutputPathsData(gradleOutputMap.copy()))
        }
        if (outputDirs.any { FileUtil.isAncestor(ideaOutDir, File(it), false) }) {
            excludeOutDir(ideModule, ideaOutDir)
        }
    }

    override fun populateModuleDependencies(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>, ideProject: DataNode<ProjectData>) {
        val mppModel = resolverCtx.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java)
        if (mppModel == null) {
            resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java)?.sourceSets?.values?.forEach { sourceSet ->
                sourceSet.dependencies.modifyDependenciesOnMppModules(ideProject, resolverCtx)
            }
            super.populateModuleDependencies(gradleModule, ideModule, ideProject)//TODO add dependencies on mpp module
        } else {
            mppModel.dependencyMap.values.modifyDependenciesOnMppModules(ideProject, resolverCtx)
        }
        populateModuleDependencies(gradleModule, ideProject, ideModule, resolverCtx)
    }

    private fun recordOutputDir(
        gradleOutputDir: File,
        effectiveOutputDir: File,
        sourceType: ExternalSystemSourceType,
        moduleData: GradleSourceSetData,
        moduleOutputsMap: MutableMap<String, Pair<String, ExternalSystemSourceType>>,
        gradleOutputMap: MultiMap<ExternalSystemSourceType, String>
    ) {
        val gradleOutputPath = toCanonicalPath(gradleOutputDir.absolutePath)
        gradleOutputMap.putValue(sourceType, gradleOutputPath)
        if (gradleOutputDir.path != effectiveOutputDir.path) {
            moduleOutputsMap[gradleOutputPath] = Pair(moduleData.id, sourceType)
        }
    }

    private fun excludeOutDir(ideModule: DataNode<ModuleData>, ideaOutDir: File) {
        val contentRootDataDataNode = ExternalSystemApiUtil.find(ideModule, ProjectKeys.CONTENT_ROOT)

        val excludedContentRootData: ContentRootData
        if (contentRootDataDataNode == null || !FileUtil.isAncestor(File(contentRootDataDataNode.data.rootPath), ideaOutDir, false)) {
            excludedContentRootData = ContentRootData(GradleConstants.SYSTEM_ID, ideaOutDir.absolutePath)
            ideModule.createChild(ProjectKeys.CONTENT_ROOT, excludedContentRootData)
        } else {
            excludedContentRootData = contentRootDataDataNode.data
        }

        excludedContentRootData.storePath(ExternalSystemSourceType.EXCLUDED, ideaOutDir.absolutePath)
    }

    companion object {
        val MPP_CONFIGURATION_ARTIFACTS =
            Key.create<MutableMap<String/* artifact path */, MutableList<String> /* module ids*/>>("gradleMPPArtifactsMap")
        val proxyObjectCloningCache = WeakHashMap<Any, Any>()

        private var nativeDebugAdvertised = false
        private val androidPluginPresent = PluginManager.getPlugin(PluginId.findId("org.jetbrains.android"))?.isEnabled ?: false

        private fun ExternalDependency.getDependencyArtifacts(): Collection<File> =
            when (this) {
                is ExternalProjectDependency -> this.projectDependencyArtifacts
                is FileCollectionDependency -> this.files
                else -> emptyList()
            }

        private fun getOrCreateAffiliatedArtifactsMap(ideProject: DataNode<ProjectData>): Map<String, List<String>>? {
            val mppArtifacts = ideProject.getUserData(MPP_CONFIGURATION_ARTIFACTS) ?: return null
            val configArtifacts = ideProject.getUserData(CONFIGURATION_ARTIFACTS) ?: return null
            // All MPP modules are already known, we can fill configurations map
            return /*ideProject.getUserData(MPP_AFFILATED_ARTIFACTS) ?:*/ HashMap<String, MutableList<String>>().also { newMap ->
                mppArtifacts.forEach { (filePath, moduleIds) ->
                    val list2add = ArrayList<String>()
                    newMap[filePath] = list2add
                    for ((index, module) in moduleIds.withIndex()) {
                        if (index == 0) {
                            configArtifacts[filePath] = module
                        } else {
                            val affiliatedFileName = "$filePath-MPP-$index"
                            configArtifacts[affiliatedFileName] = module
                            list2add.add(affiliatedFileName)
                        }
                    }
                }
                //ideProject.putUserData(MPP_AFFILATED_ARTIFACTS, newMap)
            }
        }

        private fun Collection<ExternalDependency>.modifyDependenciesOnMppModules(
            ideProject: DataNode<ProjectData>,
            resolverCtx: ProjectResolverContext
        ) {
            // Add mpp-artifacts into map used for dependency substitution
            val affiliatedArtifacts = getOrCreateAffiliatedArtifactsMap(ideProject)
            if (affiliatedArtifacts != null) {
                this.forEach { dependency ->
                    val existingArtifactDependencies = dependency.getDependencyArtifacts().map { normalizePath(it.absolutePath) }
                    val dependencies2add = existingArtifactDependencies.flatMap { affiliatedArtifacts[it] ?: emptyList() }
                        .filter { !existingArtifactDependencies.contains(it) }
                    dependencies2add.forEach {
                        dependency.addDependencyArtifactInternal(File(it))
                    }
                }
            }
        }

        private fun ExternalDependency.addDependencyArtifactInternal(file: File) {
            when (this) {
                is DefaultExternalProjectDependency -> this.projectDependencyArtifacts =
                    ArrayList<File>(this.projectDependencyArtifacts).also {
                        it.add(file)
                    }
                is ExternalProjectDependency -> try {
                    this.projectDependencyArtifacts.add(file)
                } catch (_: Exception) {
                    // ignore
                }
                is FileCollectionDependency -> this.files.add(file)
            }
        }

        fun initializeModuleData(
            gradleModule: IdeaModule,
            mainModuleNode: DataNode<ModuleData>,
            projectDataNode: DataNode<ProjectData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mainModuleData = mainModuleNode.data
            val mainModuleConfigPath = mainModuleData.linkedExternalProjectPath
            val mainModuleFileDirectoryPath = mainModuleData.moduleFileDirectoryPath

            val externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java)
            val mppModel = resolverCtx.getMppModel(gradleModule)
            if (mppModel == null || externalProject == null) return

            val jdkName = gradleModule.jdkNameIfAny

            // save artifacts locations.
            val userData = projectDataNode.getUserData(MPP_CONFIGURATION_ARTIFACTS) ?: HashMap<String, MutableList<String>>().apply {
                projectDataNode.putUserData(MPP_CONFIGURATION_ARTIFACTS, this)
            }

            mppModel.targets.filter { it.jar != null && it.jar!!.archiveFile != null }.forEach { target ->
                val path = toCanonicalPath(target.jar!!.archiveFile!!.absolutePath)
                val currentModules = userData[path] ?: ArrayList<String>().apply { userData[path] = this }
                // Test modules should not be added. Otherwise we could get dependnecy of java.mail on jvmTest
                val allSourceSets = target.compilations.filter { !it.isTestModule }.flatMap { it.sourceSets }.toSet()
                val availableViaDependsOn = allSourceSets.flatMap { it.dependsOnSourceSets }.mapNotNull { mppModel.sourceSets[it] }
                allSourceSets.union(availableViaDependsOn).forEach { sourceSet ->
                    currentModules.add(getKotlinModuleId(gradleModule, sourceSet, resolverCtx))
                }
            }

            val moduleGroup: Array<String>? = if (!resolverCtx.isUseQualifiedModuleNames) {
                val gradlePath = gradleModule.gradleProject.path
                val isRootModule = gradlePath.isEmpty() || gradlePath == ":"
                if (isRootModule) {
                    arrayOf(mainModuleData.internalName)
                } else {
                    gradlePath.split(":").drop(1).toTypedArray()
                }
            } else null

            val sourceSetMap = projectDataNode.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS)!!

            val sourceSetToRunTasks = calculateRunTasks(mppModel, gradleModule, resolverCtx)

            val sourceSetToCompilationData = LinkedHashMap<String, MutableSet<GradleSourceSetData>>()
            for (target in mppModel.targets) {
                if (delegateToAndroidPlugin(target)) continue
                if (target.name == KotlinTarget.METADATA_TARGET_NAME) continue
                val targetData = KotlinTargetData(target.name).also {
                    it.archiveFile = target.jar?.archiveFile
                    it.konanArtifacts = target.konanArtifacts
                }
                mainModuleNode.createChild(KotlinTargetData.KEY, targetData)

                val compilationIds = LinkedHashSet<String>()
                for (compilation in target.compilations) {
                    val moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                    val existingSourceSetDataNode = sourceSetMap[moduleId]?.first
                    if (existingSourceSetDataNode?.kotlinSourceSet != null) continue

                    compilationIds += moduleId

                    val moduleExternalName = getExternalModuleName(gradleModule, compilation)
                    val moduleInternalName = getInternalModuleName(gradleModule, externalProject, compilation, resolverCtx)

                    val compilationData = existingSourceSetDataNode?.data ?: GradleSourceSetData(
                        moduleId, moduleExternalName, moduleInternalName, mainModuleFileDirectoryPath, mainModuleConfigPath
                    ).also {
                        it.group = externalProject.group
                        it.version = externalProject.version

                        when (compilation.name) {
                            KotlinCompilation.MAIN_COMPILATION_NAME -> {
                                it.publication = ProjectId(externalProject.group, externalProject.name, externalProject.version)
                            }
                            KotlinCompilation.TEST_COMPILATION_NAME -> {
                                it.productionModuleId = getInternalModuleName(
                                    gradleModule,
                                    externalProject,
                                    compilation,
                                    resolverCtx,
                                    KotlinCompilation.MAIN_COMPILATION_NAME
                                )
                            }
                        }

                        it.ideModuleGroup = moduleGroup
                        it.sdkName = jdkName
                    }

                    val kotlinSourceSet = createSourceSetInfo(
                        compilation,
                        gradleModule,
                        resolverCtx
                    ) ?: continue
                    kotlinSourceSet.externalSystemRunTasks =
                        compilation.sourceSets.firstNotNullResult { sourceSetToRunTasks[it] } ?: emptyList()

                    if (compilation.platform == KotlinPlatform.JVM || compilation.platform == KotlinPlatform.ANDROID) {
                        compilationData.targetCompatibility = (kotlinSourceSet.compilerArguments as? K2JVMCompilerArguments)?.jvmTarget
                    } else if (compilation.platform == KotlinPlatform.NATIVE) {
                        // Kotlin/Native target has been added to KotlinNativeCompilation only in 1.3.60,
                        // so 'nativeExtensions' may be null in 1.3.5x or earlier versions
                        compilation.nativeExtensions?.konanTarget?.let { konanTarget ->
                            compilationData.konanTargets = setOf(konanTarget)
                        }
                    }

                    for (sourceSet in compilation.sourceSets) {
                        sourceSetToCompilationData.getOrPut(sourceSet.name) { LinkedHashSet() } += compilationData
                        for (dependentSourceSetName in sourceSet.dependsOnSourceSets) {
                            sourceSetToCompilationData.getOrPut(dependentSourceSetName) { LinkedHashSet() } += compilationData
                        }
                    }

                    val compilationDataNode =
                        (existingSourceSetDataNode ?: mainModuleNode.createChild(GradleSourceSetData.KEY, compilationData)).also {
                            it.kotlinSourceSet = kotlinSourceSet
                        }
                    if (existingSourceSetDataNode == null) {
                        sourceSetMap[moduleId] = Pair(compilationDataNode, createExternalSourceSet(compilation, compilationData, mppModel))
                    }
                }

                targetData.moduleIds = compilationIds
            }

            val ignoreCommonSourceSets by lazy { externalProject.notImportedCommonSourceSets() }
            for (sourceSet in mppModel.sourceSets.values) {
                if (delegateToAndroidPlugin(sourceSet)) continue
                if (sourceSet.actualPlatforms.supports(KotlinPlatform.COMMON) && ignoreCommonSourceSets) continue
                val moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                val existingSourceSetDataNode = sourceSetMap[moduleId]?.first
                if (existingSourceSetDataNode?.kotlinSourceSet != null) continue

                val moduleExternalName = getExternalModuleName(gradleModule, sourceSet)
                val moduleInternalName = getInternalModuleName(gradleModule, externalProject, sourceSet, resolverCtx)

                val sourceSetData = existingSourceSetDataNode?.data ?: GradleSourceSetData(
                    moduleId, moduleExternalName, moduleInternalName, mainModuleFileDirectoryPath, mainModuleConfigPath
                ).also {
                    it.group = externalProject.group
                    it.version = externalProject.version

                    val name = sourceSet.name
                    val baseName = name.removeSuffix("Test")
                    if (baseName != name) {
                        it.productionModuleId = getInternalModuleName(
                            gradleModule,
                            externalProject,
                            sourceSet,
                            resolverCtx,
                            baseName + "Main"
                        )
                    }

                    it.ideModuleGroup = moduleGroup
                    it.sdkName = jdkName

                    sourceSetToCompilationData[sourceSet.name]?.let { compilationDataRecords ->
                        it.targetCompatibility = compilationDataRecords
                            .mapNotNull { compilationData -> compilationData.targetCompatibility }
                            .minWith(VersionComparatorUtil.COMPARATOR)

                        if (sourceSet.actualPlatforms.getSinglePlatform() == KotlinPlatform.NATIVE) {
                            it.konanTargets = compilationDataRecords
                                .flatMap { compilationData -> compilationData.konanTargets }
                                .toSet()
                        }
                    }
                }

                val kotlinSourceSet = createSourceSetInfo(sourceSet, gradleModule, resolverCtx) ?: continue
                kotlinSourceSet.externalSystemRunTasks = sourceSetToRunTasks[sourceSet] ?: emptyList()

                val sourceSetDataNode =
                    (existingSourceSetDataNode ?: mainModuleNode.createChild(GradleSourceSetData.KEY, sourceSetData)).also {
                        it.kotlinSourceSet = kotlinSourceSet
                    }
                if (existingSourceSetDataNode == null) {
                    sourceSetMap[moduleId] = Pair(sourceSetDataNode, createExternalSourceSet(sourceSet, sourceSetData, mppModel))
                }
            }

            with(projectDataNode.data) {
                if (mainModuleData.linkedExternalProjectPath == linkedExternalProjectPath) {
                    group = mainModuleData.group
                    version = mainModuleData.version
                }
            }

            mainModuleNode.kotlinNativeHome = mppModel.kotlinNativeHome
            mainModuleNode.coroutines = mppModel.extraFeatures.coroutinesState
            mainModuleNode.isHmpp = mppModel.extraFeatures.isHMPPEnabled
            //TODO improve passing version of used multiplatform
        }

        private fun calculateRunTasks(
            mppModel: KotlinMPPGradleModel,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ): Map<KotlinSourceSet, Collection<ExternalSystemRunTask>> {
            val sourceSetToRunTasks: MutableMap<KotlinSourceSet, MutableCollection<ExternalSystemRunTask>> = HashMap()
            val dependsOnReverseGraph: MutableMap<String, MutableSet<KotlinSourceSet>> = HashMap()
            mppModel.targets.forEach { target ->
                target.compilations.forEach { compilation ->
                    val testRunTasks = target.testRunTasks
                        .filter { task -> task.compilationName == compilation.name }
                        .map {
                            ExternalSystemTestRunTask(
                                it.taskName,
                                getKotlinModuleId(gradleModule, compilation, resolverCtx),
                                target.name
                            )
                        }
                    val nativeMainRunTasks = target.nativeMainRunTasks
                        .filter { task -> task.compilationName == compilation.name }
                        .map {
                            ExternalSystemNativeMainRunTask(
                                it.taskName,
                                getKotlinModuleId(gradleModule, compilation, resolverCtx),
                                target.name,
                                it.entryPoint,
                                it.debuggable
                            )
                        }
                    val allRunTasks = testRunTasks + nativeMainRunTasks
                    compilation.sourceSets.forEach { sourceSet ->
                        sourceSetToRunTasks.getOrPut(sourceSet) { LinkedHashSet() } += allRunTasks
                        sourceSet.dependsOnSourceSets.forEach { dependentModule ->
                            dependsOnReverseGraph.getOrPut(dependentModule) { LinkedHashSet() } += sourceSet
                        }
                    }
                }
            }
            mppModel.sourceSets.forEach { (sourceSetName, sourceSet) ->
                dependsOnReverseGraph[sourceSetName]?.forEach { dependingSourceSet ->
                    sourceSetToRunTasks.getOrPut(sourceSet) { LinkedHashSet() } += sourceSetToRunTasks[dependingSourceSet] ?: emptyList()
                }
            }
            return sourceSetToRunTasks
        }

        fun populateContentRoots(
            gradleModule: IdeaModule,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mppModel = resolverCtx.getMppModel(gradleModule) ?: return
            val sourceSetToPackagePrefix = mppModel.targets.flatMap { it.compilations }
                .flatMap { compilation ->
                    compilation.sourceSets.map { sourceSet -> sourceSet.name to compilation.kotlinTaskProperties.packagePrefix }
                }
                .toMap()
            if (resolverCtx.getExtraProject(gradleModule, ExternalProject::class.java) == null) return
            processSourceSets(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, sourceSet ->
                if (dataNode == null || delegateToAndroidPlugin(sourceSet)) return@processSourceSets

                createContentRootData(
                    sourceSet.sourceDirs,
                    sourceSet.sourceType,
                    sourceSetToPackagePrefix[sourceSet.name],
                    dataNode
                )
                createContentRootData(
                    sourceSet.resourceDirs,
                    sourceSet.resourceType,
                    null,
                    dataNode
                )
            }

            for (gradleContentRoot in gradleModule.contentRoots ?: emptySet<IdeaContentRoot?>()) {
                if (gradleContentRoot == null) continue

                val rootDirectory = gradleContentRoot.rootDirectory ?: continue
                val ideContentRoot = ContentRootData(GradleConstants.SYSTEM_ID, rootDirectory.absolutePath).also { ideContentRoot ->
                    (gradleContentRoot.excludeDirectories ?: emptySet()).forEach { file ->
                        ideContentRoot.storePath(ExternalSystemSourceType.EXCLUDED, file.absolutePath)
                    }
                }
                ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot)
            }

            val mppModelPureKotlinSourceFolders = mppModel.targets.flatMap { it.compilations }
                .flatMap { it.kotlinTaskProperties.pureKotlinSourceFolders ?: emptyList() }
                .map { it.absolutePath }

            ideModule.pureKotlinSourceFolders =
                if (ideModule.pureKotlinSourceFolders.isEmpty())
                    mppModelPureKotlinSourceFolders
                else
                    mppModelPureKotlinSourceFolders + ideModule.pureKotlinSourceFolders
        }

        private data class CompilationWithDependencies(
            val compilation: KotlinCompilation,
            val substitutedDependencies: List<ExternalDependency>
        ) {
            val konanTarget: String?
                get() = compilation.nativeExtensions?.konanTarget

            val dependencyNames: Map<String, ExternalDependency> by lazy {
                substitutedDependencies.associateBy { it.name.removeSuffixIfPresent(" [$konanTarget]") }
            }
        }

        fun populateModuleDependencies(
            gradleModule: IdeaModule,
            ideProject: DataNode<ProjectData>,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ) {
            val mppModel = resolverCtx.getMppModel(gradleModule) ?: return
            val sourceSetMap = ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS) ?: return
            val artifactsMap = ideProject.getUserData(CONFIGURATION_ARTIFACTS) ?: return
            val substitutor = KotlinNativeLibrariesDependencySubstitutor(mppModel, gradleModule, resolverCtx)
            val sourceSetToCompilations = mutableMapOf<String, MutableList<CompilationWithDependencies>>()
            val processedModuleIds = HashSet<String>()
            processCompilations(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, compilation ->
                if (processedModuleIds.add(getKotlinModuleId(gradleModule, compilation, resolverCtx))) {
                    val substitutedDependencies =
                        substitutor.substituteDependencies(compilation.dependencies.mapNotNull { mppModel.dependencyMap[it] })
                    buildDependencies(
                        resolverCtx,
                        sourceSetMap,
                        artifactsMap,
                        dataNode,
                        preprocessDependencies(substitutedDependencies),
                        ideProject
                    )
                    KotlinNativeLibrariesFixer.applyTo(dataNode, ideProject)
                    for (sourceSet in compilation.sourceSets) {
                        (sourceSet.dependsOnSourceSets + sourceSet.name).forEach {
                            sourceSetToCompilations
                                .getOrPut(it) { mutableListOf() }
                                .add(CompilationWithDependencies(compilation, substitutedDependencies))
                        }
                        if (sourceSet.fullName() == compilation.fullName()) continue
                        val targetDataNode = getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx) ?: continue
                        addDependency(dataNode, targetDataNode, sourceSet.isTestModule)
                    }
                }
            }
            val sourceSetGraph = GraphBuilder.directed().build<KotlinSourceSet>()
            processSourceSets(gradleModule, mppModel, ideModule, resolverCtx) { dataNode, sourceSet ->
                sourceSetGraph.addNode(sourceSet)
                val productionSourceSet = dataNode
                    ?.data
                    ?.productionModuleId
                    ?.let { ideModule.findChildModuleByInternalName(it) }
                    ?.kotlinSourceSet
                    ?.kotlinModule
                    ?.toSourceSet(mppModel)
                if (productionSourceSet != null) {
                    sourceSetGraph.putEdge(sourceSet, productionSourceSet)
                }
                for (targetSourceSetName in sourceSet.dependsOnSourceSets) {
                    val targetSourceSet = mppModel.sourceSets[targetSourceSetName] ?: continue
                    sourceSetGraph.putEdge(sourceSet, targetSourceSet)
                }
                // Workaround: Non-android source sets have commonMain/commonTest in their dependsOn
                // Remove when the same is implemented for Android modules as well
                if (sourceSet.actualPlatforms.supports(KotlinPlatform.ANDROID)) {
                    val commonSourceSetName = if (sourceSet.isTestModule) {
                        KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
                    } else {
                        KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
                    }
                    val commonSourceSet = mppModel.sourceSets[commonSourceSetName]
                    if (commonSourceSet != null && commonSourceSet != sourceSet) {
                        sourceSetGraph.putEdge(sourceSet, commonSourceSet)
                    }
                }
            }
            val closedSourceSetGraph = Graphs.transitiveClosure(sourceSetGraph)
            for (sourceSet in closedSourceSetGraph.nodes()) {
                val isAndroid = delegateToAndroidPlugin(sourceSet)
                val fromDataNode = if (isAndroid) {
                    ideModule
                } else {
                    getSiblingKotlinModuleData(sourceSet, gradleModule, ideModule, resolverCtx)
                } ?: continue
                val dependeeSourceSets = closedSourceSetGraph.successors(sourceSet)
                val sourceSetInfos = if (isAndroid) {
                    ideModule.kotlinAndroidSourceSets?.filter {
                        (it.kotlinModule as? KotlinCompilation)?.sourceSets?.contains(sourceSet) ?: false
                    } ?: emptyList()
                } else {
                    listOfNotNull(fromDataNode.kotlinSourceSet)
                }
                for (sourceSetInfo in sourceSetInfos) {
                    if (sourceSetInfo.kotlinModule is KotlinCompilation) {
                        val selfName = sourceSetInfo.kotlinModule.fullName()
                        sourceSetInfo.addSourceSets(dependeeSourceSets, selfName, gradleModule, resolverCtx)
                    }
                }
                if (delegateToAndroidPlugin(sourceSet)) continue
                for (dependeeSourceSet in dependeeSourceSets) {
                    val toDataNode = getSiblingKotlinModuleData(dependeeSourceSet, gradleModule, ideModule, resolverCtx) ?: continue
                    addDependency(fromDataNode, toDataNode, dependeeSourceSet.isTestModule)
                }
                if (processedModuleIds.add(getKotlinModuleId(gradleModule, sourceSet, resolverCtx))) {
                    val mergedSubstitutedDependencies = LinkedHashSet<KotlinDependency>().apply {
                        val forceNativeDependencyPropagation: Boolean
                        val excludeInheritedNativeDependencies: Boolean
                        if (mppModel.extraFeatures.isHMPPEnabled && sourceSet.actualPlatforms.getSinglePlatform() == KotlinPlatform.NATIVE) {
                            forceNativeDependencyPropagation = mppModel.extraFeatures.isNativeDependencyPropagationEnabled
                            excludeInheritedNativeDependencies = !forceNativeDependencyPropagation
                        } else {
                            forceNativeDependencyPropagation = false
                            excludeInheritedNativeDependencies = false
                        }
                        addAll(substitutor.substituteDependencies(sourceSet))
                        dependeeSourceSets.flatMapTo(this) { dependeeSourceSet ->
                            substitutor.substituteDependencies(dependeeSourceSet).run {
                                if (excludeInheritedNativeDependencies)
                                    filter { !it.name.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) }
                                else this
                            }
                        }
                        if (forceNativeDependencyPropagation) {
                            sourceSetToCompilations[sourceSet.name]?.let { compilations ->
                                addAll(propagatedNativeDependencies(compilations))
                            }
                        }
                    }
                    buildDependencies(
                        resolverCtx,
                        sourceSetMap,
                        artifactsMap,
                        fromDataNode,
                        preprocessDependencies(mergedSubstitutedDependencies),
                        ideProject
                    )
                    @Suppress("UNCHECKED_CAST")
                    KotlinNativeLibrariesFixer.applyTo(fromDataNode as DataNode<GradleSourceSetData>, ideProject)
                }
            }
        }

        private fun KotlinNativeLibrariesDependencySubstitutor.substituteDependencies(kotlinModule: KotlinModule): List<ExternalDependency> =
            substituteDependencies(kotlinModule.dependencies.mapNotNull { mppModel.dependencyMap[it] })

        // We can't really commonize native platform libraries yet.
        // But APIs for different targets may be very similar.
        // E.g. ios_arm64 and ios_x64 have almost identical platform libraries.
        // We handle these special cases and resolve common sources for such
        // targets against libraries of one of them. E.g. common sources for
        // ios_x64 and ios_arm64 will be resolved against ios_arm64 libraries.
        //
        // Currently such special casing is available for Apple platforms
        // (iOS, watchOS and tvOS) and native Android (ARM, X86).
        // TODO: Do we need to support user's interop libraries too?
        private fun propagatedNativeDependencies(compilations: List<CompilationWithDependencies>): List<ExternalDependency> {
            if (compilations.size <= 1) {
                return emptyList()
            }

            val copyFrom = when {
                compilations.all { it.isAppleCompilation } ->
                    compilations.selectFirstAvailableTarget(
                        "watchos_arm64", "watchos_arm32", "watchos_x86",
                        "ios_arm64", "ios_arm32", "ios_x64",
                        "tvos_arm64", "tvos_x64"
                    )
                compilations.all { it.konanTarget?.startsWith("android") == true } ->
                    compilations.selectFirstAvailableTarget(
                        "android_arm64", "android_arm32", "android_x64", "android_x86"
                    )
                else -> return emptyList()
            }

            return copyFrom.dependencyNames.mapNotNull { (name, dependency) ->
                when {
                    !name.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) -> null  // Support only default platform libs for now.
                    compilations.all { it.dependencyNames.containsKey(name) } -> dependency
                    else -> null
                }
            }
        }

        private val CompilationWithDependencies.isAppleCompilation: Boolean
            get() = konanTarget?.let {
                it.startsWith("ios") || it.startsWith("watchos") || it.startsWith("tvos")
            } ?: false

        private fun Iterable<CompilationWithDependencies>.selectFirstAvailableTarget(@NonNls vararg targetsByPriority: String): CompilationWithDependencies {
            for (target in targetsByPriority) {
                val result = firstOrNull { it.konanTarget == target }
                if (result != null) {
                    return result
                }
            }
            return first()
        }

        private fun KotlinModule.toSourceSet(mppModel: KotlinMPPGradleModel) = when (this) {
            is KotlinSourceSet -> this
            is KotlinCompilation -> mppModel.sourceSets[fullName()]
            else -> null
        }

        private fun preprocessDependencies(dependencies: Collection<KotlinDependency>): List<ExternalDependency> {
            return dependencies
                .groupBy { it.id }
                .mapValues { it.value.firstOrNull { it.scope == "COMPILE" } ?: it.value.lastOrNull() }
                .values
                .filterNotNull()
        }

        private fun addDependency(fromModule: DataNode<*>, toModule: DataNode<*>, dependOnTestModule: Boolean) {
            if (fromModule.data == toModule.data) return
            val fromData = fromModule.data as? ModuleData ?: return
            val toData = toModule.data as? ModuleData ?: return
            val existing = fromModule.children.mapNotNull { it.data as? ModuleDependencyData }.filter { it.target.id == (toModule.data as? ModuleData)?.id }
            val nodeToModify =
                existing.singleOrNull() ?: existing.firstOrNull { it.scope == DependencyScope.COMPILE } ?: existing.firstOrNull()
            if (nodeToModify != null) {
                nodeToModify.scope = DependencyScope.COMPILE
                nodeToModify.isProductionOnTestDependency = nodeToModify.isProductionOnTestDependency || dependOnTestModule
                return
            }
            val moduleDependencyData = ModuleDependencyData(fromData, toData).also {
                it.scope = DependencyScope.COMPILE
                it.isExported = false
                it.isProductionOnTestDependency = dependOnTestModule
            }
            fromModule.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData)
        }

        private fun getSiblingKotlinModuleData(
            kotlinModule: KotlinModule,
            gradleModule: IdeaModule,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext
        ): DataNode<out ModuleData>? {
            val usedModuleId = getKotlinModuleId(gradleModule, kotlinModule, resolverCtx)
            return ideModule.findChildModuleById(usedModuleId)
        }

        private fun createContentRootData(
            sourceDirs: Set<File>,
            sourceType: ExternalSystemSourceType,
            packagePrefix: String?,
            parentNode: DataNode<*>
        ) {
            for (sourceDir in sourceDirs) {
                val contentRootData = ContentRootData(GradleConstants.SYSTEM_ID, sourceDir.absolutePath)
                contentRootData.storePath(sourceType, sourceDir.absolutePath, packagePrefix)
                parentNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
            }
        }

        private fun processSourceSets(
            gradleModule: IdeaModule,
            mppModel: KotlinMPPGradleModel,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext,
            processor: (DataNode<GradleSourceSetData>?, KotlinSourceSet) -> Unit
        ) {
            val sourceSetsMap = HashMap<String, DataNode<GradleSourceSetData>>()
            for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
                if (dataNode.kotlinSourceSet != null) {
                    sourceSetsMap[dataNode.data.id] = dataNode
                }
            }
            for (sourceSet in mppModel.sourceSets.values) {
                val moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                val moduleDataNode = sourceSetsMap[moduleId]
                processor(moduleDataNode, sourceSet)
            }
        }

        private fun processCompilations(
            gradleModule: IdeaModule,
            mppModel: KotlinMPPGradleModel,
            ideModule: DataNode<ModuleData>,
            resolverCtx: ProjectResolverContext,
            processor: (DataNode<GradleSourceSetData>, KotlinCompilation) -> Unit
        ) {
            val sourceSetsMap = HashMap<String, DataNode<GradleSourceSetData>>()
            for (dataNode in ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
                if (dataNode.kotlinSourceSet != null) {
                    sourceSetsMap[dataNode.data.id] = dataNode
                }
            }
            for (target in mppModel.targets) {
                if (delegateToAndroidPlugin(target)) continue
                for (compilation in target.compilations) {
                    val moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                    val moduleDataNode = sourceSetsMap[moduleId] ?: continue
                    processor(moduleDataNode, compilation)
                }
            }
        }

        private val IdeaModule.jdkNameIfAny
            get() = try {
                jdkName
            } catch (e: UnsupportedMethodException) {
                null
            }

        private fun getExternalModuleName(gradleModule: IdeaModule, kotlinModule: KotlinModule) =
            gradleModule.name + ":" + kotlinModule.fullName()

        private fun gradlePathToQualifiedName(
            rootName: String,
            gradlePath: String
        ): String? {
            return ((if (gradlePath.startsWith(":")) "$rootName." else "")
                    + Arrays.stream(gradlePath.split(":".toRegex()).toTypedArray())
                .filter { s: String -> s.isNotEmpty() }
                .collect(Collectors.joining(".")))
        }

        private fun getInternalModuleName(
            gradleModule: IdeaModule,
            externalProject: ExternalProject,
            kotlinModule: KotlinModule,
            resolverCtx: ProjectResolverContext,
            actualName: String = kotlinModule.name
        ): String {
            val delimiter: String
            val moduleName = StringBuilder()

            val buildSrcGroup = resolverCtx.buildSrcGroup
            if (resolverCtx.isUseQualifiedModuleNames) {
                delimiter = "."
                if (StringUtil.isNotEmpty(buildSrcGroup)) {
                    moduleName.append(buildSrcGroup).append(delimiter)
                }
                moduleName.append(
                    gradlePathToQualifiedName(
                        gradleModule.project.name,
                        externalProject.qName
                    )
                )
            } else {
                delimiter = "_"
                if (StringUtil.isNotEmpty(buildSrcGroup)) {
                    moduleName.append(buildSrcGroup).append(delimiter)
                }
                moduleName.append(gradleModule.name)
            }
            moduleName.append(delimiter)
            moduleName.append(kotlinModule.fullName(actualName))
            return PathUtilRt.suggestFileName(moduleName.toString(), true, false)
        }

        private fun createExternalSourceSet(
            compilation: KotlinCompilation,
            compilationData: GradleSourceSetData,
            mppModel: KotlinMPPGradleModel
        ): ExternalSourceSet {
            return DefaultExternalSourceSet().also { sourceSet ->
                val effectiveClassesDir = compilation.output.effectiveClassesDir
                val resourcesDir = compilation.output.resourcesDir

                sourceSet.name = compilation.fullName()
                sourceSet.targetCompatibility = compilationData.targetCompatibility
                sourceSet.dependencies += compilation.dependencies.mapNotNull { mppModel.dependencyMap[it] }
                //TODO after applying patch to IDEA core uncomment the following line:
                // sourceSet.isTest = compilation.sourceSets.filter { isTestModule }.isNotEmpty()
                // It will allow to get rid of hacks with guessing module type in DataServices and obtain properly set productionOnTest flags
                val sourcesWithTypes = SmartList<kotlin.Pair<ExternalSystemSourceType, DefaultExternalSourceDirectorySet>>()
                if (effectiveClassesDir != null) {
                    sourcesWithTypes += compilation.sourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.outputDir = effectiveClassesDir
                        dirSet.srcDirs = compilation.sourceSets.flatMapTo(LinkedHashSet()) { it.sourceDirs }
                        dirSet.gradleOutputDirs += compilation.output.classesDirs
                        dirSet.setInheritedCompilerOutput(false)
                    }
                }
                if (resourcesDir != null) {
                    sourcesWithTypes += compilation.resourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.outputDir = resourcesDir
                        dirSet.srcDirs = compilation.sourceSets.flatMapTo(LinkedHashSet()) { it.resourceDirs }
                        dirSet.gradleOutputDirs += resourcesDir
                        dirSet.setInheritedCompilerOutput(false)
                    }
                }

                sourceSet.setSources(sourcesWithTypes.toMap())
            }
        }


        private fun createExternalSourceSet(
            ktSourceSet: KotlinSourceSet,
            ktSourceSetData: GradleSourceSetData,
            mppModel: KotlinMPPGradleModel
        ): ExternalSourceSet {
            return DefaultExternalSourceSet().also { sourceSet ->
                sourceSet.name = ktSourceSet.name
                sourceSet.targetCompatibility = ktSourceSetData.targetCompatibility
                sourceSet.dependencies += ktSourceSet.dependencies.mapNotNull { mppModel.dependencyMap[it] }

                sourceSet.setSources(linkedMapOf(
                    ktSourceSet.sourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.srcDirs = ktSourceSet.sourceDirs
                    },
                    ktSourceSet.resourceType to DefaultExternalSourceDirectorySet().also { dirSet ->
                        dirSet.srcDirs = ktSourceSet.resourceDirs
                    }
                ).toMap())
            }
        }

        private val KotlinModule.sourceType
            get() = if (isTestModule) ExternalSystemSourceType.TEST else ExternalSystemSourceType.SOURCE

        private val KotlinModule.resourceType
            get() = if (isTestModule) ExternalSystemSourceType.TEST_RESOURCE else ExternalSystemSourceType.RESOURCE

        private fun createSourceSetInfo(
            sourceSet: KotlinSourceSet,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ): KotlinSourceSetInfo? {
            if (sourceSet.actualPlatforms.platforms.none { !it.isNotSupported() }) return null
            return KotlinSourceSetInfo(sourceSet).also { info ->
                val languageSettings = sourceSet.languageSettings
                info.moduleId = getKotlinModuleId(gradleModule, sourceSet, resolverCtx)
                info.gradleModuleId = getModuleId(resolverCtx, gradleModule)
                info.actualPlatforms.addSimplePlatforms(sourceSet.actualPlatforms.platforms)
                info.isTestModule = sourceSet.isTestModule
                info.dependsOn = sourceSet.dependsOnSourceSets.toList().map {
                    getGradleModuleQualifiedName(resolverCtx, gradleModule, it)
                }
                //TODO(auskov): target flours are lost here
                info.compilerArguments = createCompilerArguments(emptyList(), sourceSet.actualPlatforms.getSinglePlatform()).also {
                    it.multiPlatform = true
                    it.languageVersion = languageSettings.languageVersion
                    it.apiVersion = languageSettings.apiVersion
                    it.progressiveMode = languageSettings.isProgressiveMode
                    it.internalArguments = languageSettings.enabledLanguageFeatures.mapNotNull {
                        val feature = LanguageFeature.fromString(it) ?: return@mapNotNull null
                        val arg = "-XXLanguage:+$it"
                        ManualLanguageFeatureSetting(feature, LanguageFeature.State.ENABLED, arg)
                    }
                    it.useExperimental = languageSettings.experimentalAnnotationsInUse.toTypedArray()
                    it.pluginOptions = languageSettings.compilerPluginArguments
                    it.pluginClasspaths = languageSettings.compilerPluginClasspath.map(File::getPath).toTypedArray()
                    it.freeArgs = languageSettings.freeCompilerArgs.toMutableList()
                }
            }
        }

        // TODO: Unite with other createSourceSetInfo
        // This method is used in Android side of import and it's signature could not be changed
        fun createSourceSetInfo(
            compilation: KotlinCompilation,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ): KotlinSourceSetInfo? {
            if (compilation.platform.isNotSupported()) return null
            if (Proxy.isProxyClass(compilation.javaClass)) {
                return createSourceSetInfo(
                    KotlinCompilationImpl(compilation, HashMap<Any, Any>()),
                    gradleModule,
                    resolverCtx
                )
            }
            return KotlinSourceSetInfo(compilation).also { sourceSetInfo ->
                sourceSetInfo.moduleId = getKotlinModuleId(gradleModule, compilation, resolverCtx)
                sourceSetInfo.gradleModuleId = getModuleId(resolverCtx, gradleModule)
                sourceSetInfo.actualPlatforms.addSimplePlatforms(listOf(compilation.platform))
                sourceSetInfo.isTestModule = compilation.isTestModule
                sourceSetInfo.dependsOn = compilation.sourceSets.flatMap { it.dependsOnSourceSets }.map {
                    getGradleModuleQualifiedName(resolverCtx, gradleModule, it)
                }.distinct().toList()
                sourceSetInfo.compilerArguments =
                    createCompilerArguments(compilation.arguments.currentArguments.toList(), compilation.platform).also {
                        it.multiPlatform = true
                    }
                sourceSetInfo.dependencyClasspath = compilation.dependencyClasspath.toList()
                sourceSetInfo.defaultCompilerArguments =
                    createCompilerArguments(compilation.arguments.defaultArguments.toList(), compilation.platform)
                sourceSetInfo.addSourceSets(compilation.sourceSets, compilation.fullName(), gradleModule, resolverCtx)
            }
        }

        /** Checks if our IDE doesn't support such platform */
        private fun KotlinPlatform.isNotSupported() = IdePlatformKindTooling.getToolingIfAny(this) == null

        private fun KotlinSourceSetInfo.addSourceSets(
            sourceSets: Collection<KotlinModule>,
            selfName: String,
            gradleModule: IdeaModule,
            resolverCtx: ProjectResolverContext
        ) {
            sourceSets
                .asSequence()
                .filter { it.fullName() != selfName }
                .forEach { sourceSetIdsByName[it.name] = getKotlinModuleId(gradleModule, it, resolverCtx) }
        }

        private fun createCompilerArguments(args: List<String>, platform: KotlinPlatform): CommonCompilerArguments {
            val compilerArguments = IdePlatformKindTooling.getTooling(platform).kind.argumentsClass.newInstance()
            parseCommandLineArguments(args.toList(), compilerArguments)
            return compilerArguments
        }

        private fun KotlinModule.fullName(simpleName: String = name) = when (this) {
            is KotlinCompilation -> compilationFullName(simpleName, disambiguationClassifier)
            else -> simpleName
        }

        private fun getKotlinModuleId(gradleModule: IdeaModule, kotlinModule: KotlinModule, resolverCtx: ProjectResolverContext) =
            getGradleModuleQualifiedName(resolverCtx, gradleModule, kotlinModule.fullName())

        private fun getGradleModuleQualifiedName(
            resolverCtx: ProjectResolverContext,
            gradleModule: IdeaModule,
            simpleName: String
        ): String =
            getModuleId(resolverCtx, gradleModule) + ":" + simpleName

        private fun ExternalProject.notImportedCommonSourceSets() =
            GradlePropertiesFileFacade.forExternalProject(this).readProperty(KOTLIN_NOT_IMPORTED_COMMON_SOURCE_SETS_SETTING)?.equals(
                "true",
                ignoreCase = true
            ) ?: false

        private fun delegateToAndroidPlugin(kotlinTarget: KotlinTarget): Boolean =
            androidPluginPresent && kotlinTarget.platform == KotlinPlatform.ANDROID

        private fun delegateToAndroidPlugin(kotlinSourceSet: KotlinSourceSet): Boolean =
            androidPluginPresent && kotlinSourceSet.actualPlatforms.platforms.singleOrNull() == KotlinPlatform.ANDROID
    }
}

fun ProjectResolverContext.getMppModel(gradleModule: IdeaModule): KotlinMPPGradleModel? {
    val mppModel = this.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java)
    return if (mppModel is Proxy) {
        this.getExtraProject(gradleModule, KotlinMPPGradleModel::class.java)
            ?.let { kotlinMppModel ->
                KotlinMPPGradleProjectResolver.proxyObjectCloningCache[kotlinMppModel] as? KotlinMPPGradleModelImpl
                    ?: KotlinMPPGradleModelImpl(
                        kotlinMppModel,
                        KotlinMPPGradleProjectResolver.proxyObjectCloningCache
                    ).also {
                        KotlinMPPGradleProjectResolver.proxyObjectCloningCache[kotlinMppModel] = it
                    }
            }
    } else {
        mppModel
    }
}

