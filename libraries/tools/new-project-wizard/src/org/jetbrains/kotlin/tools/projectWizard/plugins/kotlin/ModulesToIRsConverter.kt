package org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList


import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.service.WizardKotlinVersion
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle.GradlePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.isGradle
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import java.nio.file.Path

data class ModulesToIrConversionData(
    val rootModules: List<Module>,
    val projectPath: Path,
    val projectName: String,
    val kotlinVersion: WizardKotlinVersion,
    val buildSystemType: BuildSystemType,
    val pomIr: PomIR
) {
    val allModules = rootModules.withAllSubModules()
    val isSingleRootModuleMode = rootModules.size == 1

    val moduleByPath = rootModules.withAllSubModules(includeSourcesets = true).associateBy(Module::path)

    fun getDependentModules(from: Module): TaskResult<List<Module>> =
        from.dependencies.mapSequence { to ->
            moduleByPath[to.path].toResult { InvalidModuleDependencyError(from.name, to.path.toString()) }
        }
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
    val data: ModulesToIrConversionData
) {

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
            return data.isSingleRootModuleMode
        }

    private val irsToAddToModules = hashMapOf<Module, MutableList<BuildSystemIR>>()
    private val moduleToBuildFile = hashMapOf<Module, BuildFileIR>()

    private fun calculatePathForModule(module: Module, rootPath: Path) = when {
        needFlattening && module.isRootModule -> data.projectPath
        else -> rootPath / module.name
    }

    fun Writer.createBuildFiles(): TaskResult<List<BuildFileIR>> = with(data) {
        val needExplicitRootBuildFile = !needFlattening
        val initialState = ModulesToIrsState(projectPath, parentModuleHasTransitivelySpecifiedKotlinVersion = false)

        val parentModuleHasKotlinVersion = allModules.any { module ->
            module.configurator == AndroidSinglePlatformModuleConfigurator
        }

        allModules.mapSequenceIgnore { module ->
            forModuleEachDependency(module) { from, to, dependencyType ->
                with(dependencyType) {
                    runArbitraryTaskBeforeIRsCreated(from, to)
                }
            }
        } andThen rootModules.mapSequence { module ->
            createBuildFileForModule(
                module,
                initialState.copy(parentModuleHasTransitivelySpecifiedKotlinVersion = parentModuleHasKotlinVersion)
            )
        }.map { it.flatten() }.map { buildFiles ->
            if (needExplicitRootBuildFile) buildFiles + createRootBuildFile()
            else buildFiles
        }.map { buildFiles ->
            buildFiles.map { buildFile ->
                val irs = buildFile.fromModules.flatMap { irsToAddToModules[it]?.toList() ?: emptyList() }
                buildFile.withIrs(irs)
            }
        }
    }

    private fun createRootBuildFile(): BuildFileIR = with(data) {
        BuildFileIR(
            projectName,
            projectPath,
            RootFileModuleStructureIR(persistentListOf()),
            emptyList(),
            pomIr,
            isRoot = true,
            rootBuildFileIrs.toPersistentList()
        )
    }


    private fun Writer.createBuildFileForModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = when (val configurator = module.configurator) {
        is MppModuleConfigurator -> createMultiplatformModule(module, state)
        is SinglePlatformModuleConfigurator -> createSinglePlatformModule(module, configurator, state)
        else -> Success(emptyList())
    }

    private fun <T : Any> forModuleEachDependency(
        from: Module,
        action: suspend ComputeContext<NoState>.(from: Module, to: Module, dependencyType: ModuleDependencyType) -> TaskResult<T>
    ): TaskResult<List<T>> {
        return from.dependencies.mapComputeM { dependency ->
            val to = data.moduleByPath.getValue(dependency.path)
            val (dependencyType) = ModuleDependencyType.getPossibleDependencyType(from, to)
                .toResult { InvalidModuleDependencyError(from, to) }
            action(from, to, dependencyType)
        }.sequence()
    }

    private fun Writer.createSinglePlatformModule(
        module: Module,
        configurator: SinglePlatformModuleConfigurator,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = computeM {
        val modulePath = calculatePathForModule(module, state.parentPath)
        val (moduleDependencies) = forModuleEachDependency(module) { from, to, dependencyType ->
            with(dependencyType) {
                @Suppress("DEPRECATION")
                with(unsafeSettingWriter) {
                    runArbitraryTask(
                        module,
                        to,
                        to.path.considerSingleRootModuleMode(data.isSingleRootModuleMode).asPath(),
                        data
                    ).ensure()
                }
                irsToAddToModules.getOrPut(to) { mutableListOf() } += createToIRs(module, to, data).get()
                createDependencyIrs(module, to, data).asSuccess()
            }
        }.map { it.flatten() }
        mutateProjectStructureByModuleConfigurator(module, modulePath)
        val buildFileIR = run {
            if (!configurator.needCreateBuildFile) return@run null
            val dependenciesIRs = buildPersistenceList<BuildSystemIR> {
                +moduleDependencies
                with(configurator) { +createModuleIRs(this@createSinglePlatformModule, data, module) }
                addIfNotNull(addSdtLibForNonGradleSignleplatformModule(module))
            }

            val moduleIr = SingleplatformModuleIR(
                if (modulePath == data.projectPath) data.projectName else module.name,
                modulePath,
                dependenciesIRs,
                module.template,
                module,
                module.sourcesets.map { sourceset ->
                    SingleplatformSourcesetIR(
                        sourceset.sourcesetType,
                        modulePath / Defaults.SRC_DIR / sourceset.sourcesetType.name,
                        persistentListOf(),
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
                listOf(module),
                data.pomIr.copy(artifactId = module.name),
                isRoot = false, /* TODO */
                createBuildFileIRs(module, state)
            ).also {
                moduleToBuildFile[module] = it
            }
        }

        module.subModules.mapSequence { subModule ->
            createBuildFileForModule(
                subModule,
                state.stateForSubModule(modulePath)
            )
        }.map { it.flatten() }
            .map { children ->
                buildFileIR?.let { children + it } ?: children
            }
    }

    private fun Writer.createMultiplatformModule(
        module: Module,
        state: ModulesToIrsState
    ): TaskResult<List<BuildFileIR>> = with(data) {
        val modulePath = calculatePathForModule(module, state.parentPath)
        mutateProjectStructureByModuleConfigurator(module, modulePath)
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
            module.subModules + module,
            pomIr,
            isRoot = false,
            buildPersistenceList {
                +createBuildFileIRs(module, state)
                module.subModules.forEach { +createBuildFileIRs(it, state) }
            }
        ).also { buildFile ->
            moduleToBuildFile[module] = buildFile
            module.subModules.forEach { subModule ->
                moduleToBuildFile[subModule] = buildFile
            }
        }.asSingletonList().asSuccess()

    }

    private fun Writer.createTargetModule(target: Module, modulePath: Path): MultiplatformModuleIR {
        mutateProjectStructureByModuleConfigurator(target, modulePath)
        val sourcesetss = target.sourcesets.map { sourceset ->
            val sourcesetName = target.name + sourceset.sourcesetType.name.capitalize()
            MultiplatformSourcesetIR(
                sourceset.sourcesetType,
                modulePath / Defaults.SRC_DIR / sourcesetName,
                target.name,
                persistentListOf(),
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
            TemplatesPlugin.addFileTemplates.execute(createTemplates(data, module, modulePath)).ensure()
            if (this@with is GradleModuleConfigurator) {
                GradlePlugin.settingsGradleFileIRs.addValues(createSettingsGradleIRs(module)).ensure()
            }
        }
    }

    private fun Reader.addSdtLibForNonGradleSignleplatformModule(module: Module): KotlinStdlibDependencyIR? {
        // for gradle stdlib is added by default KT-38221
        if (buildSystemType.isGradle) return null
        val stdlibType = module.configurator.createStdlibType(data, module) ?: return null
        return KotlinStdlibDependencyIR(
            type = stdlibType,
            isInMppModule = false,
            kotlinVersion = data.kotlinVersion,
            dependencyType = DependencyType.MAIN
        )
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
                when {
                    needRemoveVersion -> plugin.copy(version = null)
                    else -> plugin
                }
            }
        addIfNotNull(kotlinPlugin)
        +with(module.configurator) { createBuildFileIRs(this@createBuildFileIRs, data, module) }
    }
}