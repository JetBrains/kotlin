package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.GeneratedIdentificator
import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.IdentificatorOwner
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildFileIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.ModuleIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import org.jetbrains.kotlin.tools.projectWizard.templates.withSettingsOf

inline class ModulePath(val parts: List<String>) {
    fun asString(separator: String = ".") = parts.joinToString(separator)
    override fun toString(): String = asString()
}


sealed class SourcesetDependency
data class ModuleBasedSourcesetDependency(val module: Module) : SourcesetDependency()
data class PathBasedSourcesetDependency(val path: ModulePath) : SourcesetDependency() {
    companion object {
        val parser = valueParser { value, path ->
            val (stringPath) = value.parseAs<String>(path)
            PathBasedSourcesetDependency(ModulePath(stringPath.split('.')))
        }
    }
}


// A `main` or `test` sourceset for single or multiplatform projects
class Sourceset(
    val sourcesetType: SourcesetType,
    val containingModuleType: ModuleType,
    var dependencies: List<SourcesetDependency>,
    var parent: Module? = null,
    override val identificator: Identificator = GeneratedIdentificator(sourcesetType.name)
) : DisplayableSettingItem, IdentificatorOwner {
    override val text: String get() = sourcesetType.name
    override val greyText: String? get() = null

    companion object {
        fun parser(moduleType: ModuleType) = mapParser { map, path ->
            val (sourcesetType) = map.parseValue<SourcesetType>(this, path, "type", enumParser())
            val identificator = GeneratedIdentificator(sourcesetType.name)
            val (dependencies) = map.parseValue(
                this,
                path,
                "dependencies",
                listParser(PathBasedSourcesetDependency.parser)
            ) { emptyList() }

            Sourceset(sourcesetType, moduleType, dependencies, identificator = identificator)
        }
    }
}

@Suppress("EnumEntryName")
enum class SourcesetType {
    main, test;

    companion object {
        val ALL = values().toSet()
    }
}


fun TaskRunningContext.updateBuildFiles(action: (BuildFileIR) -> TaskResult<BuildFileIR>): TaskResult<Unit> =
    BuildSystemPlugin::buildFiles.update { buildFiles ->
        buildFiles.mapSequence(action)
    }

fun TaskRunningContext.updateModules(action: (ModuleIR) -> TaskResult<ModuleIR>): TaskResult<Unit> =
    updateBuildFiles { buildFile ->
        buildFile.withModulesUpdated { action(it) }
    }

fun TaskRunningContext.forEachModule(action: (ModuleIR) -> TaskResult<Unit>): TaskResult<Unit> =
    updateModules { moduleIR -> action(moduleIR).map { moduleIR } }