package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.GeneratedIdentificator
import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.IdentificatorOwner
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.context.SettingsWritingContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.ModuleConfiguratorSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.PluginSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.templates.Template

@Suppress("EnumEntryName")
enum class ModuleKind : DisplayableSettingItem {
    multiplatform,
    target,
    singleplatformJvm,
    singleplatformAndroid,
    singleplatformJs, ;

    override val text: String
        get() = name
}

// TODO separate to classes
class Module(
    var name: String,
    val configurator: ModuleConfigurator,
    var template: Template?,
    val sourcesets: List<Sourceset>,
    subModules: List<Module>,
    var parent: Module? = null,
    override val identificator: Identificator = GeneratedIdentificator(name)
) : DisplayableSettingItem, Validatable<Module>, IdentificatorOwner {

    val kind: ModuleKind
        get() = configurator.moduleKind

    override val validator = moduleNameValidator and
            moduleConfiguratorValidator and
            moduleTemplateValidator and
            subModulesValidator

    var subModules = subModules
        set(value) {
            field = value
            value.forEach { it.parent = this }
        }

    init {
        subModules.forEach { it.parent = this }
        sourcesets.forEach { it.parent = this }
    }


    override val text: String get() = name
    override val greyText: String?
        get() = when {
            kind == ModuleKind.target -> "${configurator.text} Target"
            configurator == MppModuleConfigurator -> "MPP Module"
            configurator == AndroidSinglePlatformModuleConfigurator -> "Android Module"
            configurator == IOSSinglePlatformModuleConfigurator -> "IOS Module"
            configurator == JsSingleplatformModuleConfigurator -> "JS Module"
            else -> "Module"
        }

    fun SettingsWritingContext.initDefaultValuesForSettings() {
        configurator.safeAs<ModuleConfiguratorWithSettings>()?.apply { initDefaultValuesFor(this@Module) }
        template?.apply { initDefaultValuesFor(this@Module) }
    }

    companion object {
        val ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES = setOf('-', '_')

        val parser: Parser<Module> = mapParser { map, path ->
            val (name) = map.parseValue<String>(path, "name")
            val identificator = GeneratedIdentificator(name)
            val (configurator) = map.parseValue(this, path, "type", ModuleConfigurator.getParser(identificator))

            val template = map["template"]?.let {
                Template.parser(identificator).parse(this, it, "$path.template")
            }.nullableValue()
            val sourcesets = listOf(Sourceset(SourcesetType.main), Sourceset(SourcesetType.test))
            val (submodules) = map.parseValue(this, path, "subModules", listParser(Module.parser)) { emptyList() }
            Module(name, configurator, template, sourcesets, submodules, identificator = identificator)
        }

        private val moduleNameValidator = settingValidator<Module> { module ->
            StringValidators.shouldNotBeBlank("Module name").validate(this, module.name)
        } and settingValidator { module ->
            StringValidators.shouldBeValidIdentifier("Module name `${module.name}`", ALLOWED_SPECIAL_CHARS_IN_MODULE_NAMES)
                .validate(this, module.name)
        }

        val moduleConfiguratorValidator = settingValidator<Module> { module ->
            withSettingsOf(module) {
                allSettingsOfModuleConfigurator(module.configurator).map { setting ->
                    val reference = when (setting) {
                        is PluginSetting<Any, SettingType<Any>> -> setting.reference
                        is ModuleConfiguratorSetting<Any, SettingType<Any>> -> setting.reference
                        else -> null
                    }
                    val value = reference?.notRequiredSettingValue
                        ?: reference?.savedOrDefaultValue
                        ?: return@map ValidationResult.ValidationError("${setting.title.capitalize()} should not be blank")
                    (setting.validator as SettingValidator<Any>).validate(this@settingValidator, value)
                }.fold()
            }
        }

        val moduleTemplateValidator = settingValidator<Module> { module ->
            val template = module.template ?: return@settingValidator ValidationResult.OK
            org.jetbrains.kotlin.tools.projectWizard.templates.withSettingsOf(module) {
                template.settings.map { setting ->
                    val value = setting.reference.notRequiredSettingValue
                        ?: setting.reference.savedOrDefaultValue
                        ?: return@map ValidationResult.ValidationError("${setting.title.capitalize()} should not be blank")
                    (setting.validator as SettingValidator<Any>).validate(this@settingValidator, value)
                }.fold()
            }
        }

        val subModulesValidator = inValidatorContext<Module> { module ->
            validateList(module.subModules)
        }
    }

}

val Module.path
    get() = generateSequence(this, Module::parent)
        .map { it.name }
        .toList()
        .asReversed()
        .let(::ModulePath)

val Sourceset.path
    get() = ModulePath(parent?.path?.parts.orEmpty() + sourcesetType.name)

val Module.isRootModule
    get() = parent == null

@Suppress("FunctionName")
fun MultiplatformTargetModule(name: String, configurator: ModuleConfigurator, sourcesets: List<Sourceset>) =
    Module(
        name,
        configurator,
        null,
        sourcesets,
        emptyList()
    )

@Suppress("FunctionName")
fun MultiplatformModule(name: String, targets: List<Module> = emptyList()) =
    Module(
        name,
        MppModuleConfigurator,
        null,
        emptyList(),
        targets
    )

@Suppress("FunctionName")
fun SingleplatformModule(name: String, sourcesets: List<Sourceset>) =
    Module(
        name,
        JvmSinglePlatformModuleConfigurator,
        null,
        sourcesets,
        emptyList()
    )