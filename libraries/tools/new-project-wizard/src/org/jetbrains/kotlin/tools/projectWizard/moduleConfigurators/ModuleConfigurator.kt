package org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators


import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.enumSettingImpl
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinBuildSystemPluginIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.StdlibType
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModulesToIrConversionData
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.correspondingStdlib
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import java.nio.file.Path
import kotlin.properties.ReadOnlyProperty


interface ModuleConfiguratorSettingsEnvironment {
    val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
}

class ModuleBasedConfiguratorSettingsEnvironment(
    private val configurator: ModuleConfigurator,
    private val module: Module
) : ModuleConfiguratorSettingsEnvironment {
    override val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
        get() = ModuleBasedConfiguratorSettingReference(configurator, module, this)
}

class IdBasedConfiguratorSettingsEnvironment(
    private val configurator: ModuleConfigurator,
    private val moduleId: Identificator
) : ModuleConfiguratorSettingsEnvironment {
    override val <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.reference: ModuleConfiguratorSettingReference<V, T>
        get() = IdBasedConfiguratorSettingReference(configurator, moduleId, this)
}

fun <T> withSettingsOf(
    moduleId: Identificator,
    configurator: ModuleConfigurator,
    function: ModuleConfiguratorSettingsEnvironment.() -> T
): T = function(IdBasedConfiguratorSettingsEnvironment(configurator, moduleId))

fun <T> withSettingsOf(
    module: Module,
    configurator: ModuleConfigurator = module.configurator,
    function: ModuleConfiguratorSettingsEnvironment.() -> T
): T = function(ModuleBasedConfiguratorSettingsEnvironment(configurator, module))


fun <V : Any, T : SettingType<V>> Reader.settingValue(module: Module, setting: ModuleConfiguratorSetting<V, T>): V? =
    withSettingsOf(module) {
        setting.reference.notRequiredSettingValue
    }


abstract class ModuleConfiguratorSettings : SettingsOwner {
    final override fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any?, ModuleConfiguratorSetting<V, T>> = cached { name ->
        ModuleConfiguratorSetting(create(name).buildInternal())
    }

    @Suppress("UNCHECKED_CAST")
    final override fun <V : DisplayableSettingItem> dropDownSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: DropDownSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, DropDownSettingType<V>>> =
        super.dropDownSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, DropDownSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun stringSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: StringSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<String, StringSettingType>> =
        super.stringSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<String, StringSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun booleanSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: BooleanSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Boolean, BooleanSettingType>> =
        super.booleanSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Boolean, BooleanSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> valueSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ValueSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, ValueSettingType<V>>> =
        super.valueSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<V, ValueSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun versionSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: VersionSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Version, VersionSettingType>> =
        super.versionSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Version, VersionSettingType>>

    @Suppress("UNCHECKED_CAST")
    final override fun <V : Any> listSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        parser: Parser<V>,
        init: ListSettingType.Builder<V>.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<List<V>, ListSettingType<V>>> =
        super.listSetting(
            title,
            neededAtPhase,
            parser,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<List<V>, ListSettingType<V>>>

    @Suppress("UNCHECKED_CAST")
    final override fun pathSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        init: PathSettingType.Builder.() -> Unit
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<Path, PathSettingType>> =
        super.pathSetting(
            title,
            neededAtPhase,
            init
        ) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<Path, PathSettingType>>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified E> enumSetting(
        title: String,
        neededAtPhase: GenerationPhase,
        crossinline init: DropDownSettingType.Builder<E>.() -> Unit = {}
    ): ReadOnlyProperty<Any, ModuleConfiguratorSetting<E, DropDownSettingType<E>>> where E : Enum<E>, E : DisplayableSettingItem =
        enumSettingImpl(title, neededAtPhase, init) as ReadOnlyProperty<Any, ModuleConfiguratorSetting<E, DropDownSettingType<E>>>
}

interface ModuleConfiguratorWithSettings : ModuleConfigurator {
    fun getConfiguratorSettings(): List<ModuleConfiguratorSetting<*, *>> = emptyList()
    fun getPluginSettings(): List<PluginSettingReference<Any, SettingType<Any>>> = emptyList()


    fun SettingsWriter.initDefaultValuesFor(module: Module) {
        withSettingsOf(module) {
            getConfiguratorSettings().forEach { setting ->
                setting.reference.setSettingValueToItsDefaultIfItIsNotSetValue()
            }
        }
    }

