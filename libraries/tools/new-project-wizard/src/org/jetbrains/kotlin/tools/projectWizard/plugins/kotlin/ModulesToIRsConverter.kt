package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import java.nio.file.Path

data class ModuleConfigurationData(
    val rootModules: List<Module>,
    val projectPath: Path,
    val projectName: String,
    val kotlinVersion: Version,
    val buildSystemType: BuildSystemType,
    val pomIr: PomIR,
    val writer: Writer
) {
    val allModules = rootModules.withAllSubModules()
}

private data class ModulesToIrsState(
    val parentPath: Path,
    val parentModuleHasTransitivelySpecifiedKotlinVersion: Boolean
)

private fun ModulesToIrsState.stateForSubModule(currentModulePath: Path) =
    copy(
        parentPath = currentModulePath,
        parentModuleHasTransitivelySpecifiedKotlinVersion = true
    )

class ModulesToIRsConverter(
    val data: ModuleConfigurationData
) {
    private val isSingleRootModuleMode = data.rootModules.size == 1

    // TODO get rid of mutable state
    private val rootBuildFileIrs = mutableListOf<BuildSystemIR>()

    // check if we need to flatten our module structure to a single-module
    // as we always have a root module in the project
    // which is redundant for a single module projects
    private val needFlattening: Boolean
        get() {
            if ( // We want to have root build file for android or ios projects
                data.allModules.any { it.configurator.requiresRootBuildFile }
            ) return false
            return isSingleRootModuleMode
        }

    private fun calculatePathForModule(module: Module, rootPath: Path) = when {
        needFlattening && module.isRootModule -> data.projectPath
        else -> rootPath / module.name
    }

    fun Reader.createBuildFiles(): TaskResult<List<BuildFileIR>> = with(data) {
        val needExplicitRootBuildFile = !needFlattening
        val parentModuleHasTransitivelySpecifiedKotlinVersion = allModules.any { modules ->
            modules.configurator == AndroidSinglePlatformModuleConfigurator
        }
        val initialState = ModulesToIrsState(projectPath, parentModuleHasTransitivelySpecifiedKotlinVersion)
        rootModules.mapSequence {
            createBuildFileForModule(it, initialState)
        }.map { it.flatten() }.map { buildFiles ->
            if (needExplicitRootBuildFile) buildFiles + createRootBuildFile()
            else buildFiles
        }
    }

    private fun createRootBuildFile(): BuildFileIR = with(data) {
        BuildFileIR(
            projectName,
            projectPath,
            RootFileModuleStructureIR(persistentListOf()),
            pomIr,
            rootBuildFileIrs.toPersistentList()
        )
    }


    private fun Reader.createBuildFileForModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = when (val configurator = module.configurator) {
        is MppModuleConfigurator -> createMultiplatformModule(module, state)
        is SinglePlatformModuleConfigurator -> createSinglePlatformModule(module, configurator, state)
        else -> Success(emptyList())
    }

    private fun Reader.createSinglePlatformModule(
        module: Module,
        configurator: SinglePlatformModuleConfigurator,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        writer.mutateProjectStructureByModuleConfigurator(module, modulePath)
        val buildFileIR = run {
            if (!configurator.needCreateBuildFile) return@run null
            val dependenciesIRs = buildPersistenceList<BuildSystemIR> {
                +module.sourcesets.flatMap { sourceset ->
                    sourceset.dependencies.map { it.toIR(sourceset.sourcesetType.toDependencyType()) }
                }
                with(configurator) { +createModuleIRs(this@createSinglePlatformModule, data, module) }
                addIfNotNull(
                    configurator.createStdlibType(data, module)?.let { stdlibType ->
                        KotlinStdlibDependencyIR(
                            type = stdlibType,
                            isInMppModule = false,
                            version = kotlinVersion,
                            dependencyType = DependencyType.MAIN
                        )
                    }
                )
            }

            val moduleIr = SingleplatformModuleIR(
                if (modulePath == projectPath) projectName else module.name,
                modulePath,
                dependenciesIRs,
                module.template,
                module,
                module.sourcesets.map { sourceset ->
                    SingleplatformSourcesetIR(
                        sourceset.sourcesetType,
                        modulePath / Defaults.SRC_DIR / sourceset.sourcesetType.name,
                        sourceset.dependencies.map { it.toIR(sourceset.sourcesetType.toDependencyType()) }.toPersistentList(),
                        sourceset
                    )
                }
            )
            BuildFileIR(
                module.name,
                modulePath,
                SingleplatformModulesStructureWithSingleModuleIR(
                    moduleIr,
                    persistentListOf()
                ),
                pomIr.copy(artifactId = module.name),
                createBuildFileIRs(module, state)
            )
        }

        return module.subModules.mapSequence { subModule ->
            createBuildFileForModule(
                subModule,
                state.stateForSubModule(modulePath)
            )
        }.map { it.flatten() }
            .map { children ->
                buildFileIR?.let { children + it } ?: children
            }
    }

    private fun Reader.createMultiplatformModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        writer.mutateProjectStructureByModuleConfigurator(module, modulePath)
        val targetIrs = module.subModules.flatMap { subModule ->
            with(subModule.configurator as TargetConfigurator) { createTargetIrs(subModule) }
        }

        val targetModuleIrs = module.subModules.map { target ->
            createTargetModule(target, modulePath)
        }

        return BuildFileIR(
            projectName,
            modulePath,
            MultiplatformModulesStructureIR(
                targetIrs,
                targetModuleIrs,
                persistentListOf()
            ),
            pomIr,
            buildPersistenceList {
                +createBuildFileIRs(module, state)
                module.subModules.forEach { +createBuildFileIRs(it, state) }
            }
        ).asSingletonList().asSuccess()
    }

    private fun Reader.createTargetModule(target: Module, modulePath: Path): MultiplatformModuleIR {
        data.writer.mutateProjectStructureByModuleConfigurator(target, modulePath)
        val sourcesetss = target.sourcesets.map { sourceset ->
            val sourcesetName = target.name + sourceset.sourcesetType.name.capitalize()
            val sourcesetIrs = buildList<BuildSystemIR> {
                +sourceset.dependencies.map { it.toIR(sourceset.sourcesetType.toDependencyType()) }
                if (sourceset.sourcesetType == SourcesetType.main) {
                    addIfNotNull(
                        target.configurator.createStdlibType(data, target)?.let { stdlibType ->
                            KotlinStdlibDependencyIR(
                                type = stdlibType,
                                isInMppModule = true,
                                version = data.kotlinVersion,
                                dependencyType = DependencyType.MAIN
                            )
                        }
                    )
                }
            }
            MultiplatformSourcesetIR(
                sourceset.sourcesetType,
                modulePath / Defaults.SRC_DIR / sourcesetName,
                target.name,
                sourcesetIrs.toPersistentList(),
                sourceset
            )
        }
        return MultiplatformModuleIR(
            target.name,
            modulePath,
            with(target.configurator) { createModuleIRs(this@createTargetModule, data, target) }.toPersistentList(),
            target.template,
            target,
            sourcesetss
        )
    }

    private fun Writer.mutateProjectStructureByModuleConfigurator(
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = with(module.configurator) {
        compute {
            rootBuildFileIrs += createRootBuildFileIrs(data)
            runArbitraryTask(data, module, modulePath).ensure()
            if (this@with is GradleModuleConfigurator) {
                GradlePlugin::settingsGradleFileIRs.addValues(createSettingsGradleIRs(module)).ensure()
            }
        }
    }

    private fun Reader.createBuildFileIRs(
        module: Module,
        state: ModulesToIrsState
    ) = buildPersistenceList<BuildSystemIR> {
        val kotlinPlugin = module.configurator.createKotlinPluginIR(data, module)
            ?.let { plugin ->
                // do not print version for non-root modules for gradle
                val needRemoveVersion = data.buildSystemType.isGradle
                        && state.parentModuleHasTransitivelySpecifiedKotlinVersion
                        && module.configurator != AndroidSinglePlatformModuleConfigurator
                when {
                    needRemoveVersion -> plugin.copy(version = null)
                    else -> plugin
                }
            }
        addIfNotNull(kotlinPlugin)
        +with(module.configurator) { createBuildFileIRs(this@createBuildFileIRs, data, module) }
    }

    private fun SourcesetDependency.toIR(type: DependencyType): DependencyIR = with(data) {
        val path = when (this@toIR) {
            is ModuleBasedSourcesetDependency -> module.path
            is PathBasedSourcesetDependency -> path
        }
        val modulePomIr = when (this@toIR) {
            is ModuleBasedSourcesetDependency -> pomIr.copy(artifactId = module.name)
            is PathBasedSourcesetDependency -> pomIr.copy(artifactId = path.parts.last())
        }
        return when {
            isSingleRootModuleMode
                    && path.parts.singleOrNull() == rootModules.single().name
                    && buildSystemType.isGradle -> GradleRootProjectDependencyIR(type)
            else -> ModuleDependencyIR(path, modulePomIr, type)
        }
    }
}