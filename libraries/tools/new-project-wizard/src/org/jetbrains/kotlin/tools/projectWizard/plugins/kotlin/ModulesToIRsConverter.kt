package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.TargetConfigurator
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
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
    val taskRunningContext: TaskRunningContext
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
            if ( // We want to have root build file for android projects
                data.allModules.any { it.configurator == AndroidSinglePlatformModuleConfigurator }
            ) return false
            return isSingleRootModuleMode
        }

    private fun calculatePathForModule(module: Module, rootPath: Path) = when {
        needFlattening && module.isRootModule -> data.projectPath
        else -> rootPath / module.name
    }

    fun createBuildFiles(): TaskResult<List<BuildFileIR>> = with(data) {
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
            RootFileModuleStructureIR(emptyList()),
            pomIr,
            rootBuildFileIrs
        )
    }


    private fun createBuildFileForModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = when (module.kind) {
        ModuleKind.multiplatform -> createMultiplatformModule(module, state)
        ModuleKind.singleplatform -> createSinglePlatformModule(module, state)
        else -> Success(emptyList())
    }

    private fun createSinglePlatformModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        taskRunningContext.mutateProjectStructureByModuleConfigurator(module, modulePath)
        val configurator = module.configurator
        val dependenciesIRs = module.sourcesets.flatMap { sourceset ->
            sourceset.dependencies.map { it.toIR(sourceset.sourcesetType.toDependencyType()) }
        } + configurator.createModuleIRs(data, module)

        val moduleIr = SingleplatformModuleIR(
            module.name,
            modulePath,
            dependenciesIRs,
            module.configurator.moduleType,
            module.sourcesets.map { sourceset ->
                SingleplatformSourcesetIR(
                    sourceset.sourcesetType,
                    modulePath / Defaults.SRC_DIR / sourceset.sourcesetType.name,
                    sourceset.dependencies.map { it.toIR(sourceset.sourcesetType.toDependencyType()) },
                    sourceset.template,
                    sourceset
                )
            }
        )
        val buildFileIr = BuildFileIR(
            module.name,
            modulePath,
            SingleplatformModulesStructureWithSingleModuleIR(
                moduleIr,
                emptyList()
            ),
            pomIr.copy(artifactId = module.name),
            createBuildFileIRs(module, state)
        )

        return module.subModules.mapSequence { subModule ->
            createBuildFileForModule(
                subModule,
                state.stateForSubModule(modulePath)
            )
        }.map { it.flatten() + buildFileIr }
    }

    private fun createMultiplatformModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        taskRunningContext.mutateProjectStructureByModuleConfigurator(module, modulePath)
        val targetIrs = module.subModules.flatMap { subModule ->
            (subModule.configurator as TargetConfigurator).createTargetIrs(subModule)
        }

        val sourcesetIrs = module.subModules.flatMap { target ->
            target.sourcesets.map { sourceset ->
                val sourcesetName = target.name + sourceset.sourcesetType.name.capitalize()
                SourcesetModuleIR(
                    sourcesetName,
                    modulePath / Defaults.SRC_DIR / sourcesetName,
                    sourceset.dependencies.map { it.toIR(DependencyType.MAIN) },
                    sourceset.containingModuleType,
                    sourceset.sourcesetType,
                    sourceset.template,
                    sourceset
                )
            }
        }

        return BuildFileIR(
            projectName,
            modulePath,
            MultiplatformModulesStructureIR(
                targetIrs,
                sourcesetIrs,
                emptyList()
            ),
            pomIr,
            createBuildFileIRs(module, state)
        ).asSingletonList().asSuccess()
    }

    private fun TaskRunningContext.mutateProjectStructureByModuleConfigurator(
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = with(module.configurator) {
        rootBuildFileIrs += createRootBuildFileIrs(data)
        runArbitraryTask(data, module, modulePath)
    }

    private fun createBuildFileIRs(
        module: Module,
        state: ModulesToIrsState
    ) = buildList<BuildSystemIR> {
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
        +module.configurator.createBuildFileIRs(data, module)
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