    fun <V : Any, T : SettingType<V>> Reader.settingsValue(module: Module, setting: ModuleConfiguratorSetting<V, T>): V =
        withSettingsOf(module) { setting.reference.settingValue }
}

val ModuleConfigurator.settings
    get() = when (this) {
        is ModuleConfiguratorWithSettings -> getConfiguratorSettings()
        else -> emptyList()
    }

fun Reader.allSettingsOfModuleConfigurator(moduleConfigurator: ModuleConfigurator) = when (moduleConfigurator) {
    is ModuleConfiguratorWithSettings -> buildList<Setting<Any, SettingType<Any>>> {
        +moduleConfigurator.getConfiguratorSettings()
        +moduleConfigurator.getPluginSettings().map { it.pluginSetting }
    }
    else -> emptyList()
}

fun Module.getConfiguratorSettings() = buildList<SettingReference<*, *>> {
    +configurator.settings.map { setting ->
        ModuleBasedConfiguratorSettingReference(configurator, this@getConfiguratorSettings, setting)
    }
    configurator.safeAs<ModuleConfiguratorWithSettings>()?.getPluginSettings()?.let { +it }
}


interface ModuleConfigurator : DisplayableSettingItem, EntitiesOwnerDescriptor {
    val moduleKind: ModuleKind

    val suggestedModuleName: String? get() = null
    val canContainSubModules: Boolean get() = false
    val requiresRootBuildFile: Boolean get() = false

    val kotlinDirectoryName: String get() = Defaults.KOTLIN_DIR.toString()
    val resourcesDirectoryName: String get() = Defaults.RESOURCES_DIR.toString()

    fun createBuildFileIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> =
        emptyList()

    fun createBuildFileIRsComparator(): Comparator<BuildSystemIR>? = null

    fun createModuleIRs(
        reader: Reader,
        configurationData: ModulesToIrConversionData,
        module: Module
    ): List<BuildSystemIR> =
        emptyList()

    fun createStdlibType(configurationData: ModulesToIrConversionData, module: Module): StdlibType? =
        safeAs<ModuleConfiguratorWithModuleType>()?.moduleType?.correspondingStdlib()

    fun createRootBuildFileIrs(configurationData: ModulesToIrConversionData): List<BuildSystemIR> = emptyList()
    fun createKotlinPluginIR(configurationData: ModulesToIrConversionData, module: Module): KotlinBuildSystemPluginIR? =
        null

    fun Writer.runArbitraryTask(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): TaskResult<Unit> = UNIT_SUCCESS

    fun Reader.createTemplates(
        configurationData: ModulesToIrConversionData,
        module: Module,
        modulePath: Path
    ): List<FileTemplate> = emptyList()

    companion object {
        val ALL = buildList<ModuleConfigurator> {
            +RealNativeTargetConfigurator.configurators
            +NativeForCurrentSystemTarget
            +JsBrowserTargetConfigurator
            +JsNodeTargetConfigurator
            +CommonTargetConfigurator
            +JvmTargetConfigurator
            +AndroidTargetConfigurator
            +MppModuleConfigurator
            +JvmSinglePlatformModuleConfigurator
            +AndroidSinglePlatformModuleConfigurator
            +IOSSinglePlatformModuleConfigurator
            +BrowserJsSinglePlatformModuleConfigurator
            +NodeJsSinglePlatformModuleConfigurator
        }

        init {
            ALL.groupBy(ModuleConfigurator::id)
                .forEach { (id, configurators) -> assert(configurators.size == 1) { id } }
        }

        private val BY_ID = ALL.associateBy(ModuleConfigurator::id)

        fun getParser(moduleIdentificator: Identificator): Parser<ModuleConfigurator> =
            valueParserM { value, path ->
                val (id) = value.parseAs<String>(path)
                BY_ID[id].toResult { ConfiguratorNotFoundError(id) }
            } or mapParser { map, path ->
                val (id) = map.parseValue<String>(path, "name")
                val (configurator) = BY_ID[id].toResult { ConfiguratorNotFoundError(id) }
                val (settingsWithValues) = parseSettingsMap(
                    path,
                    map,
                    configurator.settings.map { setting ->
                        val reference = withSettingsOf(moduleIdentificator, configurator) { setting.reference }
                        reference to setting
                    }
                )
                updateState { it.withSettings(settingsWithValues) }
                configurator
            }
    }
}

interface GradleModuleConfigurator : ModuleConfigurator {
    fun Reader.createSettingsGradleIRs(module: Module): List<BuildSystemIR> = emptyList()
